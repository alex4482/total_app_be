-- Remove BLOB column from file_asset table
-- Migration: V4__remove_blob_column.sql
-- Date: 2025-11-04
-- 
-- We no longer store file content in the database.
-- All files are stored on the filesystem.
-- This table now stores only metadata (id, filename, checksum, size, etc.)

-- Drop the data column (BLOB)
ALTER TABLE file_asset DROP COLUMN IF EXISTS data;

-- Add comment to document the change
COMMENT ON TABLE file_asset IS 'File metadata - actual file content is stored on filesystem at /FISIERE/';

