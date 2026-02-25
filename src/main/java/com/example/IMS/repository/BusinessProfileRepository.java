package com.example.IMS.repository;

import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.model.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BusinessProfile entity
 * 
 * IMPORTANT: All queries must respect tenant isolation
 * - Always filter by user_id or business_profile_id
 * - Never allow cross-profile data access
 */
@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    
    /**
     * Find all business profiles for a specific user
     * Used for profile selection dropdown
     */
    List<BusinessProfile> findByUser(User user);
    
    /**
     * Find all business profiles by user ID
     */
    List<BusinessProfile> findByUserId(Long userId);
    
    /**
     * Find business profile by ID and user ID (tenant isolation)
     * ALWAYS use this for queries to ensure user owns the profile
     */
    Optional<BusinessProfile> findByIdAndUserId(Long id, Long userId);
    
    /**
     * Find business profile by GSTIN
     * GSTIN is unique across all businesses
     */
    Optional<BusinessProfile> findByGstin(String gstin);
    
    /**
     * Check if GSTIN already exists (for validation)
     */
    boolean existsByGstin(String gstin);
    
    /**
     * Find all verified business profiles for a user
     */
    List<BusinessProfile> findByUserIdAndVerificationStatus(Long userId, VerificationStatus status);
    
    /**
     * Get count of business profiles per user
     */
    @Query("SELECT COUNT(bp) FROM BusinessProfile bp WHERE bp.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * Find active (verified and fully onboarded) profiles for a user
     */
    @Query("SELECT bp FROM BusinessProfile bp WHERE bp.user.id = :userId " +
           "AND bp.verificationStatus = 'VERIFIED' " +
           "AND bp.onboardingStage = 'ACTIVE'")
    List<BusinessProfile> findActiveProfilesByUserId(@Param("userId") Long userId);
    
    /**
     * Find all profiles by verification status (for admin dashboard)
     */
    List<BusinessProfile> findByVerificationStatus(VerificationStatus status);
    
    /**
     * Find all profiles by onboarding stage (for admin dashboard)
     */
    List<BusinessProfile> findByOnboardingStage(com.example.IMS.model.enums.OnboardingStage stage);
}
