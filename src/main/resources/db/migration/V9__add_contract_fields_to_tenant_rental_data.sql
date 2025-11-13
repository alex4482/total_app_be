-- Add missing columns to tenant_rental_data table
-- These fields store optional contract information and rental space reference for rental agreements

-- Add contract information fields
ALTER TABLE tenant_rental_data 
ADD COLUMN IF NOT EXISTS contract_number VARCHAR(255),
ADD COLUMN IF NOT EXISTS contract_date DATE;

COMMENT ON COLUMN tenant_rental_data.contract_number IS 'Contract number (optional)';
COMMENT ON COLUMN tenant_rental_data.contract_date IS 'Contract date (optional)';

-- Note: rental_space relationship is OneToOne bidirectional
-- If Hibernate creates rental_space_id or rental_space_name column automatically,
-- it will be handled by ddl-auto=update in dev or already exists in baseline schema

