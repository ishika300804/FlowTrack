package com.example.IMS.repository;

import com.example.IMS.model.enums.BusinessRole;
import com.example.IMS.model.BusinessProfileRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BusinessProfileRole entity
 * Provides tenant-scoped queries for multi-user business management
 * 
 * SECURITY NOTES:
 * - All queries should enforce tenant isolation
 * - Always check user_id or business_profile_id
 * - Filter by is_active = TRUE for current roles
 * - Include is_active = FALSE for audit trails
 */
@Repository
public interface BusinessProfileRoleRepository extends JpaRepository<BusinessProfileRole, Long> {
    
    // ========================================================================
    // Find Active Roles
    // ========================================================================
    
    /**
     * Find user's active role for a specific business profile
     * Most common query for permission checking
     * 
     * @param userId user ID
     * @param businessProfileId business profile ID
     * @return user's active role, or empty if no active role exists
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.user.id = :userId " +
           "AND bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.isActive = TRUE")
    Optional<BusinessProfileRole> findActiveRoleByUserAndBusinessProfile(
        @Param("userId") Long userId,
        @Param("businessProfileId") Long businessProfileId
    );
    
    /**
     * Find all active roles for a business profile
     * Used for listing team members
     * 
     * @param businessProfileId business profile ID
     * @return list of active roles
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.isActive = TRUE " +
           "ORDER BY bpr.role ASC, bpr.grantedAt ASC")
    List<BusinessProfileRole> findActiveRolesByBusinessProfile(
        @Param("businessProfileId") Long businessProfileId
    );
    
    /**
     * Find all business profiles where user has an active role
     * Used for listing businesses accessible to a user
     * 
     * @param userId user ID
     * @return list of active roles
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.user.id = :userId " +
           "AND bpr.isActive = TRUE " +
           "ORDER BY bpr.businessProfile.legalBusinessName ASC")
    List<BusinessProfileRole> findActiveRolesByUser(@Param("userId") Long userId);
    
    /**
     * Find active OWNER role for a business profile
     * Each business should have exactly one active owner
     * 
     * @param businessProfileId business profile ID
     * @return active owner role, or empty if none exists (data integrity issue)
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.role = 'OWNER' " +
           "AND bpr.isActive = TRUE")
    Optional<BusinessProfileRole> findActiveOwnerRole(@Param("businessProfileId") Long businessProfileId);
    
    // ========================================================================
    // Find Active Roles by Role Type
    // ========================================================================
    
    /**
     * Find all users with a specific role for a business
     * Example: Find all FINANCE users for a business
     * 
     * @param businessProfileId business profile ID
     * @param role specific role to filter
     * @return list of users with that role
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.role = :role " +
           "AND bpr.isActive = TRUE " +
           "ORDER BY bpr.grantedAt ASC")
    List<BusinessProfileRole> findActiveRolesByBusinessProfileAndRole(
        @Param("businessProfileId") Long businessProfileId,
        @Param("role") BusinessRole role
    );
    
    /**
     * Find all business profiles where user has a specific role
     * Example: Find all businesses where user is OWNER
     * 
     * @param userId user ID
     * @param role specific role to filter
     * @return list of business profile roles
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.user.id = :userId " +
           "AND bpr.role = :role " +
           "AND bpr.isActive = TRUE " +
           "ORDER BY bpr.businessProfile.legalBusinessName ASC")
    List<BusinessProfileRole> findActiveRolesByUserAndRole(
        @Param("userId") Long userId,
        @Param("role") BusinessRole role
    );
    
    // ========================================================================
    // Permission Checking Queries
    // ========================================================================
    
    /**
     * Check if user has an active role for a business profile
     * Fast existence check for authorization
     * 
     * @param userId user ID
     * @param businessProfileId business profile ID
     * @return true if user has any active role
     */
    @Query("SELECT CASE WHEN COUNT(bpr) > 0 THEN TRUE ELSE FALSE END " +
           "FROM BusinessProfileRole bpr " +
           "WHERE bpr.user.id = :userId " +
           "AND bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.isActive = TRUE")
    boolean existsActiveRoleByUserAndBusinessProfile(
        @Param("userId") Long userId,
        @Param("businessProfileId") Long businessProfileId
    );
    
    /**
     * Check if user is OWNER of a business profile
     * Used for permission-sensitive operations
     * 
     * @param userId user ID
     * @param businessProfileId business profile ID
     * @return true if user is active owner
     */
    @Query("SELECT CASE WHEN COUNT(bpr) > 0 THEN TRUE ELSE FALSE END " +
           "FROM BusinessProfileRole bpr " +
           "WHERE bpr.user.id = :userId " +
           "AND bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.role = 'OWNER' " +
           "AND bpr.isActive = TRUE")
    boolean isUserOwner(
        @Param("userId") Long userId,
        @Param("businessProfileId") Long businessProfileId
    );
    
    /**
     * Check if user has permission level >= required level
     * Used for fine-grained permission checking
     * 
     * Example: Check if user can manage finances (requires FINANCE or higher)
     * 
     * @param userId user ID
     * @param businessProfileId business profile ID
     * @param minPermissionLevel minimum permission level required (from BusinessRole.permissionLevel)
     * @return true if user has sufficient permission
     */
    @Query("SELECT CASE WHEN COUNT(bpr) > 0 THEN TRUE ELSE FALSE END " +
           "FROM BusinessProfileRole bpr " +
           "WHERE bpr.user.id = :userId " +
           "AND bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.isActive = TRUE " +
           "AND (" +
           "  (bpr.role = 'OWNER') OR " +  // 100
           "  (bpr.role = 'ADMIN' AND :minPermissionLevel <= 80) OR " +
           "  (bpr.role = 'FINANCE' AND :minPermissionLevel <= 60) OR " +
           "  (bpr.role = 'OPERATIONS' AND :minPermissionLevel <= 40) OR " +
           "  (bpr.role = 'AUDITOR' AND :minPermissionLevel <= 20)" +
           ")")
    boolean hasMinimumPermission(
        @Param("userId") Long userId,
        @Param("businessProfileId") Long businessProfileId,
        @Param("minPermissionLevel") int minPermissionLevel
    );
    
    // ========================================================================
    // Audit and History Queries
    // ========================================================================
    
    /**
     * Find all roles (active and revoked) for a business profile
     * Used for audit trails and team management history
     * 
     * @param businessProfileId business profile ID
     * @return list of all roles including revoked ones
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.businessProfile.id = :businessProfileId " +
           "ORDER BY bpr.isActive DESC, bpr.grantedAt DESC")
    List<BusinessProfileRole> findAllRolesByBusinessProfile(@Param("businessProfileId") Long businessProfileId);
    
    /**
     * Find all roles (active and revoked) for a user
     * Used for user activity history
     * 
     * @param userId user ID
     * @return list of all roles including revoked ones
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.user.id = :userId " +
           "ORDER BY bpr.isActive DESC, bpr.grantedAt DESC")
    List<BusinessProfileRole> findAllRolesByUser(@Param("userId") Long userId);
    
    /**
     * Find revoked roles for a business profile
     * Used for analyzing role changes and security audits
     * 
     * @param businessProfileId business profile ID
     * @return list of revoked roles
     */
    @Query("SELECT bpr FROM BusinessProfileRole bpr " +
           "WHERE bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.isActive = FALSE " +
           "ORDER BY bpr.revokedAt DESC")
    List<BusinessProfileRole> findRevokedRolesByBusinessProfile(@Param("businessProfileId") Long businessProfileId);
    
    /**
     * Count active members of a business profile
     * 
     * @param businessProfileId business profile ID
     * @return count of users with active roles
     */
    @Query("SELECT COUNT(bpr) FROM BusinessProfileRole bpr " +
           "WHERE bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.isActive = TRUE")
    long countActiveMembersByBusinessProfile(@Param("businessProfileId") Long businessProfileId);
    
    // ========================================================================
    // Data Integrity Queries
    // ========================================================================
    
    /**
     * Check if business profile has exactly one active owner
     * Should always return true - if false, data integrity issue
     * 
     * @param businessProfileId business profile ID
     * @return true if exactly one active owner exists
     */
    @Query("SELECT CASE WHEN COUNT(bpr) = 1 THEN TRUE ELSE FALSE END " +
           "FROM BusinessProfileRole bpr " +
           "WHERE bpr.businessProfile.id = :businessProfileId " +
           "AND bpr.role = 'OWNER' " +
           "AND bpr.isActive = TRUE")
    boolean hasExactlyOneActiveOwner(@Param("businessProfileId") Long businessProfileId);
    
    /**
     * Find business profiles with no active owner (data integrity check)
     * Should return empty list - if not, critical data issue
     * 
     * @return list of business profile IDs without active owner
     */
    @Query("SELECT DISTINCT bp.id " +
           "FROM BusinessProfile bp " +
           "WHERE NOT EXISTS (" +
           "  SELECT 1 FROM BusinessProfileRole bpr " +
           "  WHERE bpr.businessProfile.id = bp.id " +
           "  AND bpr.role = 'OWNER' " +
           "  AND bpr.isActive = TRUE" +
           ")")
    List<Long> findBusinessProfilesWithoutOwner();
    
    /**
     * Find business profiles with multiple active owners (data integrity check)
     * Should return empty list - if not, critical data issue
     * 
     * @return list of business profile IDs with multiple owners
     */
    @Query("SELECT bpr.businessProfile.id " +
           "FROM BusinessProfileRole bpr " +
           "WHERE bpr.role = 'OWNER' " +
           "AND bpr.isActive = TRUE " +
           "GROUP BY bpr.businessProfile.id " +
           "HAVING COUNT(bpr) > 1")
    List<Long> findBusinessProfilesWithMultipleOwners();
}
