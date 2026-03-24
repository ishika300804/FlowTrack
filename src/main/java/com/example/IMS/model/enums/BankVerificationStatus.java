package com.example.IMS.model.enums;

/**
 * Bank Account Verification Status
 * Tracks bank account verification state
 */
public enum BankVerificationStatus {
    /**
     * Not yet verified
     */
    UNVERIFIED("Unverified"),
    
    /**
     * Successfully verified via penny drop or similar
     */
    VERIFIED("Verified"),
    
    /**
     * Verification attempt failed
     */
    FAILED("Failed");
    
    private final String displayName;
    
    BankVerificationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if account can be used for settlements
     */
    public boolean canUseForSettlement() {
        return this == VERIFIED;
    }
}
