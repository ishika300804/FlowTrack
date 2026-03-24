-- =====================================================
-- FlowTrack Verification Audit Enhancements
-- Version: 1.5
-- Date: 2026-02-25
-- Description: Add performance and HTTP tracking columns
--              to existing verification_logs table
-- =====================================================

-- Add HTTP status code for API response tracking
ALTER TABLE `verification_logs` 
ADD COLUMN `http_status_code` INT NULL COMMENT 'HTTP response status code' 
AFTER `error_message`;

-- Add execution time for performance monitoring  
ALTER TABLE `verification_logs`
ADD COLUMN `execution_time_ms` BIGINT NULL COMMENT 'API call duration in milliseconds'
AFTER `http_status_code`;
