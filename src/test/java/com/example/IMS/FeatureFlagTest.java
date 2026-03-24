/**
 * Feature Flag Unit Tests — Prompt 4
 * ====================================
 *
 * Pure unit-level tests for FeatureToggleService.
 * No Spring context required — uses ReflectionTestUtils to inject @Value fields.
 *
 * Combinations verified:
 *
 *  onboarding  | soft  | hard  ‖  isSoft  | isHard
 *  ─────────────────────────────────────────────────
 *  true        | true  | true  ‖  true    | true   (full enforcement)
 *  true        | true  | false ‖  true    | false  (soft-only)
 *  true        | false | true  ‖  false   | true   (hard-only; soft disabled by flag)
 *  true        | false | false ‖  false   | false  (onboarding on, no enforcement)
 *  false       | true  | true  ‖  false   | false  (umbrella OFF — overrides everything)
 *
 * Also tests:
 *  - disableAllEnforcement() instantly turns both flags off
 *  - gracePeriodDays is returned correctly
 *  - adminOverrideEnabled is returned correctly
 */
package com.example.IMS;

import com.example.IMS.service.FeatureToggleService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FeatureToggleService — unit tests (Prompt 4)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FeatureFlagTest {

    // -------------------------------------------------------------------------
    // Helper: build a service instance with explicit flag values
    // -------------------------------------------------------------------------
    private FeatureToggleService build(boolean enabled, boolean soft, boolean hard) {
        FeatureToggleService svc = new FeatureToggleService();
        ReflectionTestUtils.setField(svc, "onboardingEnabled",     enabled);
        ReflectionTestUtils.setField(svc, "softEnforcementEnabled", soft);
        ReflectionTestUtils.setField(svc, "hardEnforcementEnabled", hard);
        ReflectionTestUtils.setField(svc, "gracePeriodDays",        30);
        ReflectionTestUtils.setField(svc, "adminOverrideEnabled",   true);
        return svc;
    }

    // =========================================================================
    // 1. isSoftEnforcementEnabled
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("softEnforcement=true + onboarding=true → isSoftEnforcementEnabled() is true")
    void softEnforcement_bothEnabled_returnsTrue() {
        FeatureToggleService svc = build(true, true, false);
        assertTrue(svc.isSoftEnforcementEnabled(),
                "Soft enforcement should be active when both onboarding and soft flags are enabled");
    }

    @Test
    @Order(2)
    @DisplayName("softEnforcement=false + onboarding=true → isSoftEnforcementEnabled() is false")
    void softEnforcement_softFlagOff_returnsFalse() {
        FeatureToggleService svc = build(true, false, false);
        assertFalse(svc.isSoftEnforcementEnabled(),
                "Soft enforcement should be inactive when the soft flag is off");
    }

    @Test
    @Order(3)
    @DisplayName("onboarding=false (umbrella OFF) → isSoftEnforcementEnabled() is false even if soft=true")
    void softEnforcement_umbrellaOff_returnsFalse() {
        FeatureToggleService svc = build(false, true, true);
        assertFalse(svc.isSoftEnforcementEnabled(),
                "Umbrella onboarding=false must disable soft enforcement regardless of the soft flag");
    }

    // =========================================================================
    // 2. isHardEnforcementEnabled
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("hardEnforcement=true + onboarding=true → isHardEnforcementEnabled() is true")
    void hardEnforcement_bothEnabled_returnsTrue() {
        FeatureToggleService svc = build(true, false, true);
        assertTrue(svc.isHardEnforcementEnabled(),
                "Hard enforcement should be active when both onboarding and hard flags are enabled");
    }

    @Test
    @Order(5)
    @DisplayName("hardEnforcement=false + onboarding=true → isHardEnforcementEnabled() is false")
    void hardEnforcement_hardFlagOff_returnsFalse() {
        FeatureToggleService svc = build(true, true, false);
        assertFalse(svc.isHardEnforcementEnabled(),
                "Hard enforcement should be inactive when the hard flag is off");
    }

    @Test
    @Order(6)
    @DisplayName("onboarding=false (umbrella OFF) → isHardEnforcementEnabled() is false even if hard=true")
    void hardEnforcement_umbrellaOff_returnsFalse() {
        FeatureToggleService svc = build(false, true, true);
        assertFalse(svc.isHardEnforcementEnabled(),
                "Umbrella onboarding=false must disable hard enforcement regardless of the hard flag");
    }

    // =========================================================================
    // 3. Full enforcement combination
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("onboarding=true, soft=true, hard=true → both enforcements active")
    void fullEnforcement_bothActive() {
        FeatureToggleService svc = build(true, true, true);
        assertTrue(svc.isSoftEnforcementEnabled(), "Soft enforcement expected ON");
        assertTrue(svc.isHardEnforcementEnabled(), "Hard enforcement expected ON");
    }

    @Test
    @Order(8)
    @DisplayName("onboarding=true, soft=false, hard=false → both enforcements inactive")
    void noEnforcement_bothInactive() {
        FeatureToggleService svc = build(true, false, false);
        assertFalse(svc.isSoftEnforcementEnabled(), "Soft enforcement expected OFF");
        assertFalse(svc.isHardEnforcementEnabled(),  "Hard enforcement expected OFF");
    }

    // =========================================================================
    // 4. disableAllEnforcement() runtime rollback
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("disableAllEnforcement() turns off both soft and hard enforcement immediately")
    void disableAllEnforcement_turnsOffBothFlags() {
        FeatureToggleService svc = build(true, true, true);

        // Pre-condition: both enforcements are ON
        assertTrue(svc.isSoftEnforcementEnabled(), "Pre-condition: soft should be ON");
        assertTrue(svc.isHardEnforcementEnabled(),  "Pre-condition: hard should be ON");

        // Trigger rollback
        svc.disableAllEnforcement();

        // Both should be OFF now — instant, no restart needed
        assertFalse(svc.isSoftEnforcementEnabled(), "After rollback: soft must be OFF");
        assertFalse(svc.isHardEnforcementEnabled(),  "After rollback: hard must be OFF");
    }

    @Test
    @Order(10)
    @DisplayName("disableAllEnforcement() does NOT disable the onboarding feature itself")
    void disableAllEnforcement_doesNotDisableOnboarding() {
        FeatureToggleService svc = build(true, true, true);

        svc.disableAllEnforcement();

        assertTrue(svc.isOnboardingEnabled(),
                "disableAllEnforcement() must only disable enforcement; onboarding feature stays enabled");
    }

    // =========================================================================
    // 5. Utility methods
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("getGracePeriodDays() returns the configured value")
    void gracePeriodDays_returnsConfiguredValue() {
        FeatureToggleService svc = new FeatureToggleService();
        ReflectionTestUtils.setField(svc, "onboardingEnabled",     true);
        ReflectionTestUtils.setField(svc, "softEnforcementEnabled", false);
        ReflectionTestUtils.setField(svc, "hardEnforcementEnabled", false);
        ReflectionTestUtils.setField(svc, "gracePeriodDays",        14);
        ReflectionTestUtils.setField(svc, "adminOverrideEnabled",   false);

        assertEquals(14, svc.getGracePeriodDays(),
                "getGracePeriodDays() must return exactly what was configured");
    }

    @Test
    @Order(12)
    @DisplayName("isAdminOverrideEnabled() returns true when configured true")
    void adminOverrideEnabled_returnsConfigured() {
        FeatureToggleService svc = build(true, false, false);
        // build() sets adminOverrideEnabled = true
        assertTrue(svc.isAdminOverrideEnabled(),
                "Admin override should be reported as enabled when configured as true");
    }

    @Test
    @Order(13)
    @DisplayName("isAdminOverrideEnabled() returns false when configured false")
    void adminOverrideDisabled_returnsFalse() {
        FeatureToggleService svc = new FeatureToggleService();
        ReflectionTestUtils.setField(svc, "onboardingEnabled",     true);
        ReflectionTestUtils.setField(svc, "softEnforcementEnabled", false);
        ReflectionTestUtils.setField(svc, "hardEnforcementEnabled", false);
        ReflectionTestUtils.setField(svc, "gracePeriodDays",        30);
        ReflectionTestUtils.setField(svc, "adminOverrideEnabled",   false);

        assertFalse(svc.isAdminOverrideEnabled(),
                "Admin override should be false when explicitly disabled");
    }

    // =========================================================================
    // 6. isOnboardingEnabled — umbrella switch
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("isOnboardingEnabled() reflects the onboarding flag directly")
    void onboardingEnabled_reflectsFlag() {
        FeatureToggleService enabled  = build(true,  false, false);
        FeatureToggleService disabled = build(false, false, false);

        assertTrue(enabled.isOnboardingEnabled(),   "Should be enabled when flag is true");
        assertFalse(disabled.isOnboardingEnabled(), "Should be disabled when flag is false");
    }
}
