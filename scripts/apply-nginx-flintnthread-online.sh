#!/usr/bin/env bash
# Apply user + seller + admin API routing on flintnthread.online and flintnthread.in (Ubuntu VPS).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOC_SRC="$SCRIPT_DIR/nginx-flintnthread-online.conf"
MAP_SRC="$SCRIPT_DIR/nginx-flintnthread-cors-map.conf"
SNIP_DST="/etc/nginx/snippets/flintnthread-api.conf"
MAP_DST="/etc/nginx/conf.d/flintnthread-cors-map.conf"
OLD_MAP="/etc/nginx/conf.d/flintnthread-http-map.conf"

echo "==> Installing CORS map (http context)..."
sudo cp "$MAP_SRC" "$MAP_DST"

echo "==> Installing API location snippets..."
sudo mkdir -p /etc/nginx/snippets
sudo cp "$LOC_SRC" "$SNIP_DST"

if [[ -f "$OLD_MAP" ]]; then
  echo "==> Removing deprecated X-Seller-Id map (no longer needed)..."
  sudo rm -f "$OLD_MAP"
fi

echo ""
echo "Add this line INSIDE server { } for BOTH:"
echo "  - flintnthread.online (and www / admin / seller subdomains)"
echo "  - flintnthread.in     (and www / admin / seller subdomains)"
echo "BEFORE your catch-all location / :"
echo ""
echo "    include snippets/flintnthread-api.conf;"
echo ""
echo "Then run:"
echo "    sudo nginx -t && sudo systemctl reload nginx"
echo ""
echo "Verify services (JSON, not HTML):"
echo "    curl -s https://flintnthread.online/api/public/marketplace-stats"
echo "    curl -s https://flintnthread.in/api/public/marketplace-stats"
echo "    curl -s https://flintnthread.online/api/admin/health"
echo "    curl -s https://flintnthread.in/api/admin/health"
echo "    curl -s -H 'Origin: https://flintnthread.in' -o /dev/null -w '%{http_code}\\n' https://flintnthread.in/api/categories/main"
echo "    # expect 200 (not 403 Invalid CORS request)"
echo ""
echo "Verify product images (must be image/jpeg or image/png, NOT text/html):"
echo "    bash scripts/verify-product-images.sh https://flintnthread.in"
echo "    curl -sI https://flintnthread.in/uploads/products/SOME_FILE.jpg | grep -i content-type"
