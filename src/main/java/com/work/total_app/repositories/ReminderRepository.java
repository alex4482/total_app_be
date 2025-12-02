package com.work.total_app.repositories;

import com.work.total_app.models.reminder.Reminder;
import com.work.total_app.models.reminder.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Reminder entities.
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    /**
     * Find all reminders that need warning emails sent (between warning start date and expiration date).
     * Excludes reminders that have already sent all warning emails and inactive reminders.
     */
    @Query("SELECT r FROM Reminder r WHERE r.warningStartDate <= :now " +
           "AND r.expirationDate > :now " +
           "AND r.emailsSentCount < r.warningEmailCount " +
           "AND r.active = true " +
           "ORDER BY r.expirationDate ASC")
    List<Reminder> findRemindersNeedingWarningEmails(@Param("now") Instant now);

    /**
     * Find all reminders that have expired and are still active.
     * These reminders will continue sending emails at the same interval.
     */
    @Query("SELECT r FROM Reminder r WHERE r.expirationDate <= :now " +
           "AND r.active = true " +
           "ORDER BY r.expirationDate ASC")
    List<Reminder> findExpiredReminders(@Param("now") Instant now);

    /**
     * Find reminders by grouping.
     */
    @Query("SELECT DISTINCT r FROM Reminder r JOIN r.groupings g WHERE g = :grouping")
    List<Reminder> findByGrouping(@Param("grouping") String grouping);

    /**
     * Find active reminders by type.
     */
    List<Reminder> findByReminderTypeAndActiveTrue(ReminderType reminderType);
}

