package com.work.total_app.models.reminder;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class CreateReminderDto {

    /**
     * Email title (will have a prefix added)
     */
    private String emailTitle;

    /**
     * Email message (will have extra message added at the end)
     */
    private String emailMessage;

    /**
     * Date when the reminder expires
     */
    private Instant expirationDate;

    /**
     * Date when to start sending warning emails
     */
    private Instant warningStartDate;

    /**
     * Number of warning emails to send in the warning period
     */
    private Integer warningEmailCount;

    /**
     * Recipient email address
     */
    private String recipientEmail;

    /**
     * Groupings this reminder belongs to (e.g., "masini", "casa", "apartament", "muncaX", "muncaY")
     */
    private List<String> groupings;
}

