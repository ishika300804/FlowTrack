# ========================================
# Prompt 2 Configuration & Schema Validation
# ========================================

Write-Host "`n=== PROMPT 2 SMOKE TEST ===" -ForegroundColor Cyan

# Test 1: Verify verification_logs schema
Write-Host "`n[1/4] Verifying verification_logs schema..." -ForegroundColor Green
$schema = mysql -u root -proot -D ims -e "DESCRIBE verification_logs;" 2>$null
if ($schema -match "http_status_code" -and $schema -match "execution_time_ms") {
    Write-Host "[OK] Schema has http_status_code and execution_time_ms columns" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Missing performance tracking columns" -ForegroundColor Red
    exit 1
}

# Test 2: Check application.properties for verification config
Write-Host "`n[2/4] Checking verification configuration..." -ForegroundColor Green
$config = Get-Content "src\main\resources\application.properties" | Select-String "verification"
$configCount = ($config | Measure-Object).Count
if ($configCount -gt 10) {
    Write-Host "[OK] Found $configCount verification config properties" -ForegroundColor Green
    Write-Host "Sample config:" -ForegroundColor Yellow
    $config | Select-Object -First 5 | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
} else {
    Write-Host "[FAIL] Insufficient verification config" -ForegroundColor Red
    exit 1
}

# Test 3: Verify WebFlux dependency
Write-Host "`n[3/4] Checking WebFlux dependency..." -ForegroundColor Green
$pom = Get-Content "pom.xml" -Raw
if ($pom -match "spring-boot-starter-webflux" -and $pom -match "reactor-netty-http") {
    Write-Host "[OK] WebFlux and Reactor Netty dependencies present" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Missing WebFlux dependencies" -ForegroundColor Red
    exit 1
}

# Test 4: Check verification service classes exist
Write-Host "`n[4/4] Verifying service classes..." -ForegroundColor Green
$services = @(
    "src\main\java\com\example\IMS\service\verification\VerificationOrchestrator.java",
    "src\main\java\com\example\IMS\service\verification\GstVerificationService.java",
    "src\main\java\com\example\IMS\service\verification\PanVerificationService.java",
    "src\main\java\com\example\IMS\service\verification\BankVerificationService.java",
    "src\main\java\com\example\IMS\service\verification\CinVerificationService.java"
)

$allExist = $true
foreach ($service in $services) {
    if (Test-Path $service) {
        $filename = Split-Path $service -Leaf
        Write-Host "  [OK] $filename" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $service missing" -ForegroundColor Red
        $allExist = $false
    }
}

if (-not $allExist) {
    exit 1
}

Write-Host "`n[PASS] Configuration smoke test completed" -ForegroundColor Green -BackgroundColor Black
Write-Host "`nVerification system components:" -ForegroundColor Cyan
Write-Host "  - Database schema: READY" -ForegroundColor Green
Write-Host "  - Configuration: READY" -ForegroundColor Green
Write-Host "  - Dependencies: READY" -ForegroundColor Green
Write-Host "  - Service classes: READY" -ForegroundColor Green

Write-Host "`nNote: Runtime testing requires UI workflow." -ForegroundColor Yellow
Write-Host "Application is running at http://localhost:8080" -ForegroundColor Yellow
Write-Host "`nReady for Prompt 3 comprehensive automated testing.`n" -ForegroundColor Cyan
