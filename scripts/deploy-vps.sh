#!/usr/bin/env bash
# Deploy user, seller, admin on Ubuntu VPS
set -euo pipefail

APP_ROOT="${APP_ROOT:-/var/www/flintnthread-backend}"
CONFIG_DIR="${CONFIG_DIR:-/etc/flintnthread}"
PROFILE="${SPRING_PROFILES_ACTIVE:-prod}"

echo "==> Building platform..."
cd "$(dirname "$0")/.."
./mvnw clean package -DskipTests

install_jar() {
  local module="$1"
  local name="$2"
  sudo mkdir -p "$APP_ROOT/$name"
  sudo cp "$module/target/"*.jar "$APP_ROOT/$name/app.jar"
}

install_jar user-service user
install_jar seller-service seller
install_jar admin-service admin

echo "==> Config: $CONFIG_DIR/application.properties"
sudo mkdir -p "$CONFIG_DIR"
if [[ ! -f "$CONFIG_DIR/application.properties" ]]; then
  sudo cp config/application.properties.example "$CONFIG_DIR/application.properties"
  echo "EDIT $CONFIG_DIR/application.properties with real secrets, then restart services."
fi

if grep -qE '^SENDGRID_API_KEY=(REPLACE_WITH_YOUR_SENDGRID_API_KEY)?$' "$CONFIG_DIR/application.properties" 2>/dev/null \
   || ! grep -q '^SENDGRID_API_KEY=.' "$CONFIG_DIR/application.properties" 2>/dev/null; then
  echo ""
  echo "WARNING: SENDGRID_API_KEY is missing in $CONFIG_DIR/application.properties"
  echo "         Mail (OTP, password reset, seller emails) will NOT work until you set it."
  echo "         Example: SENDGRID_API_KEY=SG.xxxxx"
  echo ""
fi

if grep -qE '^DB_PASSWORD=(YOUR_MYSQL_PASSWORD)?$' "$CONFIG_DIR/application.properties" 2>/dev/null \
   || ! grep -q '^DB_PASSWORD=.' "$CONFIG_DIR/application.properties" 2>/dev/null; then
  echo "WARNING: DB_PASSWORD is missing in $CONFIG_DIR/application.properties"
fi

write_unit() {
  local name="$1"
  sudo tee "/etc/systemd/system/flint-$name.service" > /dev/null <<EOF
[Unit]
Description=Flint $name
After=network.target mysql.service

[Service]
User=ubuntu
WorkingDirectory=$APP_ROOT/$name
Environment=SPRING_PROFILES_ACTIVE=$PROFILE
Environment=FLINT_CONFIG_DIR=$CONFIG_DIR
ExecStart=/usr/bin/java -jar $APP_ROOT/$name/app.jar
SuccessExitStatus=143
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
}

write_unit user
write_unit seller
write_unit admin

sudo systemctl daemon-reload
sudo systemctl enable flint-user flint-seller flint-admin
sudo systemctl restart flint-user flint-seller flint-admin

echo "==> Health checks"
sleep 8
curl -sf "http://127.0.0.1:8080/api/categories/main" | head -c 80 || echo "user: check logs"
curl -sf "http://127.0.0.1:8082/api/admin/health" || echo "admin: check logs"
curl -sf "http://127.0.0.1:8083/api/public/marketplace-stats" || echo "seller: check logs"

echo ""
echo "==> Mail check (user-service; expects 200 if SENDGRID_API_KEY is set)"
if curl -sf -X POST "http://127.0.0.1:8080/api/email/preview/send-otp-test?to=${MAIL_TEST_TO:-support@flintnthread.in}&otp=123456" >/dev/null; then
  echo "    Mail test: OK (check inbox at ${MAIL_TEST_TO:-support@flintnthread.in})"
else
  echo "    Mail test: FAILED — set SENDGRID_API_KEY in $CONFIG_DIR/application.properties and restart flint-user"
fi

echo ""
echo "==> Nginx (API + product image uploads)"
echo "Run on VPS from this repo:"
echo "  bash scripts/apply-nginx-flintnthread-online.sh"
echo "  bash scripts/patch-nginx-flintnthread-site.sh"
echo "Or manually add BEFORE location / in each flintnthread.in server { }:"
echo "  include snippets/flintnthread-api.conf;"
echo ""
echo "==> Verify product images after deploy"
echo "Run: bash scripts/verify-product-images.sh"
echo "     (API on flintnthread.in, images on flintnthread.com)"
