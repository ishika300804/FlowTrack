package com.example.IMS.model.enums;

/**
 * Business Type Classification for Business Profiles
 * Represents legal entity structure in India
 */
public enum BusinessType {
    /**
     * Sole Proprietorship - Individual ownership
     */
    PROPRIETORSHIP("Sole Proprietorship"),
    
    /**
     * Limited Liability Partnership
     */
    LLP("Limited Liability Partnership"),
    
    /**
     * Private Limited Company
     */
    PRIVATE_LIMITED("Private Limited Company"),
    
    /**
     * Partnership Firm
     */
    PARTNERSHIP("Partnership");
    
    private final String displayName;
    
    BusinessType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
