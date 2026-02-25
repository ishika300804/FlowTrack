-- ========================================================================
-- ROLLBACK SCRIPT: Business Profile Extension
-- ========================================================================
-- Purpose: Safely remove business onboarding tables if rollback needed
-- WARNING: This will delete all business profile data
-- Backup data before running this script!
-- ========================================================================

-- Step 1: Drop foreign key constraints first
-- (Order matters - must drop dependent tables before parent tables)

-- Drop verification_logs foreign key
ALTER TABLE verification_logs 
DROP FOREIGN KEY IF EXISTS fk_verification_business_profile;

-- Drop bank_details foreign key
ALTER TABLE bank_details 
DROP FOREIGN KEY IF EXISTS fk_bank_business_profile;

-- Drop business_profile_roles foreign key
ALTER TABLE business_profile_roles 
DROP FOREIGN KEY IF EXISTS fk_role_business_profile;

ALTER TABLE business_profile_roles 
DROP FOREIGN KEY IF EXISTS fk_role_user;

-- Step 2: Drop tables in reverse dependency order

DROP TABLE IF EXISTS verification_logs;
DROP TABLE IF EXISTS bank_details;
DROP TABLE IF EXISTS business_profile_roles;
DROP TABLE IF EXISTS business_profiles;

-- Step 3: Verify existing tables are untouched
-- (These queries should return expected row counts)

SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'roles' AS table_name, COUNT(*) AS row_count FROM roles
UNION ALL
SELECT 'inventory_item' AS table_name, COUNT(*) AS row_count FROM inventory_item
UNION ALL
SELECT 'vendor' AS table_name, COUNT(*) AS row_count FROM vendor
UNION ALL
SELECT 'loan' AS table_name, COUNT(*) AS row_count FROM loan
UNION ALL
SELECT 'borrower' AS table_name, COUNT(*) AS row_count FROM borrower;

-- Step 4: Verify business profile tables are gone

SELECT 
    TABLE_NAME 
FROM 
    INFORMATION_SCHEMA.TABLES 
WHERE 
    TABLE_SCHEMA = 'ims' 
    AND TABLE_NAME IN ('business_profiles', 'bank_details', 'verification_logs', 'business_profile_roles');

-- Expected result: Empty set (0 rows)

-- ========================================================================
-- ROLLBACK COMPLETE
-- ========================================================================
-- Next steps:
-- 1. Verify existing data intact (check row counts above)
-- 2. Restart application with feature.onboarding.enabled=false
-- 3. Redeploy previous application version if needed
-- ========================================================================
