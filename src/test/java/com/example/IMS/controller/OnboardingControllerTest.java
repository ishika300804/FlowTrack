/**
 * OnboardingController HTTP Layer Tests — Prompt 4
 * =================================================
 *
 * Tests the onboarding gateway pages and the banner dismiss AJAX endpoint:
 *  - GET /onboarding/required  — gate page shown by hard-enforcement interceptor
 *  - GET /onboarding/complete  — celebration page after profile becomes ACTIVE
 *  - POST /onboarding/banner/dismiss — AJAX endpoint; updates session and returns JSON
 *
 * Scenarios checked
 * -----------------
 *  1. Unauthenticated access → 302 redirect to /login
 *  2. Authenticated with NO business profile → required page shows "no profile" flag
 *  3. Authenticated with a DRAFT profile → required page shows profile list
 *  4. Authenticated with an ACTIVE profile → complete page renders
 *  5. Banner dismiss → returns JSON {"dismissed": true} and sets session flag
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
        "feature.onboarding.soft-enforcement=true"   // soft enforcement ON to test banner
})
@DisplayName("OnboardingController — HTTP layer tests (Prompt 4)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnboardingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired IUserRepository userRepository;
    @Autowired IRoleRepository roleRepository;
    @Autowired BusinessProfileRepository businessProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User testUser;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime()).substring(5, 13);

        Role retailerRole = roleRepository.findByName("ROLE_RETAILER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_RETAILER")));

        testUser = new User();
        testUser.setUsername("onbtest" + suffix);
        testUser.setEmail("onbtest" + suffix + "@test.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setEnabled(true);
        testUser.addRole(retailerRole);
        testUser = userRepository.save(testUser);
    }

    private UsernamePasswordAuthenticationToken authFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    // ========= Unauthenticated access =========

    @Test
    @Order(1)
    @DisplayName("GET /onboarding/required — unauthenticated → redirect to login")
    void required_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/onboarding/required"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /onboarding/complete — unauthenticated → redirect to login")
    void complete_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/onboarding/complete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ========= /onboarding/required — no profile =========

    @Test
    @Order(3)
    @DisplayName("GET /onboarding/required — no profile → hasProfile=false in model")
    void required_authenticated_noProfile_showsNoProfileFlag() throws Exception {
        mockMvc.perform(get("/onboarding/required")
                        .with(authentication(authFor(testUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding/required"))
                .andExpect(model().attribute("hasProfile", false));
    }

    // ========= /onboarding/required — with DRAFT profile =========

    @Test
    @Order(4)
    @DisplayName("GET /onboarding/required — DRAFT profile → hasProfile=true, profiles populated")
    void required_authenticated_draftProfile_showsProfileList() throws Exception {
        // Create a DRAFT profile for the test user
        BusinessProfile draft = buildDraftProfile(testUser);
        businessProfileRepository.save(draft);

        mockMvc.perform(get("/onboarding/required")
                        .with(authentication(authFor(testUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding/required"))
                .andExpect(model().attribute("hasProfile", true))
                .andExpect(model().attributeExists("profiles"));
    }

    // ========= /onboarding/complete =========

    @Test
    @Order(5)
    @DisplayName("GET /onboarding/complete — authenticated → renders complete page")
    void complete_authenticated_rendersPage() throws Exception {
        // Create an ACTIVE profile to give the page something to display
        BusinessProfile active = buildDraftProfile(testUser);
        active.setVerificationStatus(VerificationStatus.VERIFIED);
        active.setOnboardingStage(OnboardingStage.ACTIVE);
        businessProfileRepository.save(active);

        mockMvc.perform(get("/onboarding/complete")
                        .with(authentication(authFor(testUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding/complete"));
    }

    // ========= /onboarding/banner/dismiss =========

    @Test
    @Order(6)
    @DisplayName("POST /onboarding/banner/dismiss — returns JSON dismissed=true")
    void dismissBanner_returnsJson() throws Exception {
        mockMvc.perform(post("/onboarding/banner/dismiss")
                        .with(authentication(authFor(testUser)))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"dismissed\":true")));
    }

    @Test
    @Order(7)
    @DisplayName("POST /onboarding/banner/dismiss — unauthenticated → redirect to login")
    void dismissBanner_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/onboarding/banner/dismiss"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ========= Helper =========

    private BusinessProfile buildDraftProfile(User owner) {
        BusinessProfile p = new BusinessProfile();
        p.setUser(owner);
        p.setLegalBusinessName("OnbTest Corp " + suffix);
        p.setBusinessType(BusinessType.PRIVATE_LIMITED);
        p.setGstin("29ABCDE" + suffix.substring(0, 4) + "F1Z5");
        p.setPanNumber("ABCDE1234F");
        p.setRegisteredAddress("42 Onboarding Lane, Delhi");
        p.setState("Delhi");
        p.setPincode("110001");
        p.setVerificationStatus(VerificationStatus.DRAFT);
        return p;
    }
}
