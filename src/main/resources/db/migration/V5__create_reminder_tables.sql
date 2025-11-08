-- Create reminder system tables
-- Migration: V5__create_reminder_tables.sql
-- Date: 2025-01-XX

-- Create reminder table
CREATE TABLE IF NOT EXISTS reminder (
    id UUID PRIMARY KEY,
    email_title VARCHAR(500) NOT NULL,
    email_message TEXT NOT NULL,
    expiration_date TIMESTAMP NOT NULL,
    warning_start_date TIMESTAMP NOT NULL,
    warning_email_count INTEGER NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    emails_sent_count INTEGER NOT NULL DEFAULT 0,
    last_email_sent_at TIMESTAMP,
    expired_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create reminder_grouping table for storing reminder groupings
CREATE TABLE IF NOT EXISTS reminder_grouping (
    reminder_id UUID NOT NULL,
    grouping_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (reminder_id, grouping_name),
    CONSTRAINT fk_reminder_grouping_reminder FOREIGN KEY (reminder_id) 
        REFERENCES reminder(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_reminder_expiration_date ON reminder(expiration_date);
CREATE INDEX idx_reminder_warning_start_date ON reminder(warning_start_date);
CREATE INDEX idx_reminder_recipient_email ON reminder(recipient_email);
CREATE INDEX idx_reminder_expired_email_sent ON reminder(expired_email_sent);
CREATE INDEX idx_reminder_active ON reminder(active);
CREATE INDEX idx_reminder_grouping_name ON reminder_grouping(grouping_name);

-- Add comments
COMMENT ON TABLE reminder IS 'Stores reminders that need to be sent via email';
COMMENT ON COLUMN reminder.email_title IS 'Email title (will have prefix added)';
COMMENT ON COLUMN reminder.email_message IS 'Email message (will have extra message added at the end)';
COMMENT ON COLUMN reminder.expiration_date IS 'Date when the reminder expires';
COMMENT ON COLUMN reminder.warning_start_date IS 'Date when to start sending warning emails';
COMMENT ON COLUMN reminder.warning_email_count IS 'Number of warning emails to send in the warning period';
COMMENT ON COLUMN reminder.recipient_email IS 'Recipient email address';
COMMENT ON COLUMN reminder.emails_sent_count IS 'Number of warning emails sent so far';
COMMENT ON COLUMN reminder.last_email_sent_at IS 'Timestamp of last email sent';
COMMENT ON COLUMN reminder.expired_email_sent IS 'Whether the expiration email has been sent';
COMMENT ON COLUMN reminder.active IS 'Whether the reminder is active. Inactive reminders will not send emails. Set to false to manually stop expired reminders.';

COMMENT ON TABLE reminder_grouping IS 'Stores groupings for reminders (e.g., masini, casa, apartament, muncaX, muncaY)';
COMMENT ON COLUMN reminder_grouping.grouping_name IS 'Name of the grouping';

