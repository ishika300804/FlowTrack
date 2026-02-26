/**
 * Onboarding State Transition Tests — Prompt 4
 * =============================================
 *
 * Tests the business profile lifecycle at the SERVICE layer:
 *
 *  Life-cycle flow: DRAFT → PENDING → VERIFIED → ACTIVE
 *
 *  Transitions tested:
 *   1.  DRAFT      → PENDING   (submitForVerification — happy path)
 *   2.  REJECTED   → PENDING   (resubmit after rejection — happy path)
 *   3.  PENDING    → PENDING   (re-submit blocked — IllegalStateException)
 *   4.  VERIFIED   → PENDING   (re-submit blocked — IllegalStateException)
 *   5.  ACTIVE     → PENDING   (re-submit blocked — already verified)
 *
 *  Edit eligibility (from VerificationStatus.isEditable()):
 *   6.  DRAFT    is editable
 *   7.  REJECTED is editable
 *   8.  PENDING  is NOT editable
 *   9.  VERIFIED is NOT editable
 *
 *  Enforcement gates (from BusinessProfileService):
 *  10.  VERIFIED status → canTransact() returns true
 *  11.  PENDING  status → canTransact() throws AccessDeniedException
 *  12.  DRAFT    status → canTransact() throws AccessDeniedException
 *  13.  ACTIVE   stage  → canAccessInventory() returns true
 *  14.  no stage        → canAccessInventory() throws AccessDeniedException
 *
 * All tests are @Transactional — rolled back after each test.
 */
package com.example.IMS;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.BusinessProfileRole;
import com.example.IMS.model.Role;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.BusinessRole;
import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.OnboardingStage;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.BusinessProfileRoleRepository;
import com.example.IMS.repository.IRoleRepository;
import com.example.IMS.repository.IUserRepository;
import com.example.IMS.service.BusinessProfileService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "verification.mode=MOCK",
        "verification.auto=false",
        "feature.onboarding.hard-enforcement=false"
})
@DisplayName("Onboarding State Transition Tests (Prompt 4)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OnboardingStateTransitionTest {

    @Autowired BusinessProfileService businessProfileService;
    @Autowired BusinessProfileRepository businessProfileRepository;
    @Autowired BusinessProfileRoleRepository businessProfileRoleRepository;
    @Autowired IUserRepository userRepository;
    @Autowired IRoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User owner;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime()).substring(5, 13);

        Role retailerRole = roleRepository.findByName("ROLE_RETAILER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_RETAILER")));

        owner = new User();
        owner.setUsername("sttest" + suffix);
        owner.setEmail("sttest" + suffix + "@test.com");
        owner.setPassword(passwordEncoder.encode("password"));
        owner.setEnabled(true);
        owner.addRole(retailerRole);
        owner = userRepository.save(owner);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Save a BusinessProfile at the given status and create the OWNER role binding. */
    private BusinessProfile saveProfileWithOwner(VerificationStatus status, OnboardingStage stage) {
        BusinessProfile profile = new BusinessProfile();
        profile.setUser(owner);
        profile.setLegalBusinessName("StateTrans Corp " + suffix);
        profile.setBusinessType(BusinessType.PRIVATE_LIMITED);
        profile.setGstin("29ABCDE" + suffix.substring(0, 4) + "F1Z5");
        profile.setPanNumber("ABCDE1234F");
        profile.setRegisteredAddress("1 Transition St, Pune");
        profile.setState("Maharashtra");
        profile.setPincode("411001");
        profile.setVerificationStatus(status);
        profile.setOnboardingStage(stage);
        profile = businessProfileRepository.save(profile);

        BusinessProfileRole ownerRole = new BusinessProfileRole();
        ownerRole.setBusinessProfile(profile);
        ownerRole.setUser(owner);
        ownerRole.setRole(BusinessRole.OWNER);
        ownerRole.setIsActive(true);
        ownerRole.setGrantedAt(LocalDateTime.now());
        ownerRole.setNotes("Test owner");
        businessProfileRoleRepository.save(ownerRole);

        return profile;
    }

    // =========================================================================
    // 1. Submit for verification — happy path
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("DRAFT → PENDING: submitForVerification transitions status to PENDING")
    void draftToPending_happyPath() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.DRAFT, null);

        businessProfileService.submitForVerification(profile.getId(), owner.getId());

        BusinessProfile updated = businessProfileRepository.findById(profile.getId()).orElseThrow();
        assertEquals(VerificationStatus.PENDING, updated.getVerificationStatus(),
                "Status should change from DRAFT to PENDING after submission");
    }

    @Test
    @Order(2)
    @DisplayName("REJECTED → PENDING: rejected profile can be resubmitted")
    void rejectedToPending_happyPath() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.REJECTED, null);

        businessProfileService.submitForVerification(profile.getId(), owner.getId());

        BusinessProfile updated = businessProfileRepository.findById(profile.getId()).orElseThrow();
        assertEquals(VerificationStatus.PENDING, updated.getVerificationStatus(),
                "Rejected profile should be submittable after editing");
    }

    // =========================================================================
    // 2. Submit for verification — blocked cases (IllegalStateException)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("PENDING → PENDING: re-submission of pending profile throws IllegalStateException")
    void pendingToSubmit_alreadyPending_throwsException() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.PENDING, null);

        assertThrows(IllegalStateException.class, () ->
                businessProfileService.submitForVerification(profile.getId(), owner.getId()),
                "Submitting an already-PENDING profile must throw IllegalStateException");
    }

    @Test
    @Order(4)
    @DisplayName("VERIFIED → submit: verified profile cannot be resubmitted")
    void verifiedToSubmit_throwsException() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.VERIFIED, OnboardingStage.ACTIVE);

        assertThrows(IllegalStateException.class, () ->
                businessProfileService.submitForVerification(profile.getId(), owner.getId()),
                "Submitting a VERIFIED profile must throw IllegalStateException");
    }

    // =========================================================================
    // 3. Edit eligibility (VerificationStatus.isEditable() contract)
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("DRAFT status: isEditable() returns true")
    void draftIsEditable() {
        assertTrue(VerificationStatus.DRAFT.isEditable(),
                "DRAFT profiles must be editable");
    }

    @Test
    @Order(6)
    @DisplayName("REJECTED status: isEditable() returns true")
    void rejectedIsEditable() {
        assertTrue(VerificationStatus.REJECTED.isEditable(),
                "REJECTED profiles must be editable so user can fix and resubmit");
    }

    @Test
    @Order(7)
    @DisplayName("PENDING status: isEditable() returns false")
    void pendingIsNotEditable() {
        assertFalse(VerificationStatus.PENDING.isEditable(),
                "PENDING profiles must NOT be editable while under review");
    }

    @Test
    @Order(8)
    @DisplayName("VERIFIED status: isEditable() returns false")
    void verifiedIsNotEditable() {
        assertFalse(VerificationStatus.VERIFIED.isEditable(),
                "VERIFIED profiles must NOT be editable");
    }

    // =========================================================================
    // 4. Transaction enforcement gates
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("VERIFIED status → canTransact() returns true")
    void verifiedProfile_canTransact() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.VERIFIED, OnboardingStage.ACTIVE);

        assertTrue(businessProfileService.canTransact(profile.getId(), owner.getId()),
                "VERIFIED profiles must be able to transact");
    }

    @Test
    @Order(10)
    @DisplayName("PENDING status → canTransact() throws AccessDeniedException")
    void pendingProfile_cannotTransact() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.PENDING, null);

        assertThrows(AccessDeniedException.class, () ->
                businessProfileService.canTransact(profile.getId(), owner.getId()),
                "PENDING profiles must not be able to transact");
    }

    @Test
    @Order(11)
    @DisplayName("DRAFT status → canTransact() throws AccessDeniedException")
    void draftProfile_cannotTransact() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.DRAFT, null);

        assertThrows(AccessDeniedException.class, () ->
                businessProfileService.canTransact(profile.getId(), owner.getId()),
                "DRAFT profiles must not be able to transact");
    }

    // =========================================================================
    // 5. Inventory access gates
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("ACTIVE stage → canAccessInventory() returns true")
    void activeStage_canAccessInventory() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.VERIFIED, OnboardingStage.ACTIVE);

        assertTrue(businessProfileService.canAccessInventory(profile.getId(), owner.getId()),
                "Profiles with ACTIVE stage must be able to access inventory");
    }

    @Test
    @Order(13)
    @DisplayName("TIER1_COMPLETE stage → canAccessInventory() throws AccessDeniedException")
    void tier1Stage_cannotAccessInventory() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.VERIFIED, OnboardingStage.TIER1_COMPLETE);

        assertThrows(AccessDeniedException.class, () ->
                businessProfileService.canAccessInventory(profile.getId(), owner.getId()),
                "Profiles not yet ACTIVE must not be able to access inventory");
    }

    @Test
    @Order(14)
    @DisplayName("No stage (null) → canAccessInventory() throws AccessDeniedException")
    void noStage_cannotAccessInventory() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.PENDING, null);

        assertThrows(AccessDeniedException.class, () ->
                businessProfileService.canAccessInventory(profile.getId(), owner.getId()),
                "Profiles with no onboarding stage must not access inventory");
    }

    // =========================================================================
    // 6. isOperational composite check
    // =========================================================================

    @Test
    @Order(15)
    @DisplayName("VERIFIED + ACTIVE → isOperational() returns true")
    void verifiedAndActive_isOperational() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.VERIFIED, OnboardingStage.ACTIVE);

        assertTrue(businessProfileService.isOperational(profile.getId(), owner.getId()),
                "VERIFIED + ACTIVE profile must be operational");
    }

    @Test
    @Order(16)
    @DisplayName("PENDING + null stage → isOperational() returns false")
    void pendingNotActive_notOperational() {
        BusinessProfile profile = saveProfileWithOwner(VerificationStatus.PENDING, null);

        assertFalse(
                businessProfileService.isOperational(profile.getId(), owner.getId()),
                "PENDING profile must not be operational");
    }

    // =========================================================================
    // 7. Cross-tenant isolation — getBusinessProfile
    // =========================================================================

    @Test
    @Order(17)
    @DisplayName("Cross-tenant: user B cannot access user A's profile")
    void crossTenant_userBCannotAccessUserAProfile() {
        Role retailerRole = roleRepository.findByName("ROLE_RETAILER").orElseThrow();

        // Create user B
        User userB = new User();
        userB.setUsername("userBst" + suffix);
        userB.setEmail("userBst" + suffix + "@test.com");
        userB.setPassword(passwordEncoder.encode("password"));
        userB.setEnabled(true);
        userB.addRole(retailerRole);
        userB = userRepository.save(userB);

        // Profile belongs to owner (user A)
        BusinessProfile profileOfA = saveProfileWithOwner(VerificationStatus.DRAFT, null);

        // User B trying to access profile A
        final Long profileId = profileOfA.getId();
        final Long userBId = userB.getId();

        assertThrows(AccessDeniedException.class, () ->
                businessProfileService.getBusinessProfile(profileId, userBId),
                "User B must not be able to access User A's profile");
    }
}
