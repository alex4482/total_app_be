package com.work.total_app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for reminder system.
 * Maps properties from application.properties with prefix "app.reminder".
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.reminder")
public class ReminderProperties {
    
    /**
     * Prefix to add to email subject for all reminders.
     * Default: "[Reminder] "
     */
    private String emailPrefix = "[Reminder] ";
    
    /**
     * Extra message to add at the end of all reminder emails.
     * Default: "\n\n---\nAcest reminder a fost generat automat de sistem."
     */
    private String extraMessageSuffix = "\n\n---\nAcest reminder a fost generat automat de sistem.";
}

