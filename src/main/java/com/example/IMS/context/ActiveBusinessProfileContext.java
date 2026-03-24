package com.example.IMS.context;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Session-scoped context for active business profile
 * Enables multi-tenant operations by tracking which business profile is currently active
 * 
 * Spring Session-Scoped Bean:
 * - One instance per HTTP session
 * - Automatically cleaned up when session expires
 * - Thread-safe for the session owner
 * 
 * Security:
 * - Validates user ID matches the user who set the active profile
 * - Prevents cross-user profile access via session
 * 
 * Use Case:
 * When a user has multiple business profiles (as OWNER or team member),
 * they select which one is "active" for current operations.
 * All tenant-scoped queries use this context to filter data.
 */
@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ActiveBusinessProfileContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * ID of the currently active business profile
     */
    private Long activeBusinessProfileId;
    
    /**
     * Name of the active business profile (for display purposes)
     */
    private String activeBusinessProfileName;
    
    /**
     * User ID who set this active profile
     * Used for security validation
     */
    private Long userId;
    
    /**
     * Timestamp when the profile was set active
     */
    private long activatedAt;
    
    /**
     * Set the active business profile for this session
     * 
     * @param profileId business profile ID
     * @param profileName business profile legal name
     * @param userId user ID setting the active profile
     */
    public void setActiveBusinessProfile(Long profileId, String profileName, Long userId) {
        this.activeBusinessProfileId = profileId;
        this.activeBusinessProfileName = profileName;
        this.userId = userId;
        this.activatedAt = System.currentTimeMillis();
    }
    
    /**
     * Check if there is an active business profile
     * 
     * @return true if a profile is active
     */
    public boolean hasActiveBusinessProfile() {
        return activeBusinessProfileId != null;
    }
    
    /**
     * Get the active business profile ID
     * 
     * @return profile ID
     * @throws IllegalStateException if no active profile
     */
    public Long getActiveBusinessProfileId() {
        if (!hasActiveBusinessProfile()) {
            throw new IllegalStateException("No active business profile selected");
        }
        return activeBusinessProfileId;
    }
    
    /**
     * Get the active business profile name
     * 
     * @return profile name
     * @throws IllegalStateException if no active profile
     */
    public String getActiveBusinessProfileName() {
        if (!hasActiveBusinessProfile()) {
            throw new IllegalStateException("No active business profile selected");
        }
        return activeBusinessProfileName;
    }
    
    /**
     * Get the user ID who set the active profile
     * 
     * @return user ID
     * @throws IllegalStateException if no active profile
     */
    public Long getUserId() {
        if (!hasActiveBusinessProfile()) {
            throw new IllegalStateException("No active business profile selected");
        }
        return userId;
    }
    
    /**
     * Validate that the requesting user matches the session user
     * Security check to prevent cross-user access
     * 
     * @param requestingUserId user ID from the request
     * @throws SecurityException if user ID doesn't match
     */
    public void validateUser(Long requestingUserId) {
        if (!hasActiveBusinessProfile()) {
            throw new IllegalStateException("No active business profile selected");
        }
        
        if (!this.userId.equals(requestingUserId)) {
            throw new SecurityException(
                "User ID mismatch. Session user: " + this.userId + ", requesting user: " + requestingUserId
            );
        }
    }
    
    /**
     * Clear the active business profile from session
     * Called on logout or explicit profile deselection
     */
    public void clearActiveBusinessProfile() {
        this.activeBusinessProfileId = null;
        this.activeBusinessProfileName = null;
        this.userId = null;
        this.activatedAt = 0;
    }
    
    /**
     * Get session information for debugging/logging
     * 
     * @return formatted session info
     */
    public String getSessionInfo() {
        if (!hasActiveBusinessProfile()) {
            return "No active business profile";
        }
        
        long activeSeconds = (System.currentTimeMillis() - activatedAt) / 1000;
        return String.format(
            "Active Profile: %s (ID: %d), User: %d, Active for: %d seconds",
            activeBusinessProfileName, activeBusinessProfileId, userId, activeSeconds
        );
    }
}
