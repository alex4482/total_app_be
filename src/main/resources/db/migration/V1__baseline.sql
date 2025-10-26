-- Baseline migration
-- This migration marks the baseline state of the database.
-- All existing tables were created by Hibernate's ddl-auto=update.
--
-- Tables managed by this application:
-- - building
-- - location
-- - room
-- - rental_space
-- - tenant
-- - tenant_rental_data
-- - file_asset
-- - temp_upload
-- - email_preset
-- - index_counter
-- - index_data
-- - replaced_counter_index_data
-- - refresh_token_state
--
-- Future schema changes should be made through versioned migrations (V2, V3, etc.)
-- DO NOT modify entities directly in production - always create a new migration first.

-- This migration is intentionally empty as the schema already exists
SELECT 1;

