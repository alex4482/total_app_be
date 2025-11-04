-- Create backup_metadata table for tracking database backups
-- Migration: V3__create_backup_metadata_table.sql
-- Date: 2025-11-03

CREATE TABLE IF NOT EXISTS backup_metadata (
    id BIGSERIAL PRIMARY KEY,
    backup_name VARCHAR(255) NOT NULL UNIQUE,
    backup_type VARCHAR(20) NOT NULL CHECK (backup_type IN ('MANUAL', 'AUTOMATIC')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    local_path VARCHAR(1024) NOT NULL,
    google_drive_file_id VARCHAR(255),
    size_bytes BIGINT,
    description TEXT
);

-- Create index on backup_name for faster lookups
CREATE INDEX idx_backup_metadata_backup_name ON backup_metadata(backup_name);

-- Create index on backup_type for filtering
CREATE INDEX idx_backup_metadata_backup_type ON backup_metadata(backup_type);

-- Create index on created_at for sorting by date
CREATE INDEX idx_backup_metadata_created_at ON backup_metadata(created_at DESC);

-- Add comment to table
COMMENT ON TABLE backup_metadata IS 'Stores metadata about database backups (both manual and automatic)';
COMMENT ON COLUMN backup_metadata.backup_name IS 'Unique name of the backup (e.g., backup_2025-11-03_14-30-15_manual)';
COMMENT ON COLUMN backup_metadata.backup_type IS 'Type of backup: MANUAL or AUTOMATIC';
COMMENT ON COLUMN backup_metadata.local_path IS 'Full path to the backup ZIP file on local filesystem';
COMMENT ON COLUMN backup_metadata.google_drive_file_id IS 'Google Drive file ID if uploaded to Google Drive';
COMMENT ON COLUMN backup_metadata.size_bytes IS 'Size of the backup ZIP file in bytes';
COMMENT ON COLUMN backup_metadata.description IS 'Optional description for the backup';

