-- Fix NULL values in *_order columns for element collections
-- These columns are part of PRIMARY KEY and must be NOT NULL

-- 1. Fix tenant_emails
WITH numbered_rows AS (
    SELECT 
        tenant_id,
        emails,
        emails_order,
        ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY emails_order NULLS LAST, emails) - 1 as new_order
    FROM tenant_emails
)
UPDATE tenant_emails te
SET emails_order = nr.new_order
FROM numbered_rows nr
WHERE te.tenant_id = nr.tenant_id 
  AND te.emails = nr.emails
  AND (te.emails_order IS NULL OR te.emails_order != nr.new_order);

ALTER TABLE tenant_emails ALTER COLUMN emails_order SET NOT NULL;

-- 2. Fix tenant_phone_numbers
WITH numbered_rows AS (
    SELECT 
        tenant_id,
        phone_numbers,
        phone_numbers_order,
        ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY phone_numbers_order NULLS LAST, phone_numbers) - 1 as new_order
    FROM tenant_phone_numbers
)
UPDATE tenant_phone_numbers tpn
SET phone_numbers_order = nr.new_order
FROM numbered_rows nr
WHERE tpn.tenant_id = nr.tenant_id 
  AND tpn.phone_numbers = nr.phone_numbers
  AND (tpn.phone_numbers_order IS NULL OR tpn.phone_numbers_order != nr.new_order);

ALTER TABLE tenant_phone_numbers ALTER COLUMN phone_numbers_order SET NOT NULL;

-- 3. Fix tenant_observations
WITH numbered_rows AS (
    SELECT 
        tenant_id,
        message,
        type,
        observations_order,
        ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY observations_order NULLS LAST, message) - 1 as new_order
    FROM tenant_observations
)
UPDATE tenant_observations tobs
SET observations_order = nr.new_order
FROM numbered_rows nr
WHERE tobs.tenant_id = nr.tenant_id 
  AND tobs.message = nr.message
  AND COALESCE(tobs.type, '') = COALESCE(nr.type, '')
  AND (tobs.observations_order IS NULL OR tobs.observations_order != nr.new_order);

ALTER TABLE tenant_observations ALTER COLUMN observations_order SET NOT NULL;

-- 4. Fix tenant_attachment_ids
WITH numbered_rows AS (
    SELECT 
        tenant_id,
        attachment_ids,
        attachment_ids_order,
        ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY attachment_ids_order NULLS LAST, attachment_ids) - 1 as new_order
    FROM tenant_attachment_ids
)
UPDATE tenant_attachment_ids tai
SET attachment_ids_order = nr.new_order
FROM numbered_rows nr
WHERE tai.tenant_id = nr.tenant_id 
  AND tai.attachment_ids = nr.attachment_ids
  AND (tai.attachment_ids_order IS NULL OR tai.attachment_ids_order != nr.new_order);

ALTER TABLE tenant_attachment_ids ALTER COLUMN attachment_ids_order SET NOT NULL;

-- 5. Fix tenant_rental_data_price_changes
WITH numbered_rows AS (
    SELECT 
        tenant_rental_data_id,
        new_price,
        change_time,
        price_changes_order,
        ROW_NUMBER() OVER (PARTITION BY tenant_rental_data_id ORDER BY price_changes_order NULLS LAST, change_time NULLS LAST) - 1 as new_order
    FROM tenant_rental_data_price_changes
)
UPDATE tenant_rental_data_price_changes trdpc
SET price_changes_order = nr.new_order
FROM numbered_rows nr
WHERE trdpc.tenant_rental_data_id = nr.tenant_rental_data_id 
  AND trdpc.new_price = nr.new_price
  AND COALESCE(trdpc.change_time, '1900-01-01'::date) = COALESCE(nr.change_time, '1900-01-01'::date)
  AND (trdpc.price_changes_order IS NULL OR trdpc.price_changes_order != nr.new_order);

ALTER TABLE tenant_rental_data_price_changes ALTER COLUMN price_changes_order SET NOT NULL;

-- 6. Fix tenant_rental_data_active_services
WITH numbered_rows AS (
    SELECT 
        tenant_rental_data_id,
        service_id,
        active_services_order,
        ROW_NUMBER() OVER (PARTITION BY tenant_rental_data_id ORDER BY active_services_order NULLS LAST, service_id) - 1 as new_order
    FROM tenant_rental_data_active_services
)
UPDATE tenant_rental_data_active_services trdas
SET active_services_order = nr.new_order
FROM numbered_rows nr
WHERE trdas.tenant_rental_data_id = nr.tenant_rental_data_id 
  AND trdas.service_id = nr.service_id
  AND (trdas.active_services_order IS NULL OR trdas.active_services_order != nr.new_order);

ALTER TABLE tenant_rental_data_active_services ALTER COLUMN active_services_order SET NOT NULL;

