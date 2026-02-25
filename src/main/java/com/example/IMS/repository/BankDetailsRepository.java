package com.example.IMS.repository;

import com.example.IMS.model.BankDetails;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.enums.BankVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BankDetails entity
 * 
 * IMPORTANT: All queries must be scoped by business_profile_id
 * for tenant isolation
 */
@Repository
public interface BankDetailsRepository extends JpaRepository<BankDetails, Long> {
    
    /**
     * Find all bank accounts for a business profile
     */
    List<BankDetails> findByBusinessProfile(BusinessProfile businessProfile);
    
    /**
     * Find all bank accounts by business profile ID
     */
    List<BankDetails> findByBusinessProfileId(Long businessProfileId);
    
    /**
     * Find primary bank account for a business profile
     * Should return exactly one or none
     */
    Optional<BankDetails> findByBusinessProfileIdAndIsPrimaryTrue(Long businessProfileId);
    
    /**
     * Find verified bank accounts for a business profile
     */
    List<BankDetails> findByBusinessProfileIdAndBankVerificationStatus(
        Long businessProfileId, 
        BankVerificationStatus status
    );
    
    /**
     * Find bank account by ID and business profile ID (tenant isolation)
     * ALWAYS use this for queries to ensure proper scoping
     */
    Optional<BankDetails> findByIdAndBusinessProfileId(Long id, Long businessProfileId);
    
    /**
     * Check if a business has a primary bank account
     */
    boolean existsByBusinessProfileIdAndIsPrimaryTrue(Long businessProfileId);
    
    /**
     * Count bank accounts for a business profile
     */
    long countByBusinessProfileId(Long businessProfileId);
    
    /**
     * Find primary verified account for settlement
     * This is the most important query for payment operations
     */
    @Query("SELECT bd FROM BankDetails bd WHERE bd.businessProfile.id = :businessProfileId " +
           "AND bd.isPrimary = true " +
           "AND bd.bankVerificationStatus = 'VERIFIED'")
    Optional<BankDetails> findPrimaryVerifiedAccount(@Param("businessProfileId") Long businessProfileId);
}
