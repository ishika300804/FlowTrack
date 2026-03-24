/**
 * BusinessProfileController Security & Flow Tests — Prompt 4
 * ===========================================================
 *
 * Validates the HTTP layer for business profile CRUD operations:
 *  - Unauthenticated access → 302 redirect to /login
 *  - Authenticated GET endpoints return 200 or expected redirect
 *  - POST create profile succeeds and redirects to /business-profile/status
 *  - Edit is blocked for PENDING / VERIFIED profiles
 *  - Cross-tenant isolation: user B cannot edit user A's profile
 *
 * All tests run inside a transaction that is rolled back after each test,
 * so the MySQL DB is never polluted with test data.
 */
package com.example.IMS.controller;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.Role;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.BusinessType;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.IRoleRepository;
import com.example.IMS.repository.IUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
@DisplayName("BusinessProfileController — HTTP layer tests (Prompt 4)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BusinessProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired IUserRepository userRepository;
    @Autowired IRoleRepository roleRepository;
    @Autowired BusinessProfileRepository businessProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User retailer;
    private User anotherRetailer;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime()).substring(5, 13);

        Role retailerRole = roleRepository.findByName("ROLE_RETAILER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_RETAILER")));

        retailer = new User();
        retailer.setUsername("bpctestR" + suffix);
        retailer.setEmail("bpcR" + suffix + "@test.com");
        retailer.setPassword(passwordEncoder.encode("password"));
        retailer.setEnabled(true);
        retailer.addRole(retailerRole);
        retailer = userRepository.save(retailer);

        anotherRetailer = new User();
        anotherRetailer.setUsername("bpctestR2" + suffix);
        anotherRetailer.setEmail("bpcR2" + suffix + "@test.com");
        anotherRetailer.setPassword(passwordEncoder.encode("password"));
        anotherRetailer.setEnabled(true);
        anotherRetailer.addRole(retailerRole);
        anotherRetailer = userRepository.save(anotherRetailer);
    }

    // ------------------------------------------------------------------
    // Helper: wrap request with a real User-based authentication token
    // ------------------------------------------------------------------
    private UsernamePasswordAuthenticationToken authFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    // ========= Unauthenticated access tests =========

    @Test
    @Order(1)
    @DisplayName("GET /business-profile/create — unauthenticated → redirect to login")
    void createForm_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/business-profile/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /business-profile/status — unauthenticated → redirect to login")
    void statusPage_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/business-profile/status"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ========= Authenticated GET tests =========

    @Test
    @Order(3)
    @DisplayName("GET /business-profile/create — authenticated retailer → 200 OK")
    void createForm_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/business-profile/create")
                        .with(authentication(authFor(retailer))))
                .andExpect(status().isOk())
                .andExpect(view().name("business-profile/create"))
                .andExpect(model().attributeExists("profileRequest"))
                .andExpect(model().attributeExists("businessTypes"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /business-profile/status — authenticated, no profile → shows empty profile list")
    void statusPage_noProfile_showsEmptyList() throws Exception {
        mockMvc.perform(get("/business-profile/status")
                        .with(authentication(authFor(retailer))))
                .andExpect(status().isOk())
                .andExpect(view().name("business-profile/status"))
                .andExpect(model().attribute("hasProfile", false));
    }

    @Test
    @Order(5)
    @DisplayName("GET /business-profile/edit — no profile → redirect to create")
    void editForm_noProfile_redirectsToCreate() throws Exception {
        mockMvc.perform(get("/business-profile/edit")
                        .with(authentication(authFor(retailer))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/business-profile/create"));
    }

    // ========= POST create profile tests =========

    @Test
    @Order(6)
    @DisplayName("POST /business-profile/create — valid form → redirect to status page")
    void createProfile_validForm_redirectsToStatus() throws Exception {
        mockMvc.perform(post("/business-profile/create")
                        .with(authentication(authFor(retailer)))
                        .param("legalBusinessName", "Test Corp " + suffix)
                        .param("businessType", "PRIVATE_LIMITED")
                        .param("gstin", "29ABCDE" + suffix.substring(0, 4) + "F1Z5")
                        .param("panNumber", "ABCDE1234F")
                        .param("registeredAddress", "123 Test Street, Mumbai")
                        .param("state", "Maharashtra")
                        .param("pincode", "400001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/business-profile/status"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /business-profile/create — missing required field → returns form with errors")
    void createProfile_missingRequiredField_returnsForm() throws Exception {
        mockMvc.perform(post("/business-profile/create")
                        .with(authentication(authFor(retailer)))
                        .param("legalBusinessName", "")     // blank — required
                        .param("businessType", "PRIVATE_LIMITED")
                        .param("gstin", "29ABCDE" + suffix.substring(0, 4) + "F1Z5")
                        .param("panNumber", "ABCDE1234F")
                        .param("registeredAddress", "123 Test Street")
                        .param("state", "Maharashtra")
                        .param("pincode", "400001"))
                .andExpect(status().isOk())
                .andExpect(view().name("business-profile/create"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /business-profile/create — invalid GSTIN format → returns form")
    void createProfile_invalidGstin_returnsForm() throws Exception {
        mockMvc.perform(post("/business-profile/create")
                        .with(authentication(authFor(retailer)))
                        .param("legalBusinessName", "Test Corp")
                        .param("businessType", "PRIVATE_LIMITED")
                        .param("gstin", "INVALID_GSTIN")   // bad format
                        .param("panNumber", "ABCDE1234F")
                        .param("registeredAddress", "123 Test Street")
                        .param("state", "Maharashtra")
                        .param("pincode", "400001"))
                .andExpect(status().isOk())
                .andExpect(view().name("business-profile/create"));
    }

    // ========= Edit — status-based access control =========

    @Test
    @Order(9)
    @DisplayName("GET /business-profile/edit — PENDING profile → edit blocked with error message")
    void editForm_pendingProfile_blockEdit() throws Exception {
        // Create a profile for retailer with PENDING status
        BusinessProfile pending = createProfile(retailer, "Pending Corp " + suffix, VerificationStatus.PENDING);
        businessProfileRepository.save(pending);

        mockMvc.perform(get("/business-profile/edit")
                        .param("profileId", pending.getId().toString())
                        .with(authentication(authFor(retailer))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @Order(10)
    @DisplayName("GET /business-profile/edit — DRAFT profile → edit allowed")
    void editForm_draftProfile_allowsEdit() throws Exception {
        BusinessProfile draft = createProfile(retailer, "Draft Corp " + suffix, VerificationStatus.DRAFT);
        businessProfileRepository.save(draft);

        mockMvc.perform(get("/business-profile/edit")
                        .param("profileId", draft.getId().toString())
                        .with(authentication(authFor(retailer))))
                .andExpect(status().isOk())
                .andExpect(view().name("business-profile/edit"));
    }

    @Test
    @Order(11)
    @DisplayName("GET /business-profile/edit — VERIFIED profile → edit blocked")
    void editForm_verifiedProfile_blockEdit() throws Exception {
        BusinessProfile verified = createProfile(retailer, "Verified Corp " + suffix, VerificationStatus.VERIFIED);
        businessProfileRepository.save(verified);

        mockMvc.perform(get("/business-profile/edit")
                        .param("profileId", verified.getId().toString())
                        .with(authentication(authFor(retailer))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @Order(12)
    @DisplayName("GET /business-profile/edit — REJECTED profile → edit allowed for resubmission")
    void editForm_rejectedProfile_allowsEdit() throws Exception {
        BusinessProfile rejected = createProfile(retailer, "Rejected Corp " + suffix, VerificationStatus.REJECTED);
        businessProfileRepository.save(rejected);

        mockMvc.perform(get("/business-profile/edit")
                        .param("profileId", rejected.getId().toString())
                        .with(authentication(authFor(retailer))))
                .andExpect(status().isOk())
                .andExpect(view().name("business-profile/edit"));
    }

    // ========= Cross-tenant isolation =========

    @Test
    @Order(13)
    @DisplayName("Cross-tenant: user B cannot edit user A's profile")
    void editForm_crossTenant_userB_cannotEditUserAProfile() throws Exception {
        // Create profile belonging to retailer (user A)
        BusinessProfile profileOfA = createProfile(retailer, "UserA Corp " + suffix, VerificationStatus.DRAFT);
        businessProfileRepository.save(profileOfA);

        // User B tries to access profile A's edit page via profileId
        // Expecting: redirect to create (no profile found for user B)
        mockMvc.perform(get("/business-profile/edit")
                        .param("profileId", profileOfA.getId().toString())
                        .with(authentication(authFor(anotherRetailer))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/business-profile/create"));
    }

    // ========= Helper =========

    private BusinessProfile createProfile(User owner, String name, VerificationStatus status) {
        BusinessProfile p = new BusinessProfile();
        p.setUser(owner);
        p.setLegalBusinessName(name);
        p.setBusinessType(BusinessType.PRIVATE_LIMITED);
        p.setGstin("29ABCDE" + suffix.substring(0, 4) + "F1Z5");
        p.setPanNumber("ABCDE1234F");
        p.setRegisteredAddress("123 Test St, Mumbai");
        p.setState("Maharashtra");
        p.setPincode("400001");
        p.setVerificationStatus(status);
        return p;
    }
}
