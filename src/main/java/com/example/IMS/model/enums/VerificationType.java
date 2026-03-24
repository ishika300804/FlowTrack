package com.example.IMS.model.enums;

/**
 * Verification Type for Audit Logging
 * Represents different types of verification checks
 */
public enum VerificationType {
    /**
     * GST Number Verification
     */
    GST("GST Verification"),
    
    /**
     * PAN Number Verification
     */
    PAN("PAN Verification"),
    
    /**
     * Bank Account Verification
     */
    BANK("Bank Account Verification"),
    
    /**
     * Corporate Identification Number Verification
     */
    CIN("CIN Verification"),
    
    /**
     * Udyam Registration Number Verification (MSME)
     */
    UDYAM("Udyam Verification");
    
    private final String displayName;
    
    VerificationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
