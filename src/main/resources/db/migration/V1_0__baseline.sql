-- Flyway Baseline Migration for FlowTrack IMS
-- This migration captures the existing database schema
-- All existing tables and relationships are preserved

-- =====================================================
-- Table: borrower
-- =====================================================
CREATE TABLE IF NOT EXISTS `borrower` (
  `borrower_id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`borrower_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: dashboard_snapshot
-- =====================================================
CREATE TABLE IF NOT EXISTS `dashboard_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(255) DEFAULT NULL,
  `inventory_remaining` int DEFAULT NULL,
  `items_borrowed` bigint DEFAULT NULL,
  `items_issued` bigint DEFAULT NULL,
  `items_returned` bigint DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: item_type
-- =====================================================
CREATE TABLE IF NOT EXISTS `item_type` (
  `item_type_id` bigint NOT NULL AUTO_INCREMENT,
  `type_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`item_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: manager_type
-- =====================================================
CREATE TABLE IF NOT EXISTS `manager_type` (
  `manager_type_id` bigint NOT NULL AUTO_INCREMENT,
  `manager_type_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`manager_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: vendor
-- =====================================================
CREATE TABLE IF NOT EXISTS `vendor` (
  `vendor_id` bigint NOT NULL AUTO_INCREMENT,
  `vendor_email` varchar(255) DEFAULT NULL,
  `vendor_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: inventory_item
-- =====================================================
CREATE TABLE IF NOT EXISTS `inventory_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `item_fine_rate` double DEFAULT NULL,
  `item_invoice_number` bigint DEFAULT NULL,
  `item_name` varchar(255) DEFAULT NULL,
  `item_price` double DEFAULT NULL,
  `item_quantity` int DEFAULT NULL,
  `item_type_fk` bigint DEFAULT NULL,
  `vendor_id_fk` bigint DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FK47t81gbc6n6tphe58irl5pk94` (`item_type_fk`),
  KEY `FKoytr55nkarccuq72ly2pv65m` (`vendor_id_fk`),
  CONSTRAINT `FK47t81gbc6n6tphe58irl5pk94` FOREIGN KEY (`item_type_fk`) REFERENCES `item_type` (`item_type_id`),
  CONSTRAINT `FKoytr55nkarccuq72ly2pv65m` FOREIGN KEY (`vendor_id_fk`) REFERENCES `vendor` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: inventory_manager
-- =====================================================
CREATE TABLE IF NOT EXISTS `inventory_manager` (
  `manager_id` bigint NOT NULL AUTO_INCREMENT,
  `manager_email` varchar(255) DEFAULT NULL,
  `manager_name` varchar(255) DEFAULT NULL,
  `manager_password` varchar(255) DEFAULT NULL,
  `manager_type_fk` bigint DEFAULT NULL,
  PRIMARY KEY (`manager_id`),
  KEY `FKr2vtghktcaa08ka18s8u2f0ru` (`manager_type_fk`),
  CONSTRAINT `FKr2vtghktcaa08ka18s8u2f0ru` FOREIGN KEY (`manager_type_fk`) REFERENCES `manager_type` (`manager_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: item_repair
-- =====================================================
CREATE TABLE IF NOT EXISTS `item_repair` (
  `repair_id` bigint NOT NULL AUTO_INCREMENT,
  `repair_cost` double DEFAULT NULL,
  `item_price` double DEFAULT NULL,
  `item_id_fk` bigint DEFAULT NULL,
  `vendor_id_fk` bigint DEFAULT NULL,
  PRIMARY KEY (`repair_id`),
  KEY `FKsjs6i0tbofw67bqcy30rgv5vd` (`item_id_fk`),
  KEY `FK7oqnwauxs5oxw724gj1h78f5s` (`vendor_id_fk`),
  CONSTRAINT `FK7oqnwauxs5oxw724gj1h78f5s` FOREIGN KEY (`vendor_id_fk`) REFERENCES `vendor` (`vendor_id`),
  CONSTRAINT `FKsjs6i0tbofw67bqcy30rgv5vd` FOREIGN KEY (`item_id_fk`) REFERENCES `inventory_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: loan
-- =====================================================
CREATE TABLE IF NOT EXISTS `loan` (
  `loan_id` bigint NOT NULL AUTO_INCREMENT,
  `issue_date` varchar(255) DEFAULT NULL,
  `loan_duration` bigint DEFAULT NULL,
  `return_date` varchar(255) DEFAULT NULL,
  `total_fine` double DEFAULT NULL,
  `borrower_borrower_id` bigint DEFAULT NULL,
  `item_item_id` bigint DEFAULT NULL,
  PRIMARY KEY (`loan_id`),
  KEY `FKoo1qyebdt8c6jg179x16wdwvp` (`borrower_borrower_id`),
  KEY `FKa9pb5iwpdpul7qbnrk0n0qbvp` (`item_item_id`),
  CONSTRAINT `FKa9pb5iwpdpul7qbnrk0n0qbvp` FOREIGN KEY (`item_item_id`) REFERENCES `inventory_item` (`item_id`),
  CONSTRAINT `FKoo1qyebdt8c6jg179x16wdwvp` FOREIGN KEY (`borrower_borrower_id`) REFERENCES `borrower` (`borrower_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: roles
-- =====================================================
CREATE TABLE IF NOT EXISTS `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: users
-- =====================================================
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: user_roles (Many-to-Many Join Table)
-- =====================================================
CREATE TABLE IF NOT EXISTS `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: investor_profiles (One-to-One with users)
-- =====================================================
CREATE TABLE IF NOT EXISTS `investor_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aadhar_card_url` varchar(255) DEFAULT NULL,
  `aadhar_number` varchar(255) DEFAULT NULL,
  `address` text,
  `bank_account_number` varchar(255) DEFAULT NULL,
  `bank_ifsc_code` varchar(255) DEFAULT NULL,
  `bank_name` varchar(255) DEFAULT NULL,
  `bank_statement_url` varchar(255) DEFAULT NULL,
  `company_registration_number` varchar(255) DEFAULT NULL,
  `expected_roi_percentage` decimal(19,2) DEFAULT NULL,
  `investment_capacity` decimal(19,2) DEFAULT NULL,
  `investment_experience_years` int DEFAULT NULL,
  `investment_portfolio_url` varchar(255) DEFAULT NULL,
  `investor_name` varchar(255) NOT NULL,
  `investor_type` varchar(255) NOT NULL,
  `maximum_investment_amount` decimal(19,2) DEFAULT NULL,
  `minimum_investment_amount` decimal(19,2) DEFAULT NULL,
  `pan_card_url` varchar(255) DEFAULT NULL,
  `pan_number` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `preferred_sectors` text,
  `rejection_reason` text,
  `risk_appetite` varchar(255) DEFAULT NULL,
  `verification_status` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_soqxl69ixbqvymc4ig76oawej` (`user_id`),
  CONSTRAINT `FKbpfclc3evlln2gajncfevq8k7` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: retailer_profiles (One-to-One with users)
-- =====================================================
CREATE TABLE IF NOT EXISTS `retailer_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_address` text,
  `business_description` text,
  `business_license_url` varchar(255) DEFAULT NULL,
  `business_name` varchar(255) NOT NULL,
  `business_registration_number` varchar(255) DEFAULT NULL,
  `business_type` varchar(255) NOT NULL,
  `gst_number` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `proof_of_identity_url` varchar(255) DEFAULT NULL,
  `rejection_reason` text,
  `trademark` varchar(255) DEFAULT NULL,
  `verification_status` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_m8ur7tljd555c3qhqgujmfail` (`user_id`),
  CONSTRAINT `FK921r57gbqm9h35x2ikotmapqe` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- Table: vendor_profiles (One-to-One with users)
-- =====================================================
CREATE TABLE IF NOT EXISTS `vendor_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bank_account_number` varchar(255) DEFAULT NULL,
  `bank_ifsc_code` varchar(255) DEFAULT NULL,
  `bank_name` varchar(255) DEFAULT NULL,
  `business_type` varchar(255) NOT NULL,
  `company_address` text,
  `company_description` text,
  `company_name` varchar(255) NOT NULL,
  `company_registration_url` varchar(255) DEFAULT NULL,
  `gst_certificate_url` varchar(255) DEFAULT NULL,
  `gst_number` varchar(255) DEFAULT NULL,
  `pan_number` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `product_categories` text,
  `rejection_reason` text,
  `trade_license_number` varchar(255) DEFAULT NULL,
  `trade_license_url` varchar(255) DEFAULT NULL,
  `verification_status` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_54wrcif7ltdw3bocsbe3twow9` (`user_id`),
  CONSTRAINT `FKbdoc22aas6cny51wfhae92xit` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
