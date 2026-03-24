/**
 * Verification Persistence Test - Prompt 3
 * =========================================
 * 
 * Tests data quality and persistence for verification logs.
 * 
 * Data Quality Requirements:
 * - PAN numbers masked in request_payload JSON (e.g., "AAB***234M")
 * - Account numbers masked in request_payload JSON (e.g., "****5678")
 * - http_status_code captured correctly
 * - execution_time_ms captured correctly
 * - JSON structure valid and parseable
 * - Response data stored in response_payload
 * 
 * Scenarios:
 * - ✓ PAN masking in request payload
 * - ✓ Account number masking in request payload
 * - ✓ HTTP status code captured
 * - ✓ Execution time captured
 * - ✓ JSON structure validation
 * - ✓ Response payload stored correctly
 * 
 * Note: Uses mocked verification providers with controlled responses
 */
package com.example.IMS.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.model.BankDetails;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.model.VerificationLog;
import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.repository.BankDetailsRepository;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.IUserRepository;
import com.example.IMS.repository.VerificationLogRepository;
import com.example.IMS.service.verification.*;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {"verification.mode=MOCK", "verification.auto=false"})
@DisplayName("Verification Persistence Tests (Prompt 3)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerificationPersistenceTest {

    @Autowired
    private VerificationOrchestrator verificationOrchestrator;

    @Autowired
    private VerificationLogRepository verificationLogRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private BankDetailsRepository bankDetailsRepository;

    private BankDetails testBankDetails;

    @MockBean
    private GstVerificationService gstVerificationService;

    @MockBean
    private PanVerificationService panVerificationService;

    @MockBean
    private BankVerificationService bankVerificationService;

    private User testUser;
    private BusinessProfile testProfile;

    @BeforeEach
    void setUp() {
        // Create test user
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        while (uniqueSuffix.length() < 4) {
            uniqueSuffix = "0" + uniqueSuffix;
        }

        testUser = new User();
        testUser.setUsername("testuser" + uniqueSuffix);
        testUser.setEmail("test" + uniqueSuffix + "@example.com");
        testUser.setPassword("password123");
        testUser = userRepository.save(testUser);

        // Create test business profile
        testProfile = new BusinessProfile();
        testProfile.setUser(testUser);
        testProfile.setLegalBusinessName("Test Company");
        testProfile.setBusinessType(BusinessType.PRIVATE_LIMITED);
        testProfile.setGstin("29ABCDE" + uniqueSuffix + "F1Z5");
        testProfile.setPanNumber("AABCT1234M");
        testProfile.setRegisteredAddress("123 Test Street");
        testProfile.setState("Maharashtra");
        testProfile.setPincode("400001");
        testProfile = businessProfileRepository.save(testProfile);

        // Create bank details for account masking test
        testBankDetails = new BankDetails();
        testBankDetails.setBusinessProfile(testProfile);
        testBankDetails.setAccountNumber("1234567890");
        testBankDetails.setIfscCode("SBIN0001234");
        testBankDetails.setBankName("State Bank of India");
        testBankDetails.setAccountHolderName("Test Company");
        testBankDetails.setPrimary(true);
        testBankDetails = bankDetailsRepository.save(testBankDetails);

        // Configure mock services
        when(gstVerificationService.supports(any())).thenAnswer(inv -> {
            return inv.getArgument(0, VerificationRequest.class)
                .getVerificationType() == VerificationType.GST;
        });

        when(panVerificationService.supports(any())).thenAnswer(inv -> {
            return inv.getArgument(0, VerificationRequest.class)
                .getVerificationType() == VerificationType.PAN;
        });

        when(bankVerificationService.supports(any())).thenAnswer(inv -> {
            return inv.getArgument(0, VerificationRequest.class)
                .getVerificationType() == VerificationType.BANK;
        });
    }

    // ========================================
    // Test 1: PAN masking in request payload
    // ========================================

    @Test
    @Order(1)
    @DisplayName("PAN number masked in request_payload JSON")
    void testPersistence_PanMasking() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("panNumber", "AABCT1234M");
        responseData.put("status", "Valid");

        VerificationResult mockResult = VerificationResult.success(VerificationType.PAN, "PAN verified");
        mockResult.setData(responseData);

        when(panVerificationService.verify(any())).thenReturn(mockResult);

        // Act
        VerificationRequest request = VerificationRequest.forPan(testProfile.getId(), testProfile.getPanNumber());
        verificationOrchestrator.executeVerification(request);

        // Assert
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());

        VerificationLog log = logs.get(0);
        assertNotNull(log.getRequestPayload());

        // Verify PAN is masked in request_payload (if stored)
        String requestPayload = log.getRequestPayload();
        if (requestPayload != null && !requestPayload.isEmpty()) {
            // PAN should be masked like "AAB***234M" or "AAB****4M"
            assertFalse(requestPayload.contains(testProfile.getPanNumber()), 
                "PAN should be masked in request payload");
            
            // Check if masking pattern is present
            assertTrue(requestPayload.contains("AAB") || requestPayload.contains("***") 
                       || requestPayload.contains("****"), 
                "Request payload should contain masked PAN pattern");
        }
    }

    // ========================================
    // Test 2: Account number masking
    // ========================================

    @Test
    @Order(2)
    @DisplayName("Account number masked in request_payload JSON")
    void testPersistence_AccountMasking() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accountNumber", "1234567890");
        responseData.put("accountName", "Test Company");
        responseData.put("ifsc", "SBIN0001234");
        responseData.put("status", "Valid");

        VerificationResult mockResult = VerificationResult.success(VerificationType.BANK, "Account verified");
        mockResult.setData(responseData);

        when(bankVerificationService.verify(any())).thenReturn(mockResult);

        // Act
        VerificationRequest request = VerificationRequest.forBank(
            testProfile.getId(), testBankDetails.getId(),
            testBankDetails.getAccountNumber(), testBankDetails.getIfscCode(),
            testBankDetails.getAccountHolderName());

        verificationOrchestrator.executeVerification(request);

        // Assert
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());

        VerificationLog log = logs.get(0);
        assertNotNull(log.getRequestPayload());

        // Verify account number is masked in request_payload (if stored)
        String requestPayload = log.getRequestPayload();
        if (requestPayload != null && !requestPayload.isEmpty()) {
            // Account number should be masked like "****7890"
            assertFalse(requestPayload.contains(testBankDetails.getAccountNumber()),
                "Account number should be masked in request payload");
        }
    }

    // ========================================
    // Test 3: HTTP status code captured
    // ========================================

    @Test
    @Order(3)
    @DisplayName("HTTP status code captured in verification log")
    void testPersistence_HttpStatusCode() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("httpStatusCode", 200);
        responseData.put("gstin", testProfile.getGstin());
        responseData.put("status", "Active");

        VerificationResult mockResult = VerificationResult.success(VerificationType.GST, "GST verified");
        mockResult.setData(responseData);

        when(gstVerificationService.verify(any())).thenReturn(mockResult);

        // Act
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), testProfile.getGstin());
        verificationOrchestrator.executeVerification(request);

        // Assert
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());

        VerificationLog log = logs.get(0);

        // Verify http_status_code is captured (V1.6 migration added this column)
        assertNotNull(log.getHttpStatusCode(), "HTTP status code should be captured");
        assertTrue(log.getHttpStatusCode() > 0, "HTTP status code should be positive");
    }

    // ========================================
    // Test 4: Execution time captured
    // ========================================

    @Test
    @Order(4)
    @DisplayName("Execution time captured in verification log")
    void testPersistence_ExecutionTime() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("executionTimeMs", 250L);
        responseData.put("gstin", testProfile.getGstin());

        VerificationResult mockResult = VerificationResult.success(VerificationType.GST, "GST verified");
        mockResult.setData(responseData);

        when(gstVerificationService.verify(any())).thenReturn(mockResult);

        // Act
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), testProfile.getGstin());
        verificationOrchestrator.executeVerification(request);

        // Assert
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());

        VerificationLog log = logs.get(0);

        // Verify execution_time_ms is captured (V1.6 migration added this column)
        assertNotNull(log.getExecutionTimeMs(), "Execution time should be captured");
        assertTrue(log.getExecutionTimeMs() >= 0, "Execution time should be non-negative");
    }

    // ========================================
    // Test 5: JSON structure validation
    // ========================================

    @Test
    @Order(5)
    @DisplayName("JSON structure is valid and parseable")
    void testPersistence_JsonStructure() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("gstin", testProfile.getGstin());
        responseData.put("businessName", "Test Company");
        responseData.put("status", "Active");
        responseData.put("registrationDate", "2020-01-01");

        VerificationResult mockResult = VerificationResult.success(VerificationType.GST, "GST verified");
        mockResult.setData(responseData);

        when(gstVerificationService.verify(any())).thenReturn(mockResult);

        // Act
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), testProfile.getGstin());
        verificationOrchestrator.executeVerification(request);

        // Assert
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());

        VerificationLog log = logs.get(0);
        assertNotNull(log.getResponsePayload());

        // Verify JSON is parseable
        assertDoesNotThrow(() -> {
            JSONObject json = new JSONObject(log.getResponsePayload());
            assertNotNull(json);
            assertTrue(json.length() > 0, "JSON should contain data");
        }, "Response payload should be valid JSON");
    }

    // ========================================
    // Test 6: Response payload stored
    // ========================================

    @Test
    @Order(6)
    @DisplayName("Response payload stored correctly")
    void testPersistence_ResponsePayload() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("gstin", testProfile.getGstin());
        responseData.put("businessName", "Test Company");
        responseData.put("verificationResult", "SUCCESS");

        VerificationResult mockResult = VerificationResult.success(VerificationType.GST, "GST verified");
        mockResult.setData(responseData);

        when(gstVerificationService.verify(any())).thenReturn(mockResult);

        // Act
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), testProfile.getGstin());
        verificationOrchestrator.executeVerification(request);

        // Assert
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());

        VerificationLog log = logs.get(0);
        assertNotNull(log.getResponsePayload());

        // Verify response contains expected data
        String responsePayload = log.getResponsePayload();
        assertTrue(responsePayload.contains("gstin") || responsePayload.contains("businessName"), 
            "Response payload should contain verification data");
    }

    // ========================================
    // Test 7: Failed verification data quality
    // ========================================

    @Test
    @Order(7)
    @DisplayName("Failed verification log data quality")
    void testPersistence_FailedVerification() {
        // Arrange
        VerificationResult mockResult = VerificationResult.failure(
            VerificationType.GST, "API error", "API_ERROR");

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", "API_ERROR");
        errorData.put("httpStatusCode", 500);
        errorData.put("executionTimeMs", 150L);
        mockResult.setData(errorData);

        when(gstVerificationService.verify(any())).thenReturn(mockResult);

        // Act
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), testProfile.getGstin());
        verificationOrchestrator.executeVerification(request);

        // Assert
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());

        VerificationLog log = logs.get(0);

        // Verify failure is logged correctly
        assertEquals("FAILED", log.getVerificationResult());
        assertNotNull(log.getHttpStatusCode());
        assertNotNull(log.getExecutionTimeMs());

        // Even failures should have performance data
        assertTrue(log.getHttpStatusCode() > 0);
        assertTrue(log.getExecutionTimeMs() >= 0);
    }
}
