-- ============================================
-- V4: Add Authentication System
-- ============================================
-- Multi-user authentication with 2FA support
-- Date: 2025-12-14
-- Author: Authentication System Implementation

-- 1. Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_locked BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_failed_login_at TIMESTAMP,
    account_locked_until TIMESTAMP,
    last_successful_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    requires_email_verification BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Create email whitelist table
CREATE TABLE IF NOT EXISTS email_whitelist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Create email verification codes table
CREATE TABLE IF NOT EXISTS email_verification_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    request_ip VARCHAR(45)
);

-- 4. Create login attempts table
CREATE TABLE IF NOT EXISTS login_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 5. Create indexes for login_attempts
CREATE INDEX IF NOT EXISTS idx_ip_timestamp ON login_attempts(ip_address, created_at);
CREATE INDEX IF NOT EXISTS idx_username_timestamp ON login_attempts(username, created_at);

-- 6. Update refresh_token_state table (add new columns if they don't exist)
ALTER TABLE refresh_token_state 
ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);

ALTER TABLE refresh_token_state 
ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);

