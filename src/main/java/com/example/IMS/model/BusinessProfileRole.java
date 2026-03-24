package com.example.IMS.model;

import com.example.IMS.model.enums.BusinessRole;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Business Profile Role Entity
 * Manages user roles and permissions for multi-user business profile access
 * 
 * Business Rules:
 * - One user can have only one role per business profile
 * - Only one active OWNER role per business profile (enforced by DB trigger)
 * - Roles can be granted and revoked (soft delete via is_active flag)
 * - Audit trail maintained (who granted, who revoked, when)
 * 
 * Use Cases:
 * - Business owner grants FINANCE role to accountant
 * - Business owner grants ADMIN role to operations manager
 * - External auditor granted temporary AUDITOR role
 * - Ownership transfer (revoke old OWNER, grant new OWNER)
 */
@Entity
@Table(name = "business_profile_roles")
public class BusinessProfileRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Business profile this role applies to
     * Many roles can exist for one business (different users)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_profile_id", nullable = false)
    @NotNull(message = "Business profile is required")
    private BusinessProfile businessProfile;
    
    /**
     * User who is granted this role
     * One user can have roles in multiple businesses
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
    
    /**
     * Role granted to the user for this business
     * Enum: OWNER, ADMIN, FINANCE, OPERATIONS, AUDITOR
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "Role is required")
    private BusinessRole role;
    
    /**
     * When this role was granted
     */
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;
    
    /**
     * User who granted this role
     * NULL for initial owner (auto-created during business profile creation)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;
    
    /**
     * When this role was revoked (if applicable)
     * NULL if role is still active
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    /**
     * User who revoked this role
     * NULL if role is still active
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;
    
    /**
     * Whether this role is currently active
     * Soft delete mechanism - revoked roles have is_active = FALSE
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * Optional notes about this role assignment
     * Examples: "Granted for Q4 2026 audit", "Temporary access until 2026-06-30"
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Audit timestamps
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // ========================================================================
    // Lifecycle Callbacks
    // ========================================================================
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        
        if (this.grantedAt == null) {
            this.grantedAt = now;
        }
        
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // ========================================================================
    // Business Logic Methods
    // ========================================================================
    
    /**
     * Check if this role is currently active (not revoked)
     * 
     * @return true if role is active
     */
    public boolean isCurrentlyActive() {
        return Boolean.TRUE.equals(this.isActive) && this.revokedAt == null;
    }
    
    /**
     * Revoke this role (soft delete)
     * 
     * @param revokedBy user who is revoking the role
     * @throws IllegalStateException if role is already revoked
     */
    public void revoke(User revokedBy) {
        if (!isCurrentlyActive()) {
            throw new IllegalStateException("Role is already revoked");
        }
        
        if (this.role == BusinessRole.OWNER) {
            throw new IllegalStateException("Cannot revoke OWNER role without ownership transfer");
        }
        
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
        this.revokedBy = revokedBy;
    }
    
    /**
     * Reactivate a previously revoked role
     * 
     * @param reactivatedBy user who is reactivating the role
     * @throws IllegalStateException if role is already active
     */
    public void reactivate(User reactivatedBy) {
        if (isCurrentlyActive()) {
            throw new IllegalStateException("Role is already active");
        }
        
        this.isActive = true;
        this.revokedAt = null;
        this.revokedBy = null;
        this.notes = (this.notes != null ? this.notes + " | " : "") + 
                    "Reactivated at " + LocalDateTime.now();
    }
    
    /**
     * Check if this user has permission to perform an action
     * 
     * @param requiredPermissionLevel minimum permission level required
     * @return true if user has sufficient permission
     */
    public boolean hasPermission(int requiredPermissionLevel) {
        return isCurrentlyActive() && this.role.hasPermission(requiredPermissionLevel);
    }
    
    /**
     * Check if this role can manage (grant/revoke) another role
     * 
     * @param targetRole the role to be managed
     * @return true if this role can manage the target role
     */
    public boolean canManageRole(BusinessRole targetRole) {
        return isCurrentlyActive() && this.role.canManageRole(targetRole);
    }
    
    /**
     * Transfer ownership from this OWNER role to another user
     * This role will be revoked, new OWNER role should be created separately
     * 
     * @param transferredBy user who authorized the transfer (must be current owner)
     * @throws IllegalStateException if this is not an active OWNER role
     */
    public void transferOwnership(User transferredBy) {
        if (this.role != BusinessRole.OWNER) {
            throw new IllegalStateException("Can only transfer ownership from OWNER role");
        }
        
        if (!isCurrentlyActive()) {
            throw new IllegalStateException("Cannot transfer inactive ownership");
        }
        
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
        this.revokedBy = transferredBy;
        this.notes = (this.notes != null ? this.notes + " | " : "") + 
                    "Ownership transferred at " + LocalDateTime.now();
    }
    
    // ========================================================================
    // Getters and Setters
    // ========================================================================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BusinessProfile getBusinessProfile() {
        return businessProfile;
    }
    
    public void setBusinessProfile(BusinessProfile businessProfile) {
        this.businessProfile = businessProfile;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public BusinessRole getRole() {
        return role;
    }
    
    public void setRole(BusinessRole role) {
        this.role = role;
    }
    
    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }
    
    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }
    
    public User getGrantedBy() {
        return grantedBy;
    }
    
    public void setGrantedBy(User grantedBy) {
        this.grantedBy = grantedBy;
    }
    
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
    
    public User getRevokedBy() {
        return revokedBy;
    }
    
    public void setRevokedBy(User revokedBy) {
        this.revokedBy = revokedBy;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // ========================================================================
    // Equals, HashCode, ToString
    // ========================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusinessProfileRole that = (BusinessProfileRole) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "BusinessProfileRole{" +
                "id=" + id +
                ", businessProfile=" + (businessProfile != null ? businessProfile.getId() : null) +
                ", user=" + (user != null ? user.getId() : null) +
                ", role=" + role +
                ", isActive=" + isActive +
                ", grantedAt=" + grantedAt +
                ", revokedAt=" + revokedAt +
                '}';
    }
}
