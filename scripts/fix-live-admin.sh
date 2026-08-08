#!/usr/bin/env bash
# Fix https://flintnthread.online/api/admin/ (403 / 502) on the Ubuntu VPS.
# Run ON THE SERVER via SSH:
#   cd /opt/flintnthread && bash scripts/fix-live-admin.sh
set -euo pipefail

DOMAIN="${FLINT_DOMAIN:-flintnthread.online}"
NGINX_SITE="${NGINX_SITE:-/etc/nginx/sites-available/flintnthread.online}"
APP_ROOT="${APP_ROOT:-/opt/flintnthread}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "==> Flint admin API fix for https://${DOMAIN}/api/admin/"
echo ""

admin_local_ok() {
  curl -sf "http://127.0.0.1:8082/api/admin/health" >/dev/null 2>&1
}

echo "==> 1) Backend health (localhost)"
for port in 8080 8082 8083; do
  case "$port" in
    8080) path="/api/categories/main" ;;
    8082) path="/api/admin/health" ;;
    8083) path="/api/public/marketplace-stats" ;;
  esac
  if curl -sf "http://127.0.0.1:${port}${path}" -o /dev/null 2>/dev/null; then
    echo "  port ${port}: OK"
  else
    echo "  port ${port}: FAIL"
  fi
done

if ! admin_local_ok; then
  echo ""
  echo "==> 2) Admin service not running on :8082 — restarting..."
  if systemctl list-unit-files flint-admin.service >/dev/null 2>&1; then
    sudo systemctl restart flint-admin || true
    sleep 8
  fi
  if ! admin_local_ok; then
    echo "ERROR: admin-service still down on 127.0.0.1:8082"
    echo "  sudo systemctl status flint-admin --no-pager"
    echo "  sudo journalctl -u flint-admin -n 50 --no-pager"
    echo ""
    echo "If never deployed, from repo root on the server run:"
    echo "  export FLINT_CONFIG_DIR=/etc/flintnthread"
    echo "  export SPRING_PROFILES_ACTIVE=prod"
    echo "  bash scripts/deploy-vps.sh"
    exit 1
  fi
  echo "  admin-service is up."
else
  echo ""
  echo "==> 2) Admin service OK on :8082"
fi

echo ""
echo "==> 3) Nginx — ensure /api/admin/ routes to port 8082 (before /api/)"
if [[ ! -f "$NGINX_SITE" ]]; then
  echo "WARN: $NGINX_SITE not found."
  echo "Install full site config:"
  echo "  sudo cp $REPO_ROOT/scripts/nginx-flintnthread.online.conf $NGINX_SITE"
  echo "  sudo ln -sf $NGINX_SITE /etc/nginx/sites-enabled/"
else
  if grep -q 'location \^~ /api/admin/' "$NGINX_SITE"; then
    echo "  /api/admin/ block already present."
  else
    echo "  MISSING /api/admin/ block — inserting before general /api/ ..."
    sudo cp "$NGINX_SITE" "${NGINX_SITE}.bak.$(date +%Y%m%d%H%M%S)"
    sudo tee /tmp/flint-admin-nginx-snippet.conf >/dev/null <<'SNIP'
    # Admin app API (MUST be before /api/)
    location ^~ /api/admin/ {
        proxy_pass http://127.0.0.1:8082;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }

SNIP
  if grep -q 'location \^~ /api/' "$NGINX_SITE"; then
      sudo awk '
        /location \^~ \/api\// && !done {
          while ((getline line < "/tmp/flint-admin-nginx-snippet.conf") > 0) print line
          close("/tmp/flint-admin-nginx-snippet.conf")
          done=1
        }
        { print }
      ' "$NGINX_SITE" | sudo tee "${NGINX_SITE}.new" >/dev/null
      sudo mv "${NGINX_SITE}.new" "$NGINX_SITE"
    else
      sudo sed -i '/server_name.*flintnthread/r /tmp/flint-admin-nginx-snippet.conf' "$NGINX_SITE"
    fi
    rm -f /tmp/flint-admin-nginx-snippet.conf
    echo "  Inserted /api/admin/ proxy block."
  fi
  sudo nginx -t
  sudo systemctl reload nginx
  echo "  Nginx reloaded."
fi

echo ""
echo "==> 4) Public HTTPS check"
PUBLIC_CODE=$(curl -s -o /tmp/flint-admin-health.json -w "%{http_code}" "https://${DOMAIN}/api/admin/health" || echo "000")
echo "  https://${DOMAIN}/api/admin/health → HTTP ${PUBLIC_CODE}"
if [[ "$PUBLIC_CODE" == "200" ]]; then
  cat /tmp/flint-admin-health.json
  echo ""
  echo ""
  echo "SUCCESS — Admin API is live. You can log in from the Admin app."
else
  echo ""
  echo "Still failing. Manual steps:"
  echo "  sudo cp $REPO_ROOT/scripts/nginx-flintnthread.online.conf $NGINX_SITE"
  echo "  sudo nginx -t && sudo systemctl reload nginx"
  echo "  bash $REPO_ROOT/scripts/verify-admin-production.sh"
  exit 1
fi
