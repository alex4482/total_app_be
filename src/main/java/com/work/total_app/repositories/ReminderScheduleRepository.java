package com.work.total_app.repositories;

import com.work.total_app.models.reminder.ReminderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderScheduleRepository extends JpaRepository<ReminderSchedule, UUID> {

    /**
     * Find all unsent schedules for a reminder that should be sent now or in the past.
     */
    @Query("SELECT s FROM ReminderSchedule s WHERE s.reminder.id = :reminderId AND s.sent = false AND s.scheduledTime <= :now ORDER BY s.scheduledTime ASC")
    List<ReminderSchedule> findUnsentSchedulesForReminder(@Param("reminderId") UUID reminderId, @Param("now") Instant now);

    /**
     * Find all schedules for a reminder.
     */
    @Query("SELECT s FROM ReminderSchedule s WHERE s.reminder.id = :reminderId ORDER BY s.scheduledTime ASC")
    List<ReminderSchedule> findByReminderIdOrderByScheduledTimeAsc(@Param("reminderId") UUID reminderId);
}

