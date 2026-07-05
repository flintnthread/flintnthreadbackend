#!/usr/bin/env bash
# Apply user + seller + admin API routing on flintnthread.online (Ubuntu VPS).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOC_SRC="$SCRIPT_DIR/nginx-flintnthread-online.conf"
SNIP_DST="/etc/nginx/snippets/flintnthread-api.conf"
OLD_MAP="/etc/nginx/conf.d/flintnthread-http-map.conf"

echo "==> Installing API location snippets..."
sudo mkdir -p /etc/nginx/snippets
sudo cp "$LOC_SRC" "$SNIP_DST"

if [[ -f "$OLD_MAP" ]]; then
  echo "==> Removing deprecated X-Seller-Id map (no longer needed)..."
  sudo rm -f "$OLD_MAP"
fi

echo ""
echo "Add this line INSIDE server { } for flintnthread.online,"
echo "BEFORE your catch-all proxy_pass to :8080:"
echo ""
echo "    include snippets/flintnthread-api.conf;"
echo ""
echo "Ensure you also have (after the include, LAST among /api/ blocks):"
echo ""
echo "    location ^~ /api/ {"
echo "        proxy_pass http://127.0.0.1:8080;"
echo "        ..."
echo "    }"
echo ""
echo "Then run:"
echo "    sudo nginx -t && sudo systemctl reload nginx"
echo ""
echo "Verify services:"
echo "    curl -s http://127.0.0.1:8080/api/categories/main | head -c 60"
echo "    curl -s http://127.0.0.1:8082/api/admin/health"
echo "    curl -s http://127.0.0.1:8083/api/public/marketplace-stats"
echo "    curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8083/api/seller/dashboard"
echo "    curl -s https://flintnthread.online/api/public/marketplace-stats"
