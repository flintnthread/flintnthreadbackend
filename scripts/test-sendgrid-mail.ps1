# Test SendGrid mail for user + seller backends (SMTP + optional live API).
param(
    [string]$To = "support@flintnthread.in",
    [string]$ApiKey = "",
    [string]$BaseUrl = "https://flintnthread.online",
    [string]$SellerEmail = ""
)

if (-not $ApiKey) {
    $ApiKey = [Environment]::GetEnvironmentVariable("SENDGRID_API_KEY")
}
if (-not $ApiKey) {
    $localProps = Join-Path $PSScriptRoot "..\config\application-local.properties"
    if (Test-Path $localProps) {
        $line = Select-String -Path $localProps -Pattern '^SENDGRID_API_KEY=(.+)$' | Select-Object -First 1
        if ($line -and $line.Matches[0].Groups[1].Value.Trim()) {
            $ApiKey = $line.Matches[0].Groups[1].Value.Trim()
        }
    }
}

if (-not $ApiKey) {
    Write-Host "ERROR: Set SENDGRID_API_KEY env var or config/application-local.properties"
    exit 1
}

Write-Host "SendGrid key check..."
try {
    $scopes = Invoke-RestMethod -Uri "https://api.sendgrid.com/v3/scopes" -Headers @{ Authorization = "Bearer $ApiKey" } -TimeoutSec 30
    Write-Host "[OK] API key valid ($($scopes.scopes.Count) scopes)"
} catch {
    Write-Host "[FAIL] API key invalid: $($_.Exception.Message)"
    exit 1
}

function Test-Smtp($label, $fromName) {
    $msg = New-Object System.Net.Mail.MailMessage
    $msg.From = New-Object System.Net.Mail.MailAddress("support@flintnthread.in", $fromName)
    $msg.To.Add($To)
    $msg.Subject = "[$label] SendGrid SMTP test"
    $msg.Body = "SMTP test for $label at $(Get-Date -Format o)"
    $smtp = New-Object System.Net.Mail.SmtpClient("smtp.sendgrid.net", 587)
    $smtp.EnableSsl = $true
    $smtp.Credentials = New-Object System.Net.NetworkCredential("apikey", $ApiKey)
    try {
        $smtp.Send($msg)
        Write-Host "[OK] $label SMTP sent to $To"
    } catch {
        Write-Host "[FAIL] $label SMTP: $($_.Exception.Message)"
    } finally {
        $msg.Dispose()
        $smtp.Dispose()
    }
}

Test-Smtp "USER-SERVICE" "Flint & Thread"
Test-Smtp "SELLER-SERVICE" "Flint & Thread Seller"

Write-Host ""
Write-Host "Live API tests on $BaseUrl ..."

try {
    $userUrl = "$BaseUrl/api/email/preview/send-otp-test?to=$([uri]::EscapeDataString($To))&otp=123456"
    $user = Invoke-RestMethod -Uri $userUrl -Method POST -TimeoutSec 45
    Write-Host "[OK] user-service API: $($user.message)"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Host "[FAIL] user-service API ($code). Restart user-service on VPS after updating SendGrid key."
    if ($_.ErrorDetails.Message) { Write-Host "       $($_.ErrorDetails.Message)" }
}

if ($SellerEmail) {
    $body = @{ email = $SellerEmail } | ConvertTo-Json
    try {
        $seller = Invoke-RestMethod -Uri "$BaseUrl/api/auth/forgot-password" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 45
        Write-Host "[OK] seller-service forgot-password: $($seller.message)"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Host "[FAIL] seller-service API ($code)"
        if ($_.ErrorDetails.Message) { Write-Host "       $($_.ErrorDetails.Message)" }
    }
} else {
    Write-Host "[SKIP] seller-service API - pass -SellerEmail with a registered seller account email"
}

Write-Host ""
Write-Host "Check inbox/spam at $To"
