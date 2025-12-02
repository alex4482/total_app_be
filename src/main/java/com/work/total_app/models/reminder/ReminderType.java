package com.work.total_app.models.reminder;

/**
 * Type of reminder scheduling.
 * STANDARD: Uses warning start date, expiration date, and email count to calculate intervals automatically.
 * CUSTOM: Uses exact scheduled times specified in reminder_schedule table.
 */
public enum ReminderType {
    STANDARD,
    CUSTOM
}

