-- Revert V2: Make *_order columns nullable again
-- This reverses the NOT NULL constraints added in V2

-- 1. Revert tenant_emails
ALTER TABLE tenant_emails ALTER COLUMN emails_order DROP NOT NULL;

-- 2. Revert tenant_phone_numbers
ALTER TABLE tenant_phone_numbers ALTER COLUMN phone_numbers_order DROP NOT NULL;

-- 3. Revert tenant_observations
ALTER TABLE tenant_observations ALTER COLUMN observations_order DROP NOT NULL;

-- 4. Revert tenant_attachment_ids
ALTER TABLE tenant_attachment_ids ALTER COLUMN attachment_ids_order DROP NOT NULL;

-- 5. Revert tenant_rental_data_price_changes
ALTER TABLE tenant_rental_data_price_changes ALTER COLUMN price_changes_order DROP NOT NULL;

-- 6. Revert tenant_rental_data_active_services
ALTER TABLE tenant_rental_data_active_services ALTER COLUMN active_services_order DROP NOT NULL;

