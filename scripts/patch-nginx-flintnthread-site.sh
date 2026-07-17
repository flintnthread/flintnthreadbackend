#!/usr/bin/env bash
# Ensure flintnthread.in / .online nginx server blocks include API + /uploads routing.
# Run on VPS from repo root: bash scripts/patch-nginx-flintnthread-site.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SNIP_MARKER="include snippets/flintnthread-api.conf;"
SNIP_FILE="/etc/nginx/snippets/flintnthread-api.conf"

if [[ ! -f "$SNIP_FILE" ]]; then
  echo "Missing $SNIP_FILE — run: bash scripts/apply-nginx-flintnthread-online.sh"
  exit 1
fi

mapfile -t SITE_FILES < <(grep -Rl "server_name.*flintnthread\.\(in\|online\)" /etc/nginx/sites-enabled /etc/nginx/conf.d 2>/dev/null | sort -u || true)

if [[ ${#SITE_FILES[@]} -eq 0 ]]; then
  echo "No nginx site files found for flintnthread.in / flintnthread.online under /etc/nginx"
  exit 1
fi

patch_file() {
  local file="$1"
  if grep -qF "$SNIP_MARKER" "$file"; then
    echo "OK  already patched: $file"
    return 0
  fi
  local backup="${file}.bak.$(date +%Y%m%d%H%M%S)"
  sudo cp "$file" "$backup"
  sudo awk -v marker="$SNIP_MARKER" '
    BEGIN { inserted = 0 }
    {
      if (!inserted && $0 ~ /^[[:space:]]*location[[:space:]]+\/[[:space:]]*\{/) {
        print "    " marker
        inserted = 1
      }
      print
    }
    END {
      if (!inserted) {
        print "    " marker
      }
    }
  ' "$backup" | sudo tee "$file" >/dev/null
  echo "PATCHED: $file (backup: $backup)"
}

echo "==> Patching nginx site files..."
for f in "${SITE_FILES[@]}"; do
  patch_file "$f"
done

echo ""
echo "==> Testing nginx..."
sudo nginx -t
sudo systemctl reload nginx

echo ""
echo "==> Quick checks (image must NOT be text/html):"
SAMPLE_IMG="$(curl -sf "https://flintnthread.in/api/products?page=0&size=1" | python3 -c "import json,sys; d=json.load(sys.stdin); print((d.get('content') or [{}])[0].get('imageUrl',''))" 2>/dev/null || true)"
if [[ -n "$SAMPLE_IMG" ]]; then
  echo "Image URL: $SAMPLE_IMG"
  curl -sI "$SAMPLE_IMG" | awk 'toupper($1) ~ /^HTTP/ || tolower($1)=="content-type" {print}'
else
  echo "WARN: could not read sample imageUrl from API"
fi

echo ""
echo "Shop seller store (expect 200):"
curl -s -o /dev/null -w "  /api/shop/sellers/218/store -> %{http_code}\n" "https://flintnthread.in/api/shop/sellers/218/store" || true
