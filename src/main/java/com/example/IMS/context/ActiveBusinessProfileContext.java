package com.example.IMS.context;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

/**
 * Active Business Profile Context
 * Manages the currently active business profile for the logged-in user
 * 
 * PURPOSE:
 * - Tenant isolation: Ensures all operations are scoped to the active business
 * - Multi-business support: User can switch between their businesses
 * - Security: Prevents cross-profile data access
 * 
 * USAGE:
 * 1. After user login, show list of their business profiles
 * 2. User selects which business to work with
 * 3. Store selected business_profile_id in this context
 * 4. All subsequent queries MUST filter by this business_profile_id
 * 
 * IMPLEMENTATION:
 * - Session-scoped bean (one instance per HTTP session)
 * - Automatically cleared when session expires
 * - Thread-safe within session boundary
 * 
 * SECURITY CONSIDERATIONS:
 * - Always validate that the business_profile_id belongs to the logged-in user
 * - Re-validate on every profile switch
 * - Clear context on logout
 * - Never trust client-side business_profile_id without server-side validation
 */
@Component
@SessionScope
public class ActiveBusinessProfileContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Currently active business profile ID
     * null = no business selected (user must select one)
     */
    private Long activeBusinessProfileId;
    
    /**
     * Business name for display purposes
     */
    private String activeBusinessName;
    
    /**
     * User ID who owns this context
     * Used for validation
     */
    private Long userId;
    
    // Constructors
    public ActiveBusinessProfileContext() {}
    
    // Getters and Setters
    public Long getActiveBusinessProfileId() {
        return activeBusinessProfileId;
    }
    
    public void setActiveBusinessProfileId(Long activeBusinessProfileId) {
        this.activeBusinessProfileId = activeBusinessProfileId;
    }
    
    public String getActiveBusinessName() {
        return activeBusinessName;
    }
    
    public void setActiveBusinessName(String activeBusinessName) {
        this.activeBusinessName = activeBusinessName;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    // Helper methods
    
    /**
     * Check if a business profile is currently active
     */
    public boolean hasActiveBusinessProfile() {
        return activeBusinessProfileId != null;
    }
    
    /**
     * Set the active business profile
     * 
     * @param businessProfileId The business profile ID to activate
     * @param businessName The business name for display
     * @param userId The user ID who owns this business
     */
    public void setActiveBusinessProfile(Long businessProfileId, String businessName, Long userId) {
        this.activeBusinessProfileId = businessProfileId;
        this.activeBusinessName = businessName;
        this.userId = userId;
    }
    
    /**
     * Clear the active business profile
     * Call this on logout or profile switch
     */
    public void clearActiveBusinessProfile() {
        this.activeBusinessProfileId = null;
        this.activeBusinessName = null;
        this.userId = null;
    }
    
    /**
     * Validate that the provided user ID matches the context user
     * 
     * @param userIdToValidate The user ID to validate
     * @return true if valid, false otherwise
     */
    public boolean validateUser(Long userIdToValidate) {
        return userId != null && userId.equals(userIdToValidate);
    }
    
    @Override
    public String toString() {
        return "ActiveBusinessProfileContext{" +
                "activeBusinessProfileId=" + activeBusinessProfileId +
                ", activeBusinessName='" + activeBusinessName + '\'' +
                ", userId=" + userId +
                '}';
    }
}
