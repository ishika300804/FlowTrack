-- ============================================================================
-- FlowTrack Business Profile Extension - Phase 2: Multi-User Role Management
-- Migration: V1.2
-- Description: Add business_profile_roles table for multi-user business management
-- Author: FlowTrack Development Team
-- Date: 2026-02-25
-- Dependencies: V1.1 (business_profiles table must exist)
-- ============================================================================

-- Purpose: Enable multiple users to collaborate on a single business profile
-- with different permission levels (OWNER, ADMIN, FINANCE, OPERATIONS, AUDITOR)

-- ============================================================================
-- TABLE: business_profile_roles
-- ============================================================================
-- Manages user roles and permissions for business profiles
-- Supports scenarios:
-- 1. Business owner grants access to accountant (FINANCE role)
-- 2. Business owner grants access to operations manager (OPERATIONS role)
-- 3. Auditor granted read-only access (AUDITOR role)
-- 4. Multiple administrators (ADMIN role)
--
-- Permission hierarchy implemented in application layer:
-- OWNER > ADMIN > FINANCE > OPERATIONS > AUDITOR

CREATE TABLE IF NOT EXISTS business_profile_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Foreign key to business_profiles
    business_profile_id BIGINT NOT NULL,
    
    -- Foreign key to users (the user being granted access)
    user_id BIGINT NOT NULL,
    
    -- Role assigned to the user for this business
    -- ENUM values managed in Java: OWNER, ADMIN, FINANCE, OPERATIONS, AUDITOR
    role VARCHAR(50) NOT NULL,
    
    -- Audit fields
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT NULL, -- user_id of who granted this role (NULL for initial owner)
    revoked_at TIMESTAMP NULL,
    revoked_by BIGINT NULL,
    
    -- Metadata
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT NULL, -- Optional notes about the role grant
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT FK_business_profile_roles_business_profile_id 
        FOREIGN KEY (business_profile_id) 
        REFERENCES business_profiles(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT FK_business_profile_roles_user_id 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT FK_business_profile_roles_granted_by 
        FOREIGN KEY (granted_by) 
        REFERENCES users(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT FK_business_profile_roles_revoked_by 
        FOREIGN KEY (revoked_by) 
        REFERENCES users(id) 
        ON DELETE SET NULL,
    
    -- Unique constraint: One user can have only one role per business
    -- If role needs to change, update existing record
    CONSTRAINT UK_business_profile_roles_user_business 
        UNIQUE (user_id, business_profile_id),
    
    -- Check constraint: role must be valid
    CONSTRAINT CHK_business_profile_roles_role 
        CHECK (role IN ('OWNER', 'ADMIN', 'FINANCE', 'OPERATIONS', 'AUDITOR'))
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- INDEXES for performance optimization
-- ============================================================================

-- Index on business_profile_id for listing all users of a business
CREATE INDEX IDX_business_profile_roles_business_profile_id 
    ON business_profile_roles(business_profile_id);

-- Index on user_id for listing all businesses accessible to a user
CREATE INDEX IDX_business_profile_roles_user_id 
    ON business_profile_roles(user_id);

-- Composite index for common query: Find user's role for specific business
-- Already covered by unique constraint UK_business_profile_roles_user_business

-- Index on role for filtering by permission level
CREATE INDEX IDX_business_profile_roles_role 
    ON business_profile_roles(role);

-- Index on is_active for filtering active roles only
CREATE INDEX IDX_business_profile_roles_is_active 
    ON business_profile_roles(is_active);

-- Composite index for finding active roles for a business
CREATE INDEX IDX_business_profile_roles_business_active 
    ON business_profile_roles(business_profile_id, is_active);

-- ============================================================================
-- TRIGGER: Ensure only one OWNER per business (Business Rule Enforcement)
-- ============================================================================

DELIMITER $$

CREATE TRIGGER before_business_profile_roles_insert
BEFORE INSERT ON business_profile_roles
FOR EACH ROW
BEGIN
    -- If inserting an OWNER role, check if business already has an owner
    IF NEW.role = 'OWNER' AND NEW.is_active = TRUE THEN
        IF EXISTS (
            SELECT 1 FROM business_profile_roles 
            WHERE business_profile_id = NEW.business_profile_id 
              AND role = 'OWNER' 
              AND is_active = TRUE
        ) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Business profile already has an active OWNER. Transfer ownership first.';
        END IF;
    END IF;
END$$

CREATE TRIGGER before_business_profile_roles_update
BEFORE UPDATE ON business_profile_roles
FOR EACH ROW
BEGIN
    -- If changing to OWNER role, check if business already has an owner
    IF NEW.role = 'OWNER' AND NEW.is_active = TRUE AND OLD.role != 'OWNER' THEN
        IF EXISTS (
            SELECT 1 FROM business_profile_roles 
            WHERE business_profile_id = NEW.business_profile_id 
              AND role = 'OWNER' 
              AND is_active = TRUE
              AND id != NEW.id  -- Exclude current record
        ) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Business profile already has an active OWNER. Transfer ownership first.';
        END IF;
    END IF;
END$$

DELIMITER ;

-- ============================================================================
-- MIGRATION: Auto-create OWNER roles for existing business_profiles
-- ============================================================================
-- For each existing business profile, grant OWNER role to the user who created it

INSERT INTO business_profile_roles (
    business_profile_id,
    user_id,
    role,
    granted_at,
    granted_by,
    is_active,
    notes
)
SELECT 
    bp.id AS business_profile_id,
    bp.user_id AS user_id,
    'OWNER' AS role,
    bp.created_at AS granted_at,
    NULL AS granted_by,  -- NULL because it's auto-created, not manually granted
    TRUE AS is_active,
    'Auto-created during migration V1.2' AS notes
FROM business_profiles bp
WHERE NOT EXISTS (
    SELECT 1 FROM business_profile_roles bpr 
    WHERE bpr.business_profile_id = bp.id 
      AND bpr.user_id = bp.user_id 
      AND bpr.role = 'OWNER'
);

-- ============================================================================
-- VERIFICATION QUERIES (Run after migration to validate)
-- ============================================================================

-- 1. Verify all business profiles have exactly one active OWNER
-- SELECT 
--     bp.id,
--     bp.legal_business_name,
--     COUNT(bpr.id) as owner_count
-- FROM business_profiles bp
-- LEFT JOIN business_profile_roles bpr 
--     ON bp.id = bpr.business_profile_id 
--     AND bpr.role = 'OWNER' 
--     AND bpr.is_active = TRUE
-- GROUP BY bp.id, bp.legal_business_name
-- HAVING owner_count != 1;
-- Expected: 0 rows (all businesses have exactly one owner)

-- 2. Verify business_profile.user_id matches OWNER role
-- SELECT 
--     bp.id,
--     bp.user_id AS profile_user_id,
--     bpr.user_id AS owner_user_id
-- FROM business_profiles bp
-- JOIN business_profile_roles bpr 
--     ON bp.id = bpr.business_profile_id 
--     AND bpr.role = 'OWNER' 
--     AND bpr.is_active = TRUE
-- WHERE bp.user_id != bpr.user_id;
-- Expected: 0 rows (consistency maintained)

-- ============================================================================
-- ROLLBACK SCRIPT (Use only in emergency)
-- ============================================================================

-- To rollback this migration:
-- DROP TRIGGER IF EXISTS before_business_profile_roles_insert;
-- DROP TRIGGER IF EXISTS before_business_profile_roles_update;
-- DROP TABLE IF EXISTS business_profile_roles;
-- DELETE FROM flyway_schema_history WHERE version = '1.2';

-- ============================================================================
-- END OF MIGRATION V1.2
-- ============================================================================
