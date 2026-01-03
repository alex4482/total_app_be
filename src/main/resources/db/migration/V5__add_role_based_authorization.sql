-- V5: Add Role-Based Authorization System
-- Description: Adds user roles, soft delete support, and session user tracking

-- Add role column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'SUPERUSER';

-- Add soft delete column
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Add userId to refresh_token_state for session tracking
ALTER TABLE refresh_token_state ADD COLUMN IF NOT EXISTS user_id VARCHAR(36);

-- Create index on role for faster queries
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Create index on deleted_at for soft delete queries
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);

-- Create index on user_id in refresh_token_state
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token_state(user_id);

-- Update existing users: ADMIN user gets ADMIN role, others get SUPERUSER
UPDATE users SET role = 'SUPERUSER' WHERE role IS NULL OR role = '';
UPDATE users SET role = 'ADMIN' WHERE LOWER(username) = 'admin';

-- Comments for documentation
COMMENT ON COLUMN users.role IS 'User role: ADMIN (hard delete, 2FA), SUPERUSER (soft delete), MINIUSER (limited access)';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp. NULL = active, NOT NULL = deleted';
COMMENT ON COLUMN refresh_token_state.user_id IS 'UUID of the user owning this session';
