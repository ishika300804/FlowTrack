package com.example.IMS.model.enums;

/**
 * Business Profile Role Enumeration
 * Defines permission levels for multi-user business management
 * 
 * HIERARCHY (from highest to lowest permissions):
 * OWNER > ADMIN > FINANCE > OPERATIONS > AUDITOR
 * 
 * Use Cases:
 * - OWNER: Business proprietor, full control including deletion
 * - ADMIN: Trusted manager, can modify business details and manage team
 * - FINANCE: Accountant/CFO, manages bank accounts and verifications
 * - OPERATIONS: Operations manager, can view business details and inventory
 * - AUDITOR: External auditor, read-only access for compliance review
 */
public enum BusinessRole {
    
    /**
     * OWNER - Business Owner/Proprietor
     * 
     * Permissions:
     * - Full access to all business profile operations
     * - Can delete business profile
     * - Can transfer ownership to another user
     * - Can grant/revoke roles to other users
     * - Can modify all business details
     * - Can add/remove bank accounts
     * - Can initiate verifications
     * - Can access all financial data
     * 
     * Restrictions:
     * - Only ONE active owner per business profile
     * - Owner role cannot be revoked without transferring ownership first
     */
    OWNER(
        "Owner", 
        "Business owner with full access and control",
        100  // Highest permission level
    ),
    
    /**
     * ADMIN - Administrator
     * 
     * Permissions:
     * - Modify business details (except deletion)
     * - Add/remove bank accounts
     * - Initiate verifications
     * - Grant/revoke roles (except OWNER)
     * - Access all financial data
     * - Manage inventory
     * 
     * Restrictions:
     * - Cannot delete business profile
     * - Cannot transfer ownership
     * - Cannot remove the OWNER
     */
    ADMIN(
        "Administrator", 
        "Can manage business details and team members",
        80
    ),
    
    /**
     * FINANCE - Finance Manager/Accountant
     * 
     * Permissions:
     * - View business details
     * - Add/remove bank accounts
     * - Initiate bank verifications
     * - Access all financial data
     * - Generate financial reports
     * - View verification logs
     * 
     * Restrictions:
     * - Cannot modify business registration details
     * - Cannot manage team members
     * - Cannot delete business profile
     */
    FINANCE(
        "Finance Manager", 
        "Manages bank accounts and financial verifications",
        60
    ),
    
    /**
     * OPERATIONS - Operations Manager
     * 
     * Permissions:
     * - View business details (read-only)
     * - Access inventory management
     * - View bank account list (masked account numbers)
     * - Generate operational reports
     * 
     * Restrictions:
     * - Cannot modify business details
     * - Cannot add/remove bank accounts
     * - Cannot view full bank account numbers
     * - Cannot initiate verifications
     * - Cannot manage team members
     */
    OPERATIONS(
        "Operations Manager", 
        "View-only access to business details, can manage inventory",
        40
    ),
    
    /**
     * AUDITOR - External Auditor
     * 
     * Permissions:
     * - Read-only access to all data
     * - View business details
     * - View bank account list (masked)
     * - View verification logs
     * - Generate compliance reports
     * 
     * Restrictions:
     * - Cannot modify anything
     * - Cannot add/remove bank accounts
     * - Cannot initiate verifications
     * - Cannot manage team members
     * - Time-limited access (should be revoked after audit)
     */
    AUDITOR(
        "Auditor", 
        "Read-only access for compliance review",
        20  // Lowest permission level
    );
    
    private final String displayName;
    private final String description;
    private final int permissionLevel;
    
    BusinessRole(String displayName, String description, int permissionLevel) {
        this.displayName = displayName;
        this.description = description;
        this.permissionLevel = permissionLevel;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPermissionLevel() {
        return permissionLevel;
    }
    
    /**
     * Check if this role has higher or equal permissions than another role
     * 
     * @param other the role to compare against
     * @return true if this role has >= permissions
     */
    public boolean hasPermissionGreaterThanOrEqualTo(BusinessRole other) {
        return this.permissionLevel >= other.permissionLevel;
    }
    
    /**
     * Check if this role can manage (grant/revoke) another role
     * Rule: A role can only manage roles with lower permission levels
     * 
     * @param targetRole the role to be managed
     * @return true if this role can manage the target role
     */
    public boolean canManageRole(BusinessRole targetRole) {
        // OWNER can manage all roles except itself (ownership transfer is separate)
        if (this == OWNER && targetRole != OWNER) {
            return true;
        }
        
        // ADMIN can manage FINANCE, OPERATIONS, AUDITOR
        if (this == ADMIN && targetRole.permissionLevel < ADMIN.permissionLevel) {
            return true;
        }
        
        // Other roles cannot manage roles
        return false;
    }
    
    /**
     * Check if this role can perform a specific permission
     * 
     * @param requiredPermissionLevel minimum permission level required
     * @return true if this role meets the requirement
     */
    public boolean hasPermission(int requiredPermissionLevel) {
        return this.permissionLevel >= requiredPermissionLevel;
    }
    
    /**
     * Get all roles that can be granted by this role
     * 
     * @return array of roles that this role can grant
     */
    public BusinessRole[] getGrantableRoles() {
        switch (this) {
            case OWNER:
                return new BusinessRole[]{ADMIN, FINANCE, OPERATIONS, AUDITOR};
            case ADMIN:
                return new BusinessRole[]{FINANCE, OPERATIONS, AUDITOR};
            default:
                return new BusinessRole[]{};
        }
    }
    
    /**
     * Check if this is a read-only role
     * 
     * @return true if role has no write permissions
     */
    public boolean isReadOnly() {
        return this == AUDITOR || this == OPERATIONS;
    }
    
    /**
     * Check if this role can manage financial operations
     * 
     * @return true if role can add/modify bank accounts
     */
    public boolean canManageFinances() {
        return this == OWNER || this == ADMIN || this == FINANCE;
    }
    
    /**
     * Check if this role can modify business registration details
     * 
     * @return true if role can modify GSTIN, PAN, etc.
     */
    public boolean canModifyBusinessDetails() {
        return this == OWNER || this == ADMIN;
    }
    
    /**
     * Check if this role can delete the business profile
     * 
     * @return true if role can delete
     */
    public boolean canDeleteBusiness() {
        return this == OWNER;
    }
    
    /**
     * Get permission matrix as string (for documentation/logging)
     * 
     * @return formatted permission description
     */
    public String getPermissionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(displayName).append(" (").append(name()).append("):\n");
        sb.append("  Level: ").append(permissionLevel).append("/100\n");
        sb.append("  Can manage finances: ").append(canManageFinances()).append("\n");
        sb.append("  Can modify details: ").append(canModifyBusinessDetails()).append("\n");
        sb.append("  Can delete business: ").append(canDeleteBusiness()).append("\n");
        sb.append("  Read-only: ").append(isReadOnly()).append("\n");
        return sb.toString();
    }
}
