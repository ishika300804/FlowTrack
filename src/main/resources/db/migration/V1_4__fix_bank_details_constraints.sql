-- ============================================================================
-- V1.4: Fix Bank Details Constraints and Triggers
-- ============================================================================
-- Issue 1: UNIQUE constraint on (business_profile_id, is_primary) prevents
--          multiple non-primary banks for same business
-- Issue 2: BEFORE trigger cannot UPDATE same table (MySQL limitation)
-- 
-- Solution: Drop bad constraint, create enforcement via CHECK-style triggers
-- ============================================================================

-- Step 1: Drop existing problematic unique constraint
-- This constraint was preventing multiple is_primary=FALSE rows
ALTER TABLE bank_details 
DROP INDEX CHK_bank_details_primary_unique;

-- Step 2: Drop existing triggers (they try to UPDATE same table in BEFORE trigger)
DROP TRIGGER IF EXISTS before_bank_details_insert;
DROP TRIGGER IF EXISTS before_bank_details_update;

-- Step 3: Create NEW triggers that SIGNAL error instead of UPDATE
-- This enforces "only one primary bank per business" without table mutation

DELIMITER $$

CREATE TRIGGER before_bank_details_insert_v2
BEFORE INSERT ON bank_details
FOR EACH ROW
BEGIN
  -- If inserting a PRIMARY bank, check if another primary already exists
  IF NEW.is_primary = TRUE THEN
    IF EXISTS (
      SELECT 1 
      FROM bank_details 
      WHERE business_profile_id = NEW.business_profile_id 
        AND is_primary = TRUE
    ) THEN
      -- Block the insert with error
      SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Business profile already has a primary bank account. Update the existing primary bank or set is_primary=FALSE.';
    END IF;
  END IF;
END$$

CREATE TRIGGER before_bank_details_update_v2
BEFORE UPDATE ON bank_details
FOR EACH ROW
BEGIN
  -- If updating to PRIMARY, check if another primary exists (excluding current row)
  IF NEW.is_primary = TRUE AND OLD.is_primary = FALSE THEN
    IF EXISTS (
      SELECT 1 
      FROM bank_details 
      WHERE business_profile_id = NEW.business_profile_id 
        AND is_primary = TRUE
        AND id != NEW.id
    ) THEN
      -- Block the update with error
      SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Business profile already has a primary bank account. Unset the existing primary first.';
    END IF;
  END IF;
END$$

DELIMITER ;

-- ============================================================================
-- VERIFICATION QUERIES (not executed, for documentation)
-- ============================================================================
-- After migration:
-- 1. Multiple non-primary banks allowed:
--    INSERT INTO bank_details (..., is_primary=FALSE) -- OK
--    INSERT INTO bank_details (..., is_primary=FALSE) -- OK
-- 
-- 2. Only ONE primary bank allowed:
--    UPDATE bank_details SET is_primary=TRUE WHERE id=X -- OK if no other primary
--    UPDATE bank_details SET is_primary=TRUE WHERE id=Y -- ERROR if X is primary
-- 
-- 3. Changing primary bank (2-step process):
--    UPDATE bank_details SET is_primary=FALSE WHERE id=X -- OK (unset old)
--    UPDATE bank_details SET is_primary=TRUE WHERE id=Y  -- OK (set new)
-- ============================================================================
