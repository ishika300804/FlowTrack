-- =====================================================
-- FlowTrack Business Onboarding Extension
-- Version: 1.1
-- Date: 2026-02-25
-- Description: Introduces structured business profile management
--              with multi-tenancy support and tenant isolation
-- =====================================================

-- =====================================================
-- Table: business_profiles
-- Relationship: Many-to-One with users
-- Description: Supports multiple business profiles per user
--              with strict tenant isolation
-- =====================================================
CREATE TABLE IF NOT EXISTS `business_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `legal_business_name` varchar(255) NOT NULL,
  `business_type` enum('PROPRIETORSHIP', 'LLP', 'PRIVATE_LIMITED', 'PARTNERSHIP') NOT NULL,
  `gstin` varchar(15) NOT NULL,
  `pan_number` varchar(500) NOT NULL COMMENT 'Encrypted storage',
  `cin_number` varchar(21) DEFAULT NULL,
  `udyam_number` varchar(19) DEFAULT NULL,
  `registered_address` text NOT NULL,
  `state` varchar(100) NOT NULL,
  `pincode` varchar(10) NOT NULL,
  `verification_status` enum('DRAFT', 'PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'DRAFT',
  `onboarding_stage` enum('TIER1_COMPLETE', 'TIER2_COMPLETE', 'ACTIVE') DEFAULT NULL,
  `rejection_reason` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_business_profiles_gstin` (`gstin`),
  KEY `IDX_business_profiles_user_id` (`user_id`),
  KEY `IDX_business_profiles_pan_number` (`pan_number`(255)),
  KEY `IDX_business_profiles_verification_status` (`verification_status`),
  KEY `IDX_business_profiles_gstin` (`gstin`),
  CONSTRAINT `FK_business_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci 
COMMENT='Business profiles with multi-tenant support - one user can manage multiple businesses';

-- =====================================================
-- Table: bank_details
-- Relationship: Many-to-One with business_profiles
-- Description: Supports multiple bank accounts per business
--              with primary account designation
-- =====================================================
CREATE TABLE IF NOT EXISTS `bank_details` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_profile_id` bigint NOT NULL,
  `account_holder_name` varchar(255) NOT NULL,
  `account_number` varchar(500) NOT NULL COMMENT 'Encrypted storage',
  `ifsc_code` varchar(11) NOT NULL,
  `bank_name` varchar(255) NOT NULL,
  `branch_name` varchar(255) DEFAULT NULL,
  `is_primary` boolean NOT NULL DEFAULT false,
  `bank_verification_status` enum('UNVERIFIED', 'VERIFIED', 'FAILED') NOT NULL DEFAULT 'UNVERIFIED',
  `verification_reference_id` varchar(100) DEFAULT NULL,
  `last_verified_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `IDX_bank_details_business_profile_id` (`business_profile_id`),
  KEY `IDX_bank_details_is_primary` (`business_profile_id`, `is_primary`),
  KEY `IDX_bank_details_verification_status` (`bank_verification_status`),
  CONSTRAINT `FK_bank_details_business_profile_id` FOREIGN KEY (`business_profile_id`) REFERENCES `business_profiles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `CHK_bank_details_primary_unique` UNIQUE (`business_profile_id`, `is_primary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='Bank accounts for business profiles - supports multiple accounts with one primary';

-- =====================================================
-- Trigger: Ensure Only One Primary Bank Account
-- =====================================================
DELIMITER $$

CREATE TRIGGER `before_bank_details_insert` BEFORE INSERT ON `bank_details`
FOR EACH ROW
BEGIN
  IF NEW.is_primary = true THEN
    -- If setting a new primary, unmark existing primary for this business
    UPDATE bank_details 
    SET is_primary = false 
    WHERE business_profile_id = NEW.business_profile_id 
      AND is_primary = true
      AND id != NEW.id;
  END IF;
END$$

CREATE TRIGGER `before_bank_details_update` BEFORE UPDATE ON `bank_details`
FOR EACH ROW
BEGIN
  IF NEW.is_primary = true AND OLD.is_primary = false THEN
    -- If setting a new primary, unmark existing primary for this business
    UPDATE bank_details 
    SET is_primary = false 
    WHERE business_profile_id = NEW.business_profile_id 
      AND is_primary = true
      AND id != NEW.id;
  END IF;
END$$

DELIMITER ;

-- =====================================================
-- Table: verification_logs
-- Relationship: Many-to-One with business_profiles
-- Description: Audit trail for all verification activities
-- =====================================================
CREATE TABLE IF NOT EXISTS `verification_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_profile_id` bigint NOT NULL,
  `verification_type` enum('GST', 'PAN', 'BANK', 'CIN', 'UDYAM') NOT NULL,
  `request_payload` json DEFAULT NULL COMMENT 'API request details',
  `response_payload` json DEFAULT NULL COMMENT 'API response details',
  `verification_result` varchar(50) NOT NULL,
  `verification_provider` varchar(100) DEFAULT NULL COMMENT 'e.g., Razorpay, Cashfree, GSTN API',
  `external_reference_id` varchar(100) DEFAULT NULL,
  `error_message` text DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `IDX_verification_logs_business_profile_id` (`business_profile_id`),
  KEY `IDX_verification_logs_verification_type` (`verification_type`),
  KEY `IDX_verification_logs_created_at` (`created_at`),
  KEY `IDX_verification_logs_result` (`verification_result`),
  CONSTRAINT `FK_verification_logs_business_profile_id` FOREIGN KEY (`business_profile_id`) REFERENCES `business_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='Complete audit trail for all business verification activities';

-- =====================================================
-- Indexes for Performance Optimization
-- =====================================================

-- Composite index for active business lookup per user
CREATE INDEX `IDX_business_profiles_user_active` 
ON `business_profiles` (`user_id`, `verification_status`);

-- Composite index for verification log queries
CREATE INDEX `IDX_verification_logs_composite` 
ON `verification_logs` (`business_profile_id`, `verification_type`, `created_at`);

-- =====================================================
-- Data Integrity Comments
-- =====================================================

-- IMPORTANT NOTES FOR APPLICATION LAYER:
-- 
-- 1. TENANT ISOLATION:
--    - All queries MUST be scoped by business_profile_id
--    - No cross-profile data access allowed
--    - Use ActiveBusinessProfileContext in session
--
-- 2. PRIMARY BANK ACCOUNT:
--    - Triggers ensure only one primary per business
--    - Application should prevent deletion of primary account
--    - Settlement operations MUST use primary verified account
--
-- 3. ENCRYPTION:
--    - pan_number and account_number are encrypted at rest
--    - Use JPA encryption converter in application layer
--    - Never log or expose these fields in plaintext
--
-- 4. VERIFICATION:
--    - All verification attempts logged to verification_logs
--    - Include IP address and user agent for audit trail
--    - Store complete request/response for debugging
--
-- 5. BACKWARD COMPATIBILITY:
--    - Existing tables (users, vendor_profiles, etc.) unchanged
--    - Foreign keys use ON DELETE CASCADE for data consistency
--    - No breaking changes to existing relationships
