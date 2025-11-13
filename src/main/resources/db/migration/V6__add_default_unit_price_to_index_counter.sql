-- Add default_unit_price column to index_counter table
-- This column stores the default/global unit price for a counter
-- If an IndexData doesn't have a local unitPrice, it will use this default

ALTER TABLE index_counter 
ADD COLUMN default_unit_price DOUBLE PRECISION;

COMMENT ON COLUMN index_counter.default_unit_price IS 'Default/global unit price for this counter. Used when IndexData does not have a local unitPrice.';

