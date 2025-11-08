package com.work.total_app.models.reminder;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity for reminders that need to be sent via email.
 * Tracks expiration dates, warning periods, and email sending status.
 */
@Entity
@Table(name = "reminder")
@Data
public class Reminder {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "email_title", nullable = false, length = 500)
    private String emailTitle;

    @Column(name = "email_message", nullable = false, columnDefinition = "TEXT")
    private String emailMessage;

    @Column(name = "expiration_date", nullable = false)
    private Instant expirationDate;

    @Column(name = "warning_start_date", nullable = false)
    private Instant warningStartDate;

    @Column(name = "warning_email_count", nullable = false)
    private Integer warningEmailCount;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @ElementCollection
    @CollectionTable(name = "reminder_grouping", joinColumns = @JoinColumn(name = "reminder_id"))
    @Column(name = "grouping_name")
    private List<String> groupings = new ArrayList<>();

    @Column(name = "emails_sent_count", nullable = false)
    private Integer emailsSentCount = 0;

    @Column(name = "last_email_sent_at")
    private Instant lastEmailSentAt;

    @Column(name = "expired_email_sent", nullable = false)
    private Boolean expiredEmailSent = false;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Reminder() {}

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
        if (emailsSentCount == null) {
            emailsSentCount = 0;
        }
        if (expiredEmailSent == null) {
            expiredEmailSent = false;
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

