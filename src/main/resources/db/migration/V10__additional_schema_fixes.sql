-- Additional schema fixes
-- This migration handles any remaining schema validation issues

-- Add currency column to tenant_rental_data table
ALTER TABLE tenant_rental_data 
ADD COLUMN IF NOT EXISTS currency VARCHAR(50) DEFAULT 'RON';

COMMENT ON COLUMN tenant_rental_data.currency IS 'Currency for rental agreement (default: RON)';

