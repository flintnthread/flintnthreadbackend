# Upload admin API fix to live VPS from Windows (requires OpenSSH client).
# Usage:
#   .\scripts\deploy-admin-fix-windows.ps1 -SshUser ubuntu -SshHost flintnthread.online
#
# Or set env: $env:FLINT_SSH_USER="ubuntu"; $env:FLINT_SSH_HOST="187.127.155.2"

param(
    [string]$SshUser = $env:FLINT_SSH_USER,
    [string]$SshHost = $env:FLINT_SSH_HOST,
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

if (-not $SshUser -or -not $SshHost) {
    Write-Host @"

MISSING SSH details.

This script uploads the fixed user-service JAR to your live server so
https://flintnthread.online/api/admin/ works (login fix).

Run with your VPS SSH login:
  .\scripts\deploy-admin-fix-windows.ps1 -SshUser YOUR_USER -SshHost 187.127.155.2

Common Hostinger VPS user: root or ubuntu

"@ -ForegroundColor Yellow
    exit 1
}

$userJar = Join-Path $RepoRoot "user-service\target\user-service-0.0.1-SNAPSHOT.jar"
$adminJar = Join-Path $RepoRoot "admin-service\target\admin-service-0.0.1-SNAPSHOT.jar"

Write-Host "==> Building JARs..."
Push-Location $RepoRoot
& .\mvnw.cmd -pl user-service,admin-service -am package -DskipTests | Out-Null
Pop-Location

if (-not (Test-Path $userJar)) { throw "Missing $userJar" }
if (-not (Test-Path $adminJar)) { throw "Missing $adminJar" }

$remote = "${SshUser}@${SshHost}"
Write-Host "==> Uploading to $remote ..."

ssh $remote "sudo mkdir -p /opt/flintnthread/user /opt/flintnthread/admin"
scp $userJar "${remote}:/tmp/user-service.jar"
scp $adminJar "${remote}:/tmp/admin-service.jar"

Write-Host "==> Installing and restarting services..."
ssh $remote @'
set -e
sudo cp /tmp/user-service.jar /opt/flintnthread/user/app.jar
sudo cp /tmp/admin-service.jar /opt/flintnthread/admin/app.jar
if systemctl list-unit-files flint-user.service >/dev/null 2>&1; then
  sudo systemctl restart flint-user flint-admin
elif systemctl list-unit-files flintnthread.service >/dev/null 2>&1; then
  sudo systemctl restart flintnthread
  sudo systemctl restart flint-admin 2>/dev/null || true
else
  echo "WARN: No flint-user/flintnthread systemd unit found."
  echo "Restart Java manually on the server."
fi
sleep 8
curl -sf http://127.0.0.1:8082/api/admin/health && echo ""
curl -sI https://flintnthread.online/api/admin/health | head -3
'@

Write-Host ""
Write-Host "Done. Try Admin login again." -ForegroundColor Green
