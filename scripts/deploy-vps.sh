#!/usr/bin/env bash
# Deploy user, seller, admin on Ubuntu VPS
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/flintnthread}"
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
echo "==> Nginx (one domain for all 3 apps)"
echo "Run on VPS: bash scripts/apply-nginx-flintnthread-online.sh"
echo "Then add 'include snippets/flintnthread-api.conf;' in server { } before user catch-all."
