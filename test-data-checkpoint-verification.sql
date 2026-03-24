-- ============================================================================
-- CHECKPOINT VERIFICATION TEST DATA
-- ============================================================================
-- Purpose: Create test data to verify all 6 security checkpoints
-- DO NOT run in production - this is for verification only
-- ============================================================================

-- Step 1: Create Test Users
INSERT INTO users (id, username, email, password, enabled, first_name, last_name) VALUES
(101, 'owner1', 'owner1@test.com', '$2a$10$dummyHashForTesting12345678901234567890123456', TRUE, 'Owner', 'One'),
(102, 'owner2', 'owner2@test.com', '$2a$10$dummyHashForTesting12345678901234567890123456', TRUE, 'Owner', 'Two'),
(103, 'admin1', 'admin1@test.com', '$2a$10$dummyHashForTesting12345678901234567890123456', TRUE, 'Admin', 'One'),
(104, 'finance1', 'finance1@test.com', '$2a$10$dummyHashForTesting12345678901234567890123456', TRUE, 'Finance', 'One'),
(105, 'operations1', 'operations1@test.com', '$2a$10$dummyHashForTesting12345678901234567890123456', TRUE, 'Operations', 'One');

-- Step 2: Create Test Business Profiles (various verification states)
INSERT INTO business_profiles (id, user_id, legal_business_name, business_type, gstin, pan_number, registered_address, state, pincode, verification_status, onboarding_stage, created_at, updated_at) VALUES
-- Business 1: DRAFT status (should block transactions)
(201, 101, 'Test Business DRAFT', 'PRIVATE_LIMITED', '29ABCDE1234F1Z5', 'ABCDE1234F', '123 Test Street', 'Karnataka', '560001', 'DRAFT', NULL, NOW(), NOW()),

-- Business 2: VERIFIED + ACTIVE (fully operational)
(202, 101, 'Test Business VERIFIED', 'PARTNERSHIP', '27XYZAB5678G1Z5', 'XYZAB5678G', '456 Main Road', 'Maharashtra', '400001', 'VERIFIED', 'ACTIVE', NOW(), NOW()),

-- Business 3: VERIFIED but no ACTIVE stage (should block inventory)
(203, 102, 'Test Business Pending Onboarding', 'SOLE_PROPRIETORSHIP', '19LMNOP9012H1Z5', 'LMNOP9012H', '789 Park Avenue', 'Delhi', '110001', 'VERIFIED', 'IDENTITY_SUBMITTED', NOW(), NOW()),

-- Business 4: VERIFIED + ACTIVE but no bank account (should block settlement)
(204, 102, 'Test Business No Bank', 'PRIVATE_LIMITED', '24QRSTU3456I1Z5', 'QRSTU3456I', '321 Commerce Street', 'Tamil Nadu', '600001', 'VERIFIED', 'ACTIVE', NOW(), NOW()),

-- Business 5: Fully operational with primary verified bank
(205, 101, 'Test Business Complete', 'PRIVATE_LIMITED', '33VWXYZ7890J1Z5', 'VWXYZ7890J', '654 Business Park', 'Karnataka', '560002', 'VERIFIED', 'ACTIVE', NOW(), NOW());

-- Step 3: Create Business Profile Roles
INSERT INTO business_profile_roles (business_profile_id, user_id, role, granted_at, is_active) VALUES
-- Business 201: User 101 is OWNER
(201, 101, 'OWNER', NOW(), TRUE),

-- Business 202: User 101 is OWNER
(202, 101, 'OWNER', NOW(), TRUE),

-- Business 203: User 102 is OWNER
(203, 102, 'OWNER', NOW(), TRUE),

-- Business 204: User 102 is OWNER, User 103 is ADMIN, User 104 is FINANCE, User 105 is OPERATIONS
(204, 102, 'OWNER', NOW(), TRUE),
(204, 103, 'ADMIN', NOW(), TRUE),
(204, 104, 'FINANCE', NOW(), TRUE),
(204, 105, 'OPERATIONS', NOW(), TRUE),

-- Business 205: User 101 is OWNER
(205, 101, 'OWNER', NOW(), TRUE);

-- Step 4: Create Bank Accounts (various verification states)
INSERT INTO bank_details (business_profile_id, account_number, ifsc_code, bank_name, branch_name, account_holder_name, bank_verification_status, is_primary, verified_at, created_at, updated_at) VALUES
-- Business 202: Has UNVERIFIED bank (not primary)
(202, 'U2tGcVl3PT0=', 'HDFC0001234', 'HDFC Bank', 'Mumbai Branch', 'Test Business VERIFIED', 'UNVERIFIED', FALSE, NULL, NOW(), NOW()),

-- Business 203: Has VERIFIED bank but not primary (should still block settlement)
(203, 'VTJ0R2NWbz0=', 'ICIC0005678', 'ICICI Bank', 'Delhi Branch', 'Test Business Pending', 'VERIFIED', FALSE, NOW(), NOW(), NOW()),

-- Business 205: Has VERIFIED PRIMARY bank (fully operational)
(205, 'VlcweFkzQT0=', 'SBIN0009012', 'State Bank of India', 'Bangalore Branch', 'Test Business Complete', 'VERIFIED', TRUE, NOW(), NOW(), NOW()),

-- Business 205: Additional non-primary verified bank
(205, 'VlZwR2RGbz0=', 'AXIS0003456', 'Axis Bank', 'Chennai Branch', 'Test Business Complete', 'VERIFIED', FALSE, NOW(), NOW(), NOW());

-- ============================================================================
-- VERIFICATION QUERY SUMMARY
-- ============================================================================
SELECT 
    bp.id AS business_id,
    bp.legal_business_name,
    bp.verification_status,
    bp.onboarding_stage,
    u.email AS owner_email,
    COUNT(bd.id) AS bank_accounts,
    SUM(CASE WHEN bd.bank_verification_status='VERIFIED' AND bd.is_primary=TRUE THEN 1 ELSE 0 END) AS verified_primary_banks
FROM business_profiles bp
JOIN users u ON bp.user_id = u.id
LEFT JOIN bank_details bd ON bp.id = bd.business_profile_id
WHERE bp.id >= 201
GROUP BY bp.id, bp.legal_business_name, bp.verification_status, bp.onboarding_stage, u.email;
