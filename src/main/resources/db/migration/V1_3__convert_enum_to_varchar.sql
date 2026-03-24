-- =====================================================
-- FlowTrack Schema Fix: Convert ENUM to VARCHAR
-- Version: 1.3
-- Date: 2026-02-25
-- Description: Convert ENUM columns to VARCHAR for JPA compatibility
--              JPA @Enumerated(EnumType.STRING) expects VARCHAR, not ENUM
-- =====================================================

-- =====================================================
-- ALTER business_profiles table
-- Convert enum columns to varchar for JPA compatibility
-- =====================================================
ALTER TABLE `business_profiles` 
MODIFY COLUMN `business_type` VARCHAR(20) NOT NULL;

ALTER TABLE `business_profiles` 
MODIFY COLUMN `verification_status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE `business_profiles` 
MODIFY COLUMN `onboarding_stage` VARCHAR(20) DEFAULT NULL;

-- =====================================================
-- ALTER bank_details table
-- Convert enum column to varchar
-- =====================================================
ALTER TABLE `bank_details` 
MODIFY COLUMN `bank_verification_status` VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED';

-- =====================================================
-- ALTER verification_logs table
-- Convert enum column to varchar
-- =====================================================
ALTER TABLE `verification_logs` 
MODIFY COLUMN `verification_type` VARCHAR(20) NOT NULL;

-- =====================================================
-- Verification: Check column types after migration
-- =====================================================
-- Run these queries manually to verify:
-- SHOW COLUMNS FROM business_profiles WHERE Field IN ('business_type', 'verification_status', 'onboarding_stage');
-- SHOW COLUMNS FROM bank_details WHERE Field = 'bank_verification_status';
-- SHOW COLUMNS FROM verification_logs WHERE Field = 'verification_type';

-- =====================================================
-- ROLLBACK Script (if needed)
-- =====================================================
-- ALTER TABLE `business_profiles` MODIFY COLUMN `business_type` enum('PROPRIETORSHIP', 'LLP', 'PRIVATE_LIMITED', 'PARTNERSHIP') NOT NULL;
-- ALTER TABLE `business_profiles` MODIFY COLUMN `verification_status` enum('DRAFT', 'PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'DRAFT';
-- ALTER TABLE `business_profiles` MODIFY COLUMN `onboarding_stage` enum('TIER1_COMPLETE', 'TIER2_COMPLETE', 'ACTIVE') DEFAULT NULL;
-- ALTER TABLE `bank_details` MODIFY COLUMN `bank_verification_status` enum('UNVERIFIED', 'VERIFIED', 'FAILED') NOT NULL DEFAULT 'UNVERIFIED';
-- ALTER TABLE `verification_logs` MODIFY COLUMN `verification_type` enum('GST', 'PAN', 'BANK', 'CIN', 'UDYAM') NOT NULL;
