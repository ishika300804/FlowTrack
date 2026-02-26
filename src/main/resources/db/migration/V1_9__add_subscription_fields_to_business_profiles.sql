-- V1_9: Add subscription tracking columns to business_profiles
-- These columns were added to the BusinessProfile entity to persist plan/expiry after Razorpay payment

ALTER TABLE business_profiles
    ADD COLUMN subscription_plan         VARCHAR(20)  NULL,
    ADD COLUMN subscription_start_date   DATE         NULL,
    ADD COLUMN subscription_expiry_date  DATE         NULL,
    ADD COLUMN last_payment_id           VARCHAR(100) NULL;
