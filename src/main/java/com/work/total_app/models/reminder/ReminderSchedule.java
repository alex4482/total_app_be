package com.work.total_app.models.reminder;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for custom reminder schedule times.
 * Used when reminder type is CUSTOM to specify exact times when emails should be sent.
 */
@Entity
@Table(name = "reminder_schedule")
@Data
public class ReminderSchedule {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id", nullable = false)
    private Reminder reminder;

    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    @Column(name = "sent", nullable = false)
    private Boolean sent = false;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ReminderSchedule() {}

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (sent == null) {
            sent = false;
        }
    }
}

