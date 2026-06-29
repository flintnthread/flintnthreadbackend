# Verify seller API paths route to seller-service (not user-service) on production.
param(
    [string]$BaseUrl = "https://flintnthread.online"
)

$tests = @(
    @{ Path = "/api/public/marketplace-stats"; Expect = "200" },
    @{ Path = "/api/seller/profile"; Expect = "401" },
    @{ Path = "/api/seller/dashboard"; Expect = "401" },
    @{ Path = "/api/seller/orders/stats"; Expect = "401" },
    @{ Path = "/api/seller/products"; Expect = "401" },
    @{ Path = "/api/seller/payout/summary"; Expect = "401" },
    @{ Path = "/api/seller/earnings"; Expect = "401" },
    @{ Path = "/api/seller/notifications"; Expect = "401" },
    @{ Path = "/api/seller/locations/countries"; Expect = "200" },
    @{ Path = "/api/products"; Expect = "200" },
    @{ Path = "/api/dashboard"; Expect = "404,403" }
)

Write-Host "Testing API routing at $BaseUrl`n"

foreach ($t in $tests) {
    $url = "$BaseUrl$($t.Path)"
    try {
        $resp = Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing -TimeoutSec 15 -ErrorAction Stop
        $code = [string]$resp.StatusCode
    } catch {
        $code = [string]$_.Exception.Response.StatusCode.value__
        if (-not $code) { $code = "ERR" }
    }

    $allowed = $t.Expect -split ","
    $ok = $allowed -contains $code
    $mark = if ($ok) { "OK" } else { "FAIL" }
    Write-Host "[$mark] $code $($t.Path) (expected $($t.Expect))"
}

Write-Host "`n401 on /api/seller/* = seller-service reached (auth required)."
Write-Host "403/empty on /api/seller/* = nginx not updated yet."
Write-Host "200 + paginated content on /api/seller/products = still hitting user-service."
