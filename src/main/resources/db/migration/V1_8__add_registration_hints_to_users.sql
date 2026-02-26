-- V1_8: Add registration hint columns to users table
-- These store business data collected during registration for pre-populating the business profile form.
-- They are "hints" — the real validated data lives in business_profiles.

ALTER TABLE users
    ADD COLUMN registration_phone       VARCHAR(20)  NULL,
    ADD COLUMN registration_business_name VARCHAR(255) NULL,
    ADD COLUMN registration_business_type VARCHAR(60)  NULL,
    ADD COLUMN registration_gst_hint    VARCHAR(20)  NULL,
    ADD COLUMN registration_address     VARCHAR(500) NULL;
