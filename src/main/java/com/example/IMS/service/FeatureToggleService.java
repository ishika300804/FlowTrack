package com.example.IMS.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Feature Toggle Service for Staged Rollout Strategy
 * 
 * Controls the gradual rollout of business onboarding features:
 * - Phase 1: Schema addition (zero impact)
 * - Phase 2: Soft enforcement (banners/notifications)
 * - Phase 3: Hard enforcement (conditional blocking)
 * 
 * Feature flags can be toggled without code deployment via application.properties
 */
@Service
public class FeatureToggleService {

    @Value("${feature.onboarding.enabled:true}")
    private boolean onboardingEnabled;

    @Value("${feature.onboarding.soft-enforcement:false}")
    private boolean softEnforcementEnabled;

    @Value("${feature.onboarding.hard-enforcement:false}")
    private boolean hardEnforcementEnabled;

    @Value("${feature.onboarding.grace-period-days:30}")
    private int gracePeriodDays;

    @Value("${feature.onboarding.admin-override:true}")
    private boolean adminOverrideEnabled;

    /**
     * Phase 1: Is onboarding feature available?
     * When false, all onboarding UI/endpoints are hidden
     */
    public boolean isOnboardingEnabled() {
        return onboardingEnabled;
    }

    /**
     * Phase 2: Should we show soft enforcement (banners/notifications)?
     * When true, users see reminders but can still access all features
     */
    public boolean isSoftEnforcementEnabled() {
        return onboardingEnabled && softEnforcementEnabled;
    }

    /**
     * Phase 3: Should we block features for incomplete onboarding?
     * When true, users without ACTIVE onboarding_stage are blocked from certain operations
     */
    public boolean isHardEnforcementEnabled() {
        return onboardingEnabled && hardEnforcementEnabled;
    }

    /**
     * Grace period in days before hard enforcement applies
     * Allows existing users time to complete onboarding
     */
    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    /**
     * Can admins grant temporary exemptions from enforcement?
     */
    public boolean isAdminOverrideEnabled() {
        return adminOverrideEnabled;
    }

    /**
     * Rollback: Disable all enforcement instantly
     */
    public void disableAllEnforcement() {
        this.softEnforcementEnabled = false;
        this.hardEnforcementEnabled = false;
    }
}
