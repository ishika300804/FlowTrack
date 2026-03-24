# Verification System Quick Start Guide

## 🚀 Getting Started

### 1. Verify Dependencies Installed
```bash
# Run Maven to download WebFlux and Reactor dependencies
mvn clean install -DskipTests
```

### 2. Verify Prompt 1 Still Works
```bash
# Run security checkpoint tests
mvn test -Dtest=SecurityCheckpointVerificationTest
```
Expected output: ✅ **12/12 tests passing**

### 3. Check Application Properties
Open `src/main/resources/application.properties` and verify:
```properties
verification.mode=MOCK    # Start with Mock mode for testing
verification.auto=true    # Enable auto-trigger
```

---

## 📋 Testing Checklist

### Test 1: Auto-Trigger Verification (Mock Mode)
**Scenario**: Create a business profile and verify auto-trigger works

1. Start the application:
   ```bash
   mvn spring-boot:run
   ```

2. Navigate to business profile creation page

3. Fill in the form with **Mock-valid data**:
   - GSTIN: `29ABCDE1234F1Z5` (starts with 29 ✅)
   - PAN: `ABCDE1234F` (ends with F ✅)
   - CIN: `U74999KA2020PTC123456` (contains PTC ✅)

4. Submit the form

5. Check application logs for:
   ```
   INFO - Auto-triggered 3 verifications for business profile {id}
   INFO - User {userId} manually triggered GST verification...
   INFO - User {userId} manually triggered PAN verification...
   INFO - User {userId} manually triggered CIN verification...
   ```

6. Verify database entries:
   ```sql
   SELECT 
       verification_type, 
       verification_result, 
       result_message,
       created_at
   FROM verification_logs 
   WHERE business_profile_id = {profile_id}
   ORDER BY created_at DESC;
   ```
   Expected: 3 rows (GST, PAN, CIN) with `verification_result = 'SUCCESS'`

---

### Test 2: Mock Validation Rules
**Scenario**: Test Mock mode validation logic

| Verification Type | Valid Example | Invalid Example | Mock Rule |
|------------------|---------------|-----------------|-----------|
| **GST** | `29ABCDE1234F1Z5` | `27ABCDE1234F1Z5` | Starts with "29" |
| **PAN** | `ABCDE1234F` | `ABCDE1234P` | Ends with "F" or "C" |
| **Bank** | `123456789012` | `123456789013` | Ends with even digit |
| **CIN** | `U74999KA2020PTC123456` | `U74999KA2020FLC123456` | Contains "PTC" or "PLC" |

**Steps**:
1. Create business profiles with both valid and invalid data (according to Mock rules)
2. Verify Mock mode returns SUCCESS for valid cases, FAILURE for invalid cases
3. Check `verification_logs` table for correct `verification_result` values

---

### Test 3: Manual Retry Verification
**Scenario**: Manually retry GST verification after profile creation

1. Get existing business profile ID (e.g., from successful Test 1)

2. Call manual retry (via controller or service layer):
   ```java
   // In your test code or controller
   VerificationResult result = businessProfileService.verifyGst(profileId, userId);
   
   // Check result
   if (result.isSuccess()) {
       System.out.println("GST Verification SUCCESS: " + result.getMessage());
       System.out.println("Data: " + result.getData());
   } else {
       System.out.println("GST Verification FAILED: " + result.getMessage());
   }
   ```

3. Verify database logs show multiple attempts:
   ```sql
   SELECT 
       verification_type,
       verification_result,
       created_at
   FROM verification_logs 
   WHERE business_profile_id = {profile_id} 
   AND verification_type = 'GST'
   ORDER BY created_at DESC;
   ```
   Expected: Multiple rows for GST (auto-trigger + manual retry)

---

### Test 4: Bank Verification (Penny Drop)
**Scenario**: Add a bank account and verify bank verification is triggered

1. Navigate to "Add Bank Account" page for a business profile

2. Fill in bank details with **Mock-valid data**:
   - Account Holder Name: `John Doe`
   - Account Number: `123456789012` (ends with even digit 2 ✅)
   - IFSC Code: `SBIN0001234` (valid IFSC format)
   - Bank Name: `State Bank of India`

3. Submit the form

4. Check application logs for:
   ```
   INFO - Added bank account {id} to business profile {profileId}
   INFO - Bank verification successful for account {id}
   ```

5. Verify database entry:
   ```sql
   SELECT 
       verification_type,
       verification_result,
       request_payload,
       response_payload
   FROM verification_logs 
   WHERE business_profile_id = {profile_id} 
   AND verification_type = 'BANK'
   ORDER BY created_at DESC 
   LIMIT 1;
   ```
   Expected: 1 row with `verification_result = 'SUCCESS'`

6. Verify account number is **masked** in logs:
   ```sql
   SELECT request_payload 
   FROM verification_logs 
   WHERE verification_type = 'BANK' 
   LIMIT 1;
   ```
   Expected JSON: `{"accountNumber":"12******9012","ifsc":"SBIN0001234",...}`
   (shows first 2 and last 4 digits only)

---

### Test 5: Verification History Query
**Scenario**: Retrieve verification history for a business profile

**SQL Query**:
```sql
SELECT 
    vl.id,
    vl.verification_type,
    vl.verification_result,
    vl.result_message,
    vl.http_status_code,
    vl.execution_time_ms,
    vl.created_at,
    bp.legal_business_name
FROM verification_logs vl
JOIN business_profiles bp ON vl.business_profile_id = bp.id
WHERE vl.business_profile_id = {profile_id}
ORDER BY vl.created_at DESC;
```

**Expected Output**:
| ID | Type | Result | Message | HTTP Status | Execution Time | Created At |
|----|------|--------|---------|-------------|---------------|------------|
| 15 | BANK | SUCCESS | Bank account verified | 200 | 245 | 2026-02-25 14:30:15 |
| 14 | CIN | SUCCESS | Company verified | 200 | 1203 | 2026-02-25 14:25:10 |
| 13 | PAN | SUCCESS | PAN verified | 200 | 1050 | 2026-02-25 14:25:09 |
| 12 | GST | SUCCESS | GSTIN verified | 200 | 980 | 2026-02-25 14:25:08 |

---

### Test 6: Data Masking Verification
**Scenario**: Verify sensitive data is masked in verification logs

**SQL Query**:
```sql
SELECT 
    verification_type,
    request_payload,
    response_payload
FROM verification_logs 
WHERE business_profile_id = {profile_id}
ORDER BY created_at DESC;
```

**Expected Masking Patterns**:
1. **GST Request**: 
   ```json
   {"gstin":"29AB******F1"}
   ```
   Shows first 4 and last 2 characters

2. **PAN Request**:
   ```json
   {"pan":"AB******F"}
   ```
   Shows first 2 and last 1 character

3. **Bank Request**:
   ```json
   {"accountNumber":"12******9012","ifsc":"SBIN0001234"}
   ```
   Shows first 2 and last 4 digits

4. **API Key in WebClient Logs** (check application.log):
   ```
   DEBUG - Request Header: X-API-Key=[MASKED]
   ```

---

## 🔄 Switching to Real Mode

### Prerequisites
1. **Obtain API keys** from verification providers:
   - GST: Signzy, Karza, AuthBridge
   - PAN: Same providers
   - Bank: Razorpay, Cashfree, PayU
   - CIN: MCA API or third-party

2. **Update application.properties**:
   ```properties
   # Switch to Real mode
   verification.mode=REAL
   
   # Add real API URLs and keys
   verification.gst.api.url=https://api.signzy.com/gst/verify
   verification.gst.api.key=your_actual_gst_api_key
   
   verification.pan.api.url=https://api.signzy.com/pan/verify
   verification.pan.api.key=your_actual_pan_api_key
   
   verification.bank.api.url=https://api.razorpay.com/v1/fund_accounts/validations
   verification.bank.api.key=your_actual_razorpay_key
   
   verification.cin.api.url=https://api.signzy.com/cin/verify
   verification.cin.api.key=your_actual_cin_api_key
   ```

3. **Restart application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Test with REAL documents**:
   - Use actual GSTIN, PAN, CIN numbers
   - Use actual bank account details
   - Verify API calls are made (check network tab or application logs)

---

## 🐛 Troubleshooting

### Issue 1: Auto-trigger not working
**Symptoms**: No verification logs after profile creation

**Solution**:
1. Check `verification.auto=true` in application.properties
2. Check application logs for errors
3. Verify business profile has GSTIN, PAN, or CIN fields populated
4. Check database for verification_logs entries

---

### Issue 2: WebClient timeout errors
**Symptoms**: `ReadTimeoutException` in logs

**Solution**:
1. Increase timeout in application.properties:
   ```properties
   verification.gst.timeout-ms=60000  # 60 seconds
   verification.webclient.read-timeout=60000
   ```
2. Check if external API is reachable
3. Verify network connectivity

---

### Issue 3: Compilation errors after integration
**Symptoms**: "VerificationRequest cannot be resolved"

**Solution**:
1. Verify imports use correct package:
   ```java
   import com.example.IMS.dto.verification.VerificationRequest;
   import com.example.IMS.dto.verification.VerificationResult;
   ```
   NOT:
   ```java
   import com.example.IMS.service.verification.dto.*; // WRONG
   ```

2. Refresh Maven dependencies:
   ```bash
   mvn clean install -DskipTests
   ```

---

### Issue 4: Tests failing after Prompt 2 integration
**Symptoms**: SecurityCheckpointVerificationTest fails

**Solution**:
1. Run tests to see specific failure:
   ```bash
   mvn test -Dtest=SecurityCheckpointVerificationTest
   ```

2. Check if verification.auto is causing side effects:
   ```properties
   # Temporarily disable for tests
   verification.auto=false
   ```

3. Verify all 12 tests pass:
   - ✅ canTransact blocks DRAFT profiles
   - ✅ canAccessInventory enforces ACTIVE stage
   - ✅ canUseForSettlement requires verified primary bank
   - ✅ Tenant isolation prevents cross-tenant access
   - ✅ RBAC enforcement at service layer
   - ✅ Encryption verification (PAN, account number)

---

## 📊 Monitoring and Observability

### 1. Check Verification Logs
```sql
-- All verifications in last 24 hours
SELECT 
    verification_type,
    verification_result,
    COUNT(*) as total_attempts,
    AVG(execution_time_ms) as avg_execution_time
FROM verification_logs 
WHERE created_at >= NOW() - INTERVAL 24 HOUR
GROUP BY verification_type, verification_result
ORDER BY verification_type, verification_result;
```

### 2. Success Rate by Type
```sql
SELECT 
    verification_type,
    SUM(CASE WHEN verification_result = 'SUCCESS' THEN 1 ELSE 0 END) as success_count,
    COUNT(*) as total_count,
    ROUND(SUM(CASE WHEN verification_result = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM verification_logs
GROUP BY verification_type;
```

### 3. Failed Verifications (Last 7 Days)
```sql
SELECT 
    vl.id,
    bp.legal_business_name,
    vl.verification_type,
    vl.result_message,
    vl.error_details,
    vl.created_at
FROM verification_logs vl
JOIN business_profiles bp ON vl.business_profile_id = bp.id
WHERE vl.verification_result != 'SUCCESS'
AND vl.created_at >= NOW() - INTERVAL 7 DAY
ORDER BY vl.created_at DESC;
```

### 4. Application Logs (Verification-specific)
```bash
# Search for verification-related logs
grep -i "verification" logs/application.log | tail -50

# Count verification attempts
grep -i "verification" logs/application.log | wc -l
```

---

## ✅ Final Verification Checklist

- [ ] **Prompt 1 tests passing** (12/12)
- [ ] **All verification services compiled** (no errors)
- [ ] **Application starts successfully** (mvn spring-boot:run)
- [ ] **Mock mode works** (valid data returns SUCCESS)
- [ ] **Auto-trigger works** (logs show "Auto-triggered X verifications")
- [ ] **Manual retry works** (service methods return VerificationResult)
- [ ] **Bank verification works** (triggered after account creation)
- [ ] **Data masking works** (sensitive data masked in verification_logs)
- [ ] **Encryption still works** (pan_number and account_number encrypted in database)
- [ ] **Tenant isolation works** (users cannot access other tenants' verifications)

---

## 🎯 Next Actions (Optional)

1. **Create Verification UI** (Thymeleaf):
   - Show verification status badges on business profile page
   - Add "Retry Verification" buttons
   - Display verification history timeline

2. **Create REST API Endpoints**:
   - `POST /api/business-profiles/{id}/verify/{type}`
   - `GET /api/business-profiles/{id}/verification-history`
   - `GET /api/business-profiles/{id}/verification-status`

3. **Implement Scheduled Jobs**:
   - Auto-retry failed verifications after 24 hours
   - Update `verification_status` field based on verification results
   - Send email notifications for successful/failed verifications

4. **Add Unit Tests**:
   - Test each verification service in Mock and Real modes
   - Test WebClient retry logic
   - Test data masking methods
   - Test orchestrator routing

5. **Production Readiness**:
   - Load test WebClient with high concurrency
   - Monitor connection pool usage
   - Set up alerts for failed verifications
   - Create runbook for troubleshooting

---

## 📞 Support

For questions or issues:
1. Check PROMPT_2_IMPLEMENTATION_SUMMARY.md for detailed documentation
2. Review application logs: `logs/application.log`
3. Check database: `SELECT * FROM verification_logs ORDER BY created_at DESC LIMIT 10;`
4. Verify configuration: `application.properties` has all required properties

---

**Last Updated**: February 25, 2026  
**Version**: Prompt 2 - Complete Implementation  
**Status**: ✅ Production Ready (Mock Mode)  
**Next**: Switch to Real mode with actual API keys
