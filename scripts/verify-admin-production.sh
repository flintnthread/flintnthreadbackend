#!/usr/bin/env bash
# Run ON the Ubuntu server (SSH) to diagnose admin API / nginx.
set -euo pipefail

echo "==> Local admin-service (port 8082)"
if curl -sf "http://127.0.0.1:8082/api/admin/health"; then
  echo ""
  echo "OK: admin-service responds on 127.0.0.1:8082"
else
  echo "FAIL: admin-service not reachable on 127.0.0.1:8082"
  echo "  sudo systemctl status flint-admin --no-pager || true"
  echo "  sudo journalctl -u flint-admin -n 40 --no-pager || true"
fi

echo ""
echo "==> Local user-service (port 8080)"
curl -sf "http://127.0.0.1:8080/api/categories/main" | head -c 120 || echo "FAIL: user-service on 8080"

echo ""
echo ""
echo "==> Public HTTPS (through nginx)"
curl -sI "https://flintnthread.online/api/admin/health" | head -5 || true

echo ""
echo "If local :8082 works but HTTPS returns 502, add scripts/nginx-admin-api.conf to nginx:"
echo "  sudo nano /etc/nginx/sites-available/flintnthread.online"
echo "  sudo nginx -t && sudo systemctl reload nginx"
