package com.example.IMS.repository;

import com.example.IMS.model.VerificationLog;
import com.example.IMS.model.enums.VerificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for VerificationLog entity
 * 
 * Used for audit trails and compliance reporting
 * All queries scoped by business_profile_id
 */
@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
    
    /**
     * Find all verification logs for a business profile
     */
    List<VerificationLog> findByBusinessProfileId(Long businessProfileId);
    
    /**
     * Find verification logs by business and type
     */
    List<VerificationLog> findByBusinessProfileIdAndVerificationType(
        Long businessProfileId, 
        VerificationType verificationType
    );
    
    /**
     * Find verification logs with pagination (for admin dashboard)
     */
    Page<VerificationLog> findByBusinessProfileId(Long businessProfileId, Pageable pageable);
    
    /**
     * Find recent verification logs (last N days)
     */
    @Query("SELECT vl FROM VerificationLog vl WHERE vl.businessProfile.id = :businessProfileId " +
           "AND vl.createdAt >= :startDate ORDER BY vl.createdAt DESC")
    List<VerificationLog> findRecentLogs(
        @Param("businessProfileId") Long businessProfileId,
        @Param("startDate") LocalDateTime startDate
    );
    
    /**
     * Find failed verification attempts (for retry logic)
     */
    @Query("SELECT vl FROM VerificationLog vl WHERE vl.businessProfile.id = :businessProfileId " +
           "AND vl.verificationType = :type " +
           "AND vl.verificationResult IN ('FAILED', 'ERROR') " +
           "ORDER BY vl.createdAt DESC")
    List<VerificationLog> findFailedAttempts(
        @Param("businessProfileId") Long businessProfileId,
        @Param("type") VerificationType type
    );
    
    /**
     * Count verification attempts by type and result
     */
    @Query("SELECT COUNT(vl) FROM VerificationLog vl WHERE vl.businessProfile.id = :businessProfileId " +
           "AND vl.verificationType = :type " +
           "AND vl.verificationResult = :result")
    long countByTypeAndResult(
        @Param("businessProfileId") Long businessProfileId,
        @Param("type") VerificationType type,
        @Param("result") String result
    );
    
    /**
     * Find the last successful verification for a type
     */
    @Query("SELECT vl FROM VerificationLog vl WHERE vl.businessProfile.id = :businessProfileId " +
           "AND vl.verificationType = :type " +
           "AND vl.verificationResult = 'SUCCESS' " +
           "ORDER BY vl.createdAt DESC")
    List<VerificationLog> findLastSuccessfulVerification(
        @Param("businessProfileId") Long businessProfileId,
        @Param("type") VerificationType type,
        Pageable pageable
    );
}
