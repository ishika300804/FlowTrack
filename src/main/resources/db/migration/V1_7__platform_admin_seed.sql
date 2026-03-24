-- ============================================================================
-- FlowTrack - Platform Admin Role & Seed User
-- Migration: V1.7
-- Description: Creates ROLE_PLATFORM_ADMIN and seeds a default platform admin user
-- Default credentials: username=platformadmin / password=Admin@2024
-- !! CHANGE THE PASSWORD IMMEDIATELY IN PRODUCTION !!
-- Dependencies: V1.0 (roles, users, user_roles tables must exist)
-- ============================================================================

-- ============================================================================
-- STEP 1: Insert ROLE_PLATFORM_ADMIN (skip if already exists)
-- ============================================================================
INSERT IGNORE INTO `roles` (`name`) VALUES ('ROLE_PLATFORM_ADMIN');

-- ============================================================================
-- STEP 2: Insert default platform admin user
-- BCrypt hash of "Admin@2024" (cost=10)
-- !! CHANGE THIS IN PRODUCTION — run: new BCryptPasswordEncoder().encode("your-password") !!
-- ============================================================================
INSERT IGNORE INTO `users` (`username`, `email`, `password`, `first_name`, `last_name`, `enabled`)
VALUES (
    'platformadmin',
    'platform.admin@flowtrack.in',
    '$2b$10$HBG28gqzHAe4YHHtVJFs.usCYyDsKp.Cdx.nmN3cA.YzQpgSdBTjq',
    'Platform',
    'Admin',
    1
);

-- ============================================================================
-- STEP 3: Assign ROLE_PLATFORM_ADMIN to the platform admin user
-- ============================================================================
INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `users` u
CROSS JOIN `roles` r
WHERE u.username = 'platformadmin'
  AND r.name     = 'ROLE_PLATFORM_ADMIN';
