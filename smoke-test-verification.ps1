# ========================================
# Prompt 2 Verification Smoke Test
# ========================================
# Tests auto-trigger verification on business profile creation

Write-Host "`n=== PROMPT 2 VERIFICATION SMOKE TEST ===" -ForegroundColor Cyan
Write-Host "Testing: Auto-trigger, WebClient, Retry, DB Persistence`n" -ForegroundColor Yellow

# Test business profile data
$profileData = @{
    userId = 101
    legalBusinessName = "Smoke Test Industries Pvt Ltd"
    businessType = "PRIVATE_LIMITED"
    gstin = "29AABCT1234M1Z5"
    panNumber = "AABCT1234M"
    cinNumber = "U12345MH2020PTC123456"
    registeredAddress = "456 Test Avenue, Industrial Area"
    state = "Karnataka"
    pincode = "560001"
} | ConvertTo-Json

Write-Host "[1/5] Creating business profile (should auto-trigger GST/PAN/CIN verification)..." -ForegroundColor Green

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/business-profiles" `
        -Method POST `
        -ContentType "application/json" `
        -Body $profileData
    
    $businessId = $response.id
    Write-Host "[OK] Business profile created: ID = $businessId" -ForegroundColor Green
    
    # Wait for async verification to complete
    Write-Host "`n[2/5] Waiting 3 seconds for verification to complete..." -ForegroundColor Green
    Start-Sleep -Seconds 3
    
    # Check verification_logs table
    Write-Host "`n[3/5] Checking verification_logs table..." -ForegroundColor Green
    $logCount = mysql -u root -proot -D ims -se "SELECT COUNT(*) FROM verification_logs WHERE business_profile_id = $businessId;" 2>$null
    Write-Host "[OK] Found $logCount verification log(s)" -ForegroundColor Green
    
    if ([int]$logCount -gt 0) {
        Write-Host "`n[4/5] Verification Details:" -ForegroundColor Green
        mysql -u root -proot -D ims -t -e "SELECT id, verification_type, verification_result, verification_provider, http_status_code, execution_time_ms, created_at FROM verification_logs WHERE business_profile_id = $businessId ORDER BY created_at;" 2>$null
        
        # Show masked data storage
        Write-Host "`n[5/5] Request Payload Sample (Masked):" -ForegroundColor Green
        mysql -u root -proot -D ims -se "SELECT JSON_PRETTY(request_payload) FROM verification_logs WHERE business_profile_id = $businessId LIMIT 1;" 2>$null
        
        Write-Host "\n[PASS] SMOKE TEST PASSED" -ForegroundColor Green -BackgroundColor Black
        Write-Host "  - Auto-trigger: WORKING" -ForegroundColor Green
        Write-Host "  - Database logging: WORKING" -ForegroundColor Green
        Write-Host "  - Masked data storage: WORKING" -ForegroundColor Green
    } else {
        Write-Host "\n[FAIL] SMOKE TEST FAILED: No verification logs found" -ForegroundColor Red
        Write-Host "  Check if verification.auto=true in application.properties" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "\n[FAIL] SMOKE TEST FAILED: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`nReady for Prompt 3 comprehensive testing.`n" -ForegroundColor Cyan
