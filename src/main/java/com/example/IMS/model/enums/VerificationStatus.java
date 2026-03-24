package com.example.IMS.model.enums;

/**
 * Business Profile Verification Status
 * Tracks the approval lifecycle of business profiles
 */
public enum VerificationStatus {
    /**
     * Initial draft state - not yet submitted
     */
    DRAFT("Draft"),
    
    /**
     * Submitted and awaiting verification
     */
    PENDING("Pending Verification"),
    
    /**
     * Successfully verified and approved
     */
    VERIFIED("Verified"),
    
    /**
     * Verification rejected
     */
    REJECTED("Rejected");
    
    private final String displayName;
    
    VerificationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if profile can perform transactions
     */
    public boolean canTransact() {
        return this == VERIFIED;
    }
    
    /**
     * Check if profile can be edited
     */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }
}
