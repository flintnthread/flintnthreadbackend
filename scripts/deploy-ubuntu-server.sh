#!/usr/bin/env bash
# Run ON the Ubuntu production server (SSH) to deploy HTML email update.
# Usage: bash deploy-ubuntu-server.sh

set -euo pipefail

APP_DIR="${APP_DIR:-/home/ubuntu/flintnthread-backend}"
SERVICE_NAME="${SERVICE_NAME:-flintnthread}"

echo "==> Pulling latest main from GitHub..."
cd "$APP_DIR"
git fetch origin main
git checkout main
git pull origin main

echo "==> Building JAR..."
mvn clean package -DskipTests

JAR="$APP_DIR/target/authdemo-0.0.1-SNAPSHOT.jar"
if [[ ! -f "$JAR" ]]; then
  echo "ERROR: JAR not found at $JAR"
  exit 1
fi

echo "==> Restarting backend service: $SERVICE_NAME"
if systemctl list-units --full -all | grep -q "$SERVICE_NAME.service"; then
  sudo systemctl restart "$SERVICE_NAME"
  sudo systemctl status "$SERVICE_NAME" --no-pager
else
  echo "WARN: systemd service '$SERVICE_NAME' not found."
  echo "Restart your Java process manually, e.g.:"
  echo "  pkill -f authdemo-0.0.1-SNAPSHOT.jar"
  echo "  nohup java -jar $JAR > app.log 2>&1 &"
fi

echo "==> Verify deploy (should return html-otp-v2):"
sleep 5
curl -s "https://flintnthread.online/api/email/preview/version" || true
echo ""
echo "==> SendGrid: ensure Azure App Service has SENDGRID_API_KEY in Configuration → Application settings"
echo "    and sender support@flintnthread.in is verified in SendGrid."
echo "==> Verify invoice QR HTML endpoint (expect HTTP 200 text/html):"
curl -sI "https://flintnthread.online/api/email/preview/invoice-html?order_id=341&seller_id=248" | head -5 || true
echo ""
echo "If /html-invoice still shows Unmatched Route, add scripts/nginx-invoice-qr.conf to nginx."
echo "Done."
