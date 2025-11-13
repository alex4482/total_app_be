-- Add unit_price and total_cost columns to index_data table
-- unit_price: Local/override price per unit (optional - if null, uses counter's defaultUnitPrice)
-- total_cost: Total cost for this reading period (consumption * effectiveUnitPrice)

ALTER TABLE index_data 
ADD COLUMN IF NOT EXISTS unit_price DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS total_cost DOUBLE PRECISION;

COMMENT ON COLUMN index_data.unit_price IS 'Local/override price per unit (optional). If null, uses counter''s defaultUnitPrice or location price.';
COMMENT ON COLUMN index_data.total_cost IS 'Total cost for this reading period (consumption * effectiveUnitPrice).';

