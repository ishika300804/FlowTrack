/**
 * Verification Manual Retry Integration Test - Prompt 3
 * =======================================================
 * 
 * Tests manual retry functionality through BusinessProfileService layer.
 * 
 * Flow:
 * 1. User calls BusinessProfileService.verifyGst/Pan/Cin()
 * 2. Service routes to VerificationOrchestrator
 * 3. Orchestrator executes verification
 * 4. Result saved to verification_logs
 * 
 * Scenarios:
 * - ✓ Manual GST verification succeeds
 * - ✓ Manual PAN verification succeeds
 * - ✓ Manual CIN verification succeeds
 * - ✓ Retry when business profile data missing
 * - ✓ Retry for non-existent business profile fails
 * 
 * Note: Uses mocked verification providers (no external API calls)
 */
package com.example.IMS.verification;

import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.BusinessProfileRole;
import com.example.IMS.model.User;
import com.example.IMS.model.VerificationLog;
import com.example.IMS.model.enums.BusinessRole;
import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.BusinessProfileRoleRepository;
import com.example.IMS.repository.IUserRepository;
import com.example.IMS.repository.VerificationLogRepository;
import com.example.IMS.service.BusinessProfileService;
import com.example.IMS.service.verification.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {"verification.mode=MOCK", "verification.auto=false"})
@DisplayName("Verification Manual Retry Integration Tests (Prompt 3)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerificationManualRetryIntegrationTest {

    @Autowired
    private BusinessProfileService businessProfileService;

    @Autowired
    private VerificationLogRepository verificationLogRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private BusinessProfileRoleRepository roleRepository;

    @MockBean
    private GstVerificationService gstVerificationService;

    @MockBean
    private PanVerificationService panVerificationService;

    @MockBean
    private CinVerificationService cinVerificationService;

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
        String uniqueGstin = "29ABCDE" + uniqueSuffix + "F1Z5";
        testProfile = new BusinessProfile();
        testProfile.setUser(testUser);
        testProfile.setLegalBusinessName("Test Company");
        testProfile.setBusinessType(BusinessType.PRIVATE_LIMITED);
        testProfile.setGstin(uniqueGstin);
        testProfile.setPanNumber("AABCT1234M");
        testProfile.setCinNumber("U12345MH2020PTC123456");
        testProfile.setRegisteredAddress("123 Test Street");
        testProfile.setState("Maharashtra");
        testProfile.setPincode("400001");
        testProfile = businessProfileRepository.save(testProfile);

        // Grant OWNER role to testUser for access control
        BusinessProfileRole ownerRole = new BusinessProfileRole();
        ownerRole.setBusinessProfile(testProfile);
        ownerRole.setUser(testUser);
        ownerRole.setRole(BusinessRole.OWNER);
        ownerRole.setIsActive(true);
        roleRepository.save(ownerRole);

        // Configure mock services
        when(gstVerificationService.supports(any())).thenAnswer(inv -> {
            return inv.getArgument(0, com.example.IMS.dto.verification.VerificationRequest.class)
                .getVerificationType() == VerificationType.GST;
        });

        when(panVerificationService.supports(any())).thenAnswer(inv -> {
            return inv.getArgument(0, com.example.IMS.dto.verification.VerificationRequest.class)
                .getVerificationType() == VerificationType.PAN;
        });

        when(cinVerificationService.supports(any())).thenAnswer(inv -> {
            return inv.getArgument(0, com.example.IMS.dto.verification.VerificationRequest.class)
                .getVerificationType() == VerificationType.CIN;
        });
    }

    // ========================================
    // Test 1: Manual GST verification
    // ========================================

    @Test
    @Order(1)
    @DisplayName("Manual GST verification succeeds")
    void testManualRetry_Gst_Success() {
        // Arrange
        when(gstVerificationService.verify(any()))
            .thenReturn(VerificationResult.success(VerificationType.GST, "GST verified"));

        // Act
        VerificationResult result = businessProfileService.verifyGst(testProfile.getId(), testUser.getId());

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(VerificationType.GST, result.getVerificationType());

        // Verify service was called
        verify(gstVerificationService, times(1)).verify(any());

        // Verify log was saved
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());
        assertEquals(VerificationType.GST, logs.get(0).getVerificationType());
        assertEquals("SUCCESS", logs.get(0).getVerificationResult());
    }

    // ========================================
    // Test 2: Manual PAN verification
    // ========================================

    @Test
    @Order(2)
    @DisplayName("Manual PAN verification succeeds")
    void testManualRetry_Pan_Success() {
        // Arrange
        when(panVerificationService.verify(any()))
            .thenReturn(VerificationResult.success(VerificationType.PAN, "PAN verified"));

        // Act
        VerificationResult result = businessProfileService.verifyPan(testProfile.getId(), testUser.getId());

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(VerificationType.PAN, result.getVerificationType());

        // Verify service was called
        verify(panVerificationService, times(1)).verify(any());

        // Verify log was saved
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());
        assertEquals(VerificationType.PAN, logs.get(0).getVerificationType());
    }

    // ========================================
    // Test 3: Manual CIN verification
    // ========================================

    @Test
    @Order(3)
    @DisplayName("Manual CIN verification succeeds")
    void testManualRetry_Cin_Success() {
        // Arrange
        when(cinVerificationService.verify(any()))
            .thenReturn(VerificationResult.success(VerificationType.CIN, "CIN verified"));

        // Act
        VerificationResult result = businessProfileService.verifyCin(testProfile.getId(), testUser.getId());

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(VerificationType.CIN, result.getVerificationType());

        // Verify service was called
        verify(cinVerificationService, times(1)).verify(any());

        // Verify log was saved
        List<VerificationLog> logs = verificationLogRepository.findByBusinessProfileId(testProfile.getId());
        assertEquals(1, logs.size());
        assertEquals(VerificationType.CIN, logs.get(0).getVerificationType());
    }

    // ========================================
    // Test 4: Retry when data missing
    // ========================================

    @Test
    @Order(4)
    @DisplayName("Manual retry fails when business profile data missing")
    void testManualRetry_MissingData() {
        // Arrange - Create profile without CIN
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        while (uniqueSuffix.length() < 4) {
            uniqueSuffix = "0" + uniqueSuffix;
        }

        BusinessProfile emptyProfile = new BusinessProfile();
        emptyProfile.setUser(testUser);
        emptyProfile.setLegalBusinessName("Empty Company");
        emptyProfile.setBusinessType(BusinessType.PROPRIETORSHIP);
        emptyProfile.setGstin("27ABCDE" + uniqueSuffix + "F1Z5");
        emptyProfile.setPanNumber("AAAAA0000A");
        emptyProfile.setRegisteredAddress("456 Empty Street");
        emptyProfile.setState("Delhi");
        emptyProfile.setPincode("110001");
        // CIN intentionally not set
        emptyProfile = businessProfileRepository.save(emptyProfile);
        
        // Grant OWNER role to testUser for this profile too
        BusinessProfileRole emptyProfileRole = new BusinessProfileRole();
        emptyProfileRole.setBusinessProfile(emptyProfile);
        emptyProfileRole.setUser(testUser);
        emptyProfileRole.setRole(BusinessRole.OWNER);
        emptyProfileRole.setIsActive(true);
        roleRepository.save(emptyProfileRole);

        // Act
        VerificationResult result = businessProfileService.verifyCin(emptyProfile.getId(), testUser.getId());

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("CIN") || result.getMessage().contains("not set") 
                   || result.getMessage().contains("missing"));

        // Verify service was NOT called
        verify(cinVerificationService, never()).verify(any());
    }

    // ========================================
    // Test 5: Retry for non-existent business profile
    // ========================================

    @Test
    @Order(5)
    @DisplayName("Manual retry fails for non-existent business profile")
    void testManualRetry_NonExistentProfile() {
        // Arrange
        Long invalidId = 999999L;

        // Act & Assert
        assertThrows(Exception.class, () -> {
            businessProfileService.verifyGst(invalidId, testUser.getId());
        });

        // Verify service was NOT called
        verify(gstVerificationService, never()).verify(any());
    }
}
