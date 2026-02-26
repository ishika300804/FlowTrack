/**
 * OnboardingAdminController Security & Flow Tests — Prompt 4
 * ===========================================================
 *
 * Tests admin-only onboarding management endpoints:
 *
 * Access control:
 *  - ROLE_PLATFORM_ADMIN → all /admin/onboarding/* endpoints return 200
 *  - Other roles (ROLE_RETAILER) → 403 Forbidden
 *  - Unauthenticated → 302 redirect to /login
 *
 * Functional:
 *  - Dashboard shows metrics model attributes
 *  - Legacy-users page lists users without profiles
 *  - Status report renders breakdown maps
 *  - Pending-approvals lists PENDING profiles
 *  - POST /approve/{id} → transitions to VERIFIED + ACTIVE; returns JSON {success: true}
 *  - POST /reject/{id}  → transitions to REJECTED; returns JSON {success: true}
 *  - Non-existent profile → returns JSON {success: false}
 */
package com.example.IMS.controller;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.Role;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.OnboardingStage;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.IRoleRepository;
import com.example.IMS.repository.IUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "verification.mode=MOCK",
        "verification.auto=false",
        "feature.onboarding.hard-enforcement=false",
        "feature.onboarding.soft-enforcement=false"
})
@DisplayName("OnboardingAdminController — access control & flow tests (Prompt 4)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnboardingAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired IUserRepository userRepository;
    @Autowired IRoleRepository roleRepository;
    @Autowired BusinessProfileRepository businessProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User adminUser;
    private User retailerUser;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime()).substring(5, 13);

        Role adminRole = roleRepository.findByName("ROLE_PLATFORM_ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_PLATFORM_ADMIN not found — run V1_7 migration first"));

        Role retailerRole = roleRepository.findByName("ROLE_RETAILER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_RETAILER")));

        // Create test admin user
        adminUser = new User();
        adminUser.setUsername("admtest" + suffix);
        adminUser.setEmail("admtest" + suffix + "@test.com");
        adminUser.setPassword(passwordEncoder.encode("password"));
        adminUser.setEnabled(true);
        adminUser.addRole(adminRole);
        adminUser = userRepository.save(adminUser);

        // Create test retailer (non-admin) user
        retailerUser = new User();
        retailerUser.setUsername("rettest" + suffix);
        retailerUser.setEmail("rettest" + suffix + "@test.com");
        retailerUser.setPassword(passwordEncoder.encode("password"));
        retailerUser.setEnabled(true);
        retailerUser.addRole(retailerRole);
        retailerUser = userRepository.save(retailerUser);
    }

    private UsernamePasswordAuthenticationToken authFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    // ====================================================================
    // 1. Access Control — unauthenticated
    // ====================================================================

    @Test
    @Order(1)
    @DisplayName("GET /admin/onboarding/dashboard — unauthenticated → redirect to login")
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/onboarding/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /admin/onboarding/legacy-users — unauthenticated → redirect to login")
    void legacyUsers_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/onboarding/legacy-users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ====================================================================
    // 2. Access Control — non-admin (ROLE_RETAILER) blocked
    // ====================================================================

    @Test
    @Order(3)
    @DisplayName("GET /admin/onboarding/dashboard — ROLE_RETAILER → 403 Forbidden")
    void dashboard_retailer_returns403() throws Exception {
        mockMvc.perform(get("/admin/onboarding/dashboard")
                        .with(authentication(authFor(retailerUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("GET /admin/onboarding/legacy-users — ROLE_RETAILER → 403 Forbidden")
    void legacyUsers_retailer_returns403() throws Exception {
        mockMvc.perform(get("/admin/onboarding/legacy-users")
                        .with(authentication(authFor(retailerUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("GET /admin/onboarding/status-report — ROLE_RETAILER → 403 Forbidden")
    void statusReport_retailer_returns403() throws Exception {
        mockMvc.perform(get("/admin/onboarding/status-report")
                        .with(authentication(authFor(retailerUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("GET /admin/onboarding/pending-approvals — ROLE_RETAILER → 403 Forbidden")
    void pendingApprovals_retailer_returns403() throws Exception {
        mockMvc.perform(get("/admin/onboarding/pending-approvals")
                        .with(authentication(authFor(retailerUser))))
                .andExpect(status().isForbidden());
    }

    // ====================================================================
    // 3. Admin access — all pages render successfully
    // ====================================================================

    @Test
    @Order(7)
    @DisplayName("GET /admin/onboarding/dashboard — ROLE_PLATFORM_ADMIN → 200, model populated")
    void dashboard_admin_returns200WithMetrics() throws Exception {
        mockMvc.perform(get("/admin/onboarding/dashboard")
                        .with(authentication(authFor(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/onboarding-dashboard"))
                .andExpect(model().attributeExists("metrics"))
                .andExpect(model().attributeExists("featureFlags"));
    }

    @Test
    @Order(8)
    @DisplayName("GET /admin/onboarding/legacy-users — ROLE_PLATFORM_ADMIN → 200, lists legacy users")
    void legacyUsers_admin_returns200WithList() throws Exception {
        mockMvc.perform(get("/admin/onboarding/legacy-users")
                        .with(authentication(authFor(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/legacy-users"))
                .andExpect(model().attributeExists("legacyUsers"))
                .andExpect(model().attributeExists("totalLegacyUsers"));
    }

    @Test
    @Order(9)
    @DisplayName("GET /admin/onboarding/status-report — ROLE_PLATFORM_ADMIN → 200, breakdown maps present")
    void statusReport_admin_returns200WithBreakdowns() throws Exception {
        mockMvc.perform(get("/admin/onboarding/status-report")
                        .with(authentication(authFor(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/onboarding-status-report"))
                .andExpect(model().attributeExists("statusBreakdown"))
                .andExpect(model().attributeExists("stageBreakdown"));
    }

    @Test
    @Order(10)
    @DisplayName("GET /admin/onboarding/pending-approvals — ROLE_PLATFORM_ADMIN → 200")
    void pendingApprovals_admin_returns200() throws Exception {
        mockMvc.perform(get("/admin/onboarding/pending-approvals")
                        .with(authentication(authFor(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/pending-approvals"))
                .andExpect(model().attributeExists("pendingProfiles"));
    }

    // ====================================================================
    // 4. Approve flow
    // ====================================================================

    @Test
    @Order(11)
    @DisplayName("POST /admin/onboarding/approve/{id} — admin → JSON success, profile is VERIFIED+ACTIVE")
    void approveProfile_admin_successAndStatusChanges() throws Exception {
        BusinessProfile pending = savePendingProfile(retailerUser, "ToApprove " + suffix);

        mockMvc.perform(post("/admin/onboarding/approve/" + pending.getId())
                        .with(authentication(authFor(adminUser)))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"success\":true")));

        // Verify DB change
        BusinessProfile updated = businessProfileRepository.findById(pending.getId()).orElseThrow();
        Assertions.assertEquals(VerificationStatus.VERIFIED, updated.getVerificationStatus());
        Assertions.assertEquals(OnboardingStage.ACTIVE, updated.getOnboardingStage());
    }

    @Test
    @Order(12)
    @DisplayName("POST /admin/onboarding/approve/{id} — ROLE_RETAILER → 403 Forbidden")
    void approveProfile_retailer_returns403() throws Exception {
        BusinessProfile pending = savePendingProfile(retailerUser, "ToApprove2 " + suffix);

        mockMvc.perform(post("/admin/onboarding/approve/" + pending.getId())
                        .with(authentication(authFor(retailerUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @DisplayName("POST /admin/onboarding/approve — non-existent profileId → JSON success=false")
    void approveProfile_notFound_returnsFailure() throws Exception {
        mockMvc.perform(post("/admin/onboarding/approve/999999999")
                        .with(authentication(authFor(adminUser)))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"success\":false")));
    }

    // ====================================================================
    // 5. Reject flow
    // ====================================================================

    @Test
    @Order(14)
    @DisplayName("POST /admin/onboarding/reject/{id} — admin → JSON success, profile is REJECTED")
    void rejectProfile_admin_successAndStatusChanges() throws Exception {
        BusinessProfile pending = savePendingProfile(retailerUser, "ToReject " + suffix);

        mockMvc.perform(post("/admin/onboarding/reject/" + pending.getId())
                        .param("reason", "Missing GST documents")
                        .with(authentication(authFor(adminUser)))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"success\":true")));

        // Verify DB change
        BusinessProfile updated = businessProfileRepository.findById(pending.getId()).orElseThrow();
        Assertions.assertEquals(VerificationStatus.REJECTED, updated.getVerificationStatus());
    }

    @Test
    @Order(15)
    @DisplayName("POST /admin/onboarding/reject/{id} — ROLE_RETAILER → 403 Forbidden")
    void rejectProfile_retailer_returns403() throws Exception {
        BusinessProfile pending = savePendingProfile(retailerUser, "ToReject2 " + suffix);

        mockMvc.perform(post("/admin/onboarding/reject/" + pending.getId())
                        .param("reason", "Test reason")
                        .with(authentication(authFor(retailerUser))))
                .andExpect(status().isForbidden());
    }

    // ====================================================================
    // Helper
    // ====================================================================

    private BusinessProfile savePendingProfile(User owner, String name) {
        BusinessProfile p = new BusinessProfile();
        p.setUser(owner);
        p.setLegalBusinessName(name);
        p.setBusinessType(BusinessType.PRIVATE_LIMITED);
        p.setGstin("29ABCDE" + suffix.substring(0, 4) + "F1Z5");
        p.setPanNumber("ABCDE1234F");
        p.setRegisteredAddress("1 Admin Test St, Delhi");
        p.setState("Delhi");
        p.setPincode("110001");
        p.setVerificationStatus(VerificationStatus.PENDING);
        return businessProfileRepository.save(p);
    }
}
