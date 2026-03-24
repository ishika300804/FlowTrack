-- MySQL Setup Script for Inventory Management System
-- Run this script in MySQL to create the database

-- Create database
CREATE DATABASE IF NOT EXISTS ims 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Use the database
USE ims;

-- Show confirmation
SELECT 'Database ims created successfully!' AS Status;

-- Optional: Create dedicated user (recommended for production)
-- Uncomment the lines below if you want a dedicated user

-- CREATE USER IF NOT EXISTS 'ims_user'@'localhost' IDENTIFIED BY 'ims_password_123';
-- GRANT ALL PRIVILEGES ON ims.* TO 'ims_user'@'localhost';
-- FLUSH PRIVILEGES;
-- SELECT 'User ims_user created with full privileges on ims' AS Status;

-- Note: Tables will be created automatically by Spring Boot when you run the application
-- The application will also insert sample data on first run
