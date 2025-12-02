-- Add reminder_type column to reminder table
ALTER TABLE reminder ADD COLUMN IF NOT EXISTS reminder_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

-- Create reminder_schedule table for custom reminder schedules
CREATE TABLE IF NOT EXISTS reminder_schedule (
    id UUID NOT NULL PRIMARY KEY,
    reminder_id UUID NOT NULL,
    scheduled_time TIMESTAMP NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_reminder_schedule_reminder FOREIGN KEY (reminder_id) REFERENCES reminder(id) ON DELETE CASCADE
);

-- Create index for efficient querying
CREATE INDEX IF NOT EXISTS idx_reminder_schedule_reminder_id ON reminder_schedule(reminder_id);
CREATE INDEX IF NOT EXISTS idx_reminder_schedule_scheduled_time ON reminder_schedule(scheduled_time);
CREATE INDEX IF NOT EXISTS idx_reminder_schedule_sent ON reminder_schedule(sent);

COMMENT ON TABLE reminder_schedule IS 'Exact scheduled times for custom reminder emails';
COMMENT ON COLUMN reminder.reminder_type IS 'Type of reminder: STANDARD (auto-calculated intervals) or CUSTOM (exact scheduled times)';

