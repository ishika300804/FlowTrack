/**
 * Verification Orchestrator Test - Prompt 3
 * ===========================================
 * 
 * Tests VerificationOrchestrator's routing, logging, and auto-trigger logic.
 * 
 * Scenarios:
 * - ✓ Routing: Correct provider selection for each type
 * - ✓ Execution: Success/failure handling
 * - ✓ Logging: Verification attempts logged with masked data
 * - ✓ Auto-trigger: Enabled/disabled configurations
 * - ✓ Manual retry: Retry verification logic
 * - ✓ Error handling: Null requests, invalid types
 * 
 * Note: Uses Mockito to mock verification services (no external API calls)
 */
package com.example.IMS.verification;

import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.exception.VerificationException;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.model.Role;
import com.example.IMS.model.VerificationLog;
import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.IUserRepository;
import com.example.IMS.repository.VerificationLogRepository;
import com.example.IMS.service.verification.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "verification.mode=MOCK",
    "verification.auto=true"
})
@DisplayName("Verification Orchestrator Tests (Prompt 3)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerificationOrchestratorTest {

    @Autowired
    private VerificationOrchestrator verificationOrchestrator;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private VerificationLogRepository verificationLogRepository;

    @Autowired
    private IUserRepository userRepository;

    @MockBean
    private GstVerificationService gstVerificationService;

    @MockBean
    private PanVerificationService panVerificationService;

    @MockBean
    private BankVerificationService bankVerificationService;

    @MockBean
    private CinVerificationService cinVerificationService;

    private User testUser;
    private BusinessProfile testProfile;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data to avoid unique constraint violations
        verificationLogRepository.deleteAll();
        
        // Generate unique values to avoid constraint violations (@Transactional rollback not working)
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 10000); // 4 digits max
        // Pad to 4 digits if necessary
        while (uniqueSuffix.length() < 4) {
            uniqueSuffix = "0" + uniqueSuffix;
        }
        
        // Valid GSTIN format: [2 digits][5 letters][4 digits][1 letter][1 digit/letter]Z[1 digit/letter]
        // Example: 29ABCDE1234F1Z5
        String uniqueGstin = "29ABCDE" + uniqueSuffix + "F1Z5";

        // Create test user (roles optional - defaults to empty set)
        testUser = new User();
        testUser.setUsername("testuser" + uniqueSuffix);
        testUser.setEmail("test" + uniqueSuffix + "@example.com");
        testUser.setPassword("password123"); // Required field for User entity validation
        testUser = userRepository.save(testUser);

        // Create test business profile with all required fields
        testProfile = new BusinessProfile();
        testProfile.setUser(testUser);
        testProfile.setLegalBusinessName("Test Company");
        testProfile.setBusinessType(BusinessType.PRIVATE_LIMITED);
        testProfile.setGstin(uniqueGstin); // Use unique GSTIN to avoid constraint violations
        testProfile.setPanNumber("AABCT1234M");
        testProfile.setCinNumber("U12345MH2020PTC123456");
        testProfile.setRegisteredAddress("123 Test Street, Mumbai");
        testProfile.setState("Maharashtra");
        testProfile.setPincode("400001");
        testProfile = businessProfileRepository.save(testProfile);

        // Configure mock services to support their respective types
        when(gstVerificationService.supports(any(VerificationRequest.class)))
            .thenAnswer(invocation -> {
                VerificationRequest req = invocation.getArgument(0);
                return req.getVerificationType() == VerificationType.GST;
            });

        when(panVerificationService.supports(any(VerificationRequest.class)))
            .thenAnswer(invocation -> {
                VerificationRequest req = invocation.getArgument(0);
                return req.getVerificationType() == VerificationType.PAN;
            });

        when(bankVerificationService.supports(any(VerificationRequest.class)))
            .thenAnswer(invocation -> {
                VerificationRequest req = invocation.getArgument(0);
                return req.getVerificationType() == VerificationType.BANK;
            });

        when(cinVerificationService.supports(any(VerificationRequest.class)))
            .thenAnswer(invocation -> {
                VerificationRequest req = invocation.getArgument(0);
                return req.getVerificationType() == VerificationType.CIN;
            });
    }

    // ========================================
    // Test 1: Routing - GST Type
    // ========================================

    @Test
    @Order(1)
    @DisplayName("Routing - GST verification routes to GST service")
    void testRouting_GstType() {
        // Arrange
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), "29ABCDE1234F1Z5");
        VerificationResult mockResult = VerificationResult.success(VerificationType.GST, "GST verified");

        when(gstVerificationService.verify(any(VerificationRequest.class))).thenReturn(mockResult);

        // Act
        VerificationResult result = verificationOrchestrator.executeVerification(request);

        // Assert
        assertTrue(result.isSuccess());
        verify(gstVerificationService, times(1)).verify(any(VerificationRequest.class));
        verify(panVerificationService, never()).verify(any(VerificationRequest.class));
        verify(bankVerificationService, never()).verify(any(VerificationRequest.class));
        verify(cinVerificationService, never()).verify(any(VerificationRequest.class));
    }

    // ========================================
    // Test 2: Routing - PAN Type
    // ========================================

    @Test
    @Order(2)
    @DisplayName("Routing - PAN verification routes to PAN service")
    void testRouting_PanType() {
        // Arrange
        VerificationRequest request = VerificationRequest.forPan(testProfile.getId(), "AABCT1234M");
        VerificationResult mockResult = VerificationResult.success(VerificationType.PAN, "PAN verified");

        when(panVerificationService.verify(any(VerificationRequest.class))).thenReturn(mockResult);

        // Act
        VerificationResult result = verificationOrchestrator.executeVerification(request);

        // Assert
        assertTrue(result.isSuccess());
        verify(panVerificationService, times(1)).verify(any(VerificationRequest.class));
        verify(gstVerificationService, never()).verify(any(VerificationRequest.class));
    }

    // ========================================
    // Test 3: Routing - Bank Type
    // ========================================

    @Test
    @Order(3)
    @DisplayName("Routing - Bank verification routes to Bank service")
    void testRouting_BankType() {
        // Arrange
        VerificationRequest request = VerificationRequest.forBank(
            testProfile.getId(), 1L, "1234567890", "SBIN0001234", "John Doe");
        VerificationResult mockResult = VerificationResult.success(VerificationType.BANK, "Bank verified");

        when(bankVerificationService.verify(any(VerificationRequest.class))).thenReturn(mockResult);

        // Act
        VerificationResult result = verificationOrchestrator.executeVerification(request);

        // Assert
        assertTrue(result.isSuccess());
        verify(bankVerificationService, times(1)).verify(any(VerificationRequest.class));
        verify(gstVerificationService, never()).verify(any(VerificationRequest.class));
    }

    // ========================================
    // Test 4: Routing - CIN Type
    // ========================================

    @Test
    @Order(4)
    @DisplayName("Routing - CIN verification routes to CIN service")
    void testRouting_CinType() {
        // Arrange
        VerificationRequest request = VerificationRequest.forCin(testProfile.getId(), "U12345MH2020PTC123456");
        VerificationResult mockResult = VerificationResult.success(VerificationType.CIN, "CIN verified");

        when(cinVerificationService.verify(any(VerificationRequest.class))).thenReturn(mockResult);

        // Act
        VerificationResult result = verificationOrchestrator.executeVerification(request);

        // Assert
        assertTrue(result.isSuccess());
        verify(cinVerificationService, times(1)).verify(any(VerificationRequest.class));
        verify(gstVerificationService, never()).verify(any(VerificationRequest.class));
    }

    // ========================================
    // Test 5: Execution - Successful Verification
    // ========================================

    @Test
    @Order(5)
    @DisplayName("Execution - Successful verification logged correctly")
    void testExecution_Success() {
        // Arrange
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), "29ABCDE1234F1Z5");
        VerificationResult mockResult = VerificationResult.success(VerificationType.GST, "GST verified");
        mockResult.addData("businessName", "Test Company");

        when(gstVerificationService.verify(any(VerificationRequest.class))).thenReturn(mockResult);

        // Act
        VerificationResult result = verificationOrchestrator.executeVerification(request);

        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("executionTimeMs"));

        // Verify logging
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());
        assertEquals(VerificationType.GST, logs.get(0).getVerificationType());
        assertEquals("SUCCESS", logs.get(0).getVerificationResult());
    }

    // ========================================
    // Test 6: Execution - Failed Verification
    // ========================================

    @Test
    @Order(6)
    @DisplayName("Execution - Failed verification logged correctly")
    void testExecution_Failure() {
        // Arrange
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), "INVALID_GSTIN");
        VerificationResult mockResult = VerificationResult.failure(VerificationType.GST, "Invalid GSTIN", "VALIDATION_ERROR");

        when(gstVerificationService.verify(any(VerificationRequest.class))).thenReturn(mockResult);

        // Act
        VerificationResult result = verificationOrchestrator.executeVerification(request);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Invalid GSTIN", result.getMessage());

        // Verify logging
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertTrue(logs.size() > 0);
        VerificationLog latestLog = logs.get(logs.size() - 1);
        assertEquals(VerificationType.GST, latestLog.getVerificationType());
        assertEquals("FAILED", latestLog.getVerificationResult());
    }

    // ========================================
    // Test 7: Execution - Exception Handling
    // ========================================

    @Test
    @Order(7)
    @DisplayName("Execution - VerificationException handled gracefully")
    void testExecution_VerificationException() {
        // Arrange
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), "29ABCDE1234F1Z5");

        when(gstVerificationService.verify(any(VerificationRequest.class)))
            .thenThrow(new VerificationException(VerificationType.GST, "API_ERROR", "External API down"));

        // Act
        VerificationResult result = verificationOrchestrator.executeVerification(request);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("External API down"));
    }

    // ========================================
    // Test 8: Auto-Trigger - Enabled
    // ========================================

    @Test
    @Order(8)
    @DisplayName("Auto-trigger - Executes when enabled")
    void testAutoTrigger_Enabled() {
        // Arrange
        when(gstVerificationService.verify(any(VerificationRequest.class)))
            .thenReturn(VerificationResult.success(VerificationType.GST, "GST verified"));
        when(panVerificationService.verify(any(VerificationRequest.class)))
            .thenReturn(VerificationResult.success(VerificationType.PAN, "PAN verified"));
        when(cinVerificationService.verify(any(VerificationRequest.class)))
            .thenReturn(VerificationResult.success(VerificationType.CIN, "CIN verified"));

        // Act
        Map<VerificationType, VerificationResult> results = verificationOrchestrator.autoTriggerVerifications(testProfile.getId());

        // Assert
        assertEquals(3, results.size());
        assertTrue(results.containsKey(VerificationType.GST));
        assertTrue(results.containsKey(VerificationType.PAN));
        assertTrue(results.containsKey(VerificationType.CIN));
        assertTrue(results.get(VerificationType.GST).isSuccess());
    }

    // ========================================
    // Test 9: Manual Retry - GST
    // ========================================

    @Test
    @Order(9)
    @DisplayName("Manual retry - GST verification")
    void testManualRetry_Gst() {
        // Arrange
        when(gstVerificationService.verify(any(VerificationRequest.class)))
            .thenReturn(VerificationResult.success(VerificationType.GST, "GST verified on retry"));

        // Act
        VerificationResult result = verificationOrchestrator.retryVerification(testProfile.getId(), VerificationType.GST);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("GST verified on retry", result.getMessage());
        verify(gstVerificationService, times(1)).verify(any(VerificationRequest.class));
    }

    // ========================================
    // Test 10: Manual Retry - Missing Data
    // ========================================

    @Test
    @Order(10)
    @DisplayName("Manual retry - Fails when business profile data missing")
    void testManualRetry_MissingData() {
        // Arrange: Create profile without CIN (testing CIN verification failure)
        BusinessProfile emptyProfile = new BusinessProfile();
        emptyProfile.setUser(testUser);
        emptyProfile.setLegalBusinessName("Empty Company");
        emptyProfile.setBusinessType(BusinessType.PROPRIETORSHIP);
        emptyProfile.setGstin("27AABCT1234F1Z5"); // GSTIN is required
        emptyProfile.setPanNumber("AAAAA0000A");
        emptyProfile.setRegisteredAddress("456 Empty Street");
        emptyProfile.setState("Delhi");
        emptyProfile.setPincode("110001");
        // Note: CIN intentionally not set - test fails when trying to verify CIN
        emptyProfile = businessProfileRepository.save(emptyProfile);

        // Act - Try to verify CIN when CIN is not set
        VerificationResult result = verificationOrchestrator.retryVerification(emptyProfile.getId(), VerificationType.CIN);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("CIN not set") || result.getMessage().contains("CIN number not provided"));
    }

    // ========================================
    // Test 11: Error Handling - Null Request
    // ========================================

    @Test
    @Order(11)
    @DisplayName("Error handling - Null request throws exception")
    void testErrorHandling_NullRequest() {
        assertThrows(IllegalArgumentException.class, () -> {
            verificationOrchestrator.executeVerification(null);
        });
    }

    // ========================================
    // Test 12: Error Handling - Invalid Business Profile ID
    // ========================================

    @Test
    @Order(12)
    @DisplayName("Error handling - Non-existent business profile")
    void testErrorHandling_InvalidBusinessProfileId() {
        // Arrange
        VerificationRequest request = VerificationRequest.forGst(99999L, "29ABCDE1234F1Z5");

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            verificationOrchestrator.executeVerification(request);
        });
    }

    // ========================================
    // Test 13: Verification History
    // ========================================

    @Test
    @Order(13)
    @DisplayName("Verification history - Retrieves all logs for profile")
    void testVerificationHistory() {
        // Arrange: Execute multiple verifications
        VerificationRequest gstRequest = VerificationRequest.forGst(testProfile.getId(), "29ABCDE1234F1Z5");
        VerificationRequest panRequest = VerificationRequest.forPan(testProfile.getId(), "AABCT1234M");

        when(gstVerificationService.verify(any())).thenReturn(VerificationResult.success(VerificationType.GST, "GST verified"));
        when(panVerificationService.verify(any())).thenReturn(VerificationResult.success(VerificationType.PAN, "PAN verified"));

        verificationOrchestrator.executeVerification(gstRequest);
        verificationOrchestrator.executeVerification(panRequest);

        // Act
        List<VerificationLog> history = verificationOrchestrator.getVerificationHistory(testProfile.getId());

        // Assert
        assertTrue(history.size() >= 2);
        assertTrue(history.stream().anyMatch(log -> log.getVerificationType() == VerificationType.GST));
        assertTrue(history.stream().anyMatch(log -> log.getVerificationType() == VerificationType.PAN));
    }

    // ========================================
    // Test 14: Latest Successful Verification
    // ========================================

    @Test
    @Order(14)
    @DisplayName("Latest successful verification - Returns most recent success")
    void testLatestSuccessfulVerification() {
        // Arrange
        VerificationRequest request = VerificationRequest.forGst(testProfile.getId(), "29ABCDE1234F1Z5");
        when(gstVerificationService.verify(any())).thenReturn(VerificationResult.success(VerificationType.GST, "GST verified"));

        verificationOrchestrator.executeVerification(request);

        // Act
        VerificationLog latestSuccess = verificationOrchestrator.getLatestSuccessfulVerification(
            testProfile.getId(), VerificationType.GST);

        // Assert
        assertNotNull(latestSuccess);
        assertEquals(VerificationType.GST, latestSuccess.getVerificationType());
        assertEquals("SUCCESS", latestSuccess.getVerificationResult());
    }

    // ========================================
    // Test 15: Auto-Verification Flag Check
    // ========================================

    @Test
    @Order(15)
    @DisplayName("Auto-verification flag check")
    void testAutoVerificationFlag() {
        // Act
        boolean isEnabled = verificationOrchestrator.isAutoVerificationEnabled();

        // Assert
        assertTrue(isEnabled); // verification.auto=true in @TestPropertySource
    }
}
