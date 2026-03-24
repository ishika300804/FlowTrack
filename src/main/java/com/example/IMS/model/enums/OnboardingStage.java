package com.example.IMS.model.enums;

/**
 * Business Onboarding Stage
 * Represents progressive stages of business setup
 */
public enum OnboardingStage {
    /**
     * Tier 1 complete - Basic business information verified
     */
    TIER1_COMPLETE("Tier 1 Complete - Basic Info"),
    
    /**
     * Tier 2 complete - Financial information verified
     */
    TIER2_COMPLETE("Tier 2 Complete - Financial Info"),
    
    /**
     * Fully active - All onboarding steps completed
     */
    ACTIVE("Active");
    
    private final String displayName;
    
    OnboardingStage(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if business can perform full operations
     */
    public boolean isFullyActive() {
        return this == ACTIVE;
    }
}
