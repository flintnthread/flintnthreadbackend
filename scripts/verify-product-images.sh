#!/usr/bin/env bash
# Post-deploy check: API image URLs must return image/* (not SPA HTML).
set -euo pipefail

API_DOMAIN="${API_DOMAIN:-https://flintnthread.in}"
API_DOMAIN="${API_DOMAIN%/}"
MEDIA_CDN="${MEDIA_CDN:-https://flintnthread.com}"
MEDIA_CDN="${MEDIA_CDN%/}"

echo "==> Product image deploy check (API: $API_DOMAIN, CDN: $MEDIA_CDN)"

API_JSON="$(curl -sf "${API_DOMAIN}/api/products?page=0&size=3" || true)"
if [[ -z "$API_JSON" ]]; then
  echo "FAIL: Cannot reach ${API_DOMAIN}/api/products"
  exit 1
fi

IMAGE_URLS="$(python3 - <<'PY' "$API_JSON"
import json, sys
data = json.loads(sys.argv[1])
rows = data.get("content") or data if isinstance(data, list) else []
urls = []
for row in rows:
    if not isinstance(row, dict):
        continue
    if row.get("imageUrl"):
        urls.append(str(row["imageUrl"]).strip())
    for img in row.get("images") or []:
        if isinstance(img, dict) and img.get("imageUrl"):
            urls.append(str(img["imageUrl"]).strip())
seen = set()
for u in urls:
    if u and u not in seen:
        seen.add(u)
        print(u)
PY
)"

if [[ -z "$IMAGE_URLS" ]]; then
  echo "WARN: No imageUrl fields in API response — check database product_images."
  exit 0
fi

FAIL=0
while IFS= read -r URL; do
  [[ -z "$URL" ]] && continue
  HEADERS="$(curl -sI --max-time 20 "$URL" || true)"
  STATUS="$(echo "$HEADERS" | awk 'toupper($1) ~ /^HTTP/ {print $2; exit}')"
  CT="$(echo "$HEADERS" | awk -F': ' 'tolower($1)=="content-type" {print tolower($2); exit}')"
  CT="${CT//$'\r'/}"

  if [[ "$STATUS" != "200" ]]; then
    echo "FAIL [$STATUS] $URL"
    FAIL=1
    continue
  fi
  if [[ "$CT" == text/html* ]]; then
    echo "FAIL [HTML not image] $URL"
    echo "     nginx is serving the shop SPA instead of /uploads/products/."
    echo "     Fix: include snippets/flintnthread-api.conf BEFORE location / in nginx."
    FAIL=1
    continue
  fi
  if [[ "$CT" != image/* ]]; then
    echo "WARN [$CT] $URL"
    continue
  fi
  echo "OK  [$CT] $URL"
done <<< "$IMAGE_URLS"

if [[ "$FAIL" -ne 0 ]]; then
  echo ""
  echo "On VPS run:"
  echo "  bash scripts/apply-nginx-flintnthread-online.sh"
  echo "  # add inside server { } for flintnthread.com / flintnthread.in:"
  echo "  include snippets/flintnthread-api.conf;"
  echo "  sudo nginx -t && sudo systemctl reload nginx"
  echo ""
  echo "Ensure product files exist:"
  echo "  ls /var/flintnthread/uploads/products/ | head"
  echo "  curl -sI http://127.0.0.1:8083/uploads/products/<filename>"
  exit 1
fi

echo "==> All checked product image URLs look good."
