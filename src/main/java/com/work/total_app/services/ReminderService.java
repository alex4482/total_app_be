package com.work.total_app.services;

import com.work.total_app.models.reminder.CreateReminderDto;
import com.work.total_app.models.reminder.Reminder;
import com.work.total_app.models.reminder.ReminderDto;
import com.work.total_app.models.reminder.ReminderSchedule;
import com.work.total_app.models.reminder.ReminderType;
import com.work.total_app.models.reminder.UpdateReminderDto;
import com.work.total_app.repositories.ReminderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ReminderService {

    @Autowired
    private ReminderRepository reminderRepository;

    /**
     * Create a new reminder.
     */
    @Transactional
    public ReminderDto createReminder(CreateReminderDto dto) {
        Reminder reminder = new Reminder();
        reminder.setEmailTitle(dto.getEmailTitle());
        reminder.setEmailMessage(dto.getEmailMessage());
        reminder.setExpirationDate(dto.getExpirationDate());
        reminder.setWarningStartDate(dto.getWarningStartDate());
        reminder.setWarningEmailCount(dto.getWarningEmailCount());
        reminder.setRecipientEmail(dto.getRecipientEmail());
        reminder.setGroupings(dto.getGroupings() != null ? dto.getGroupings() : List.of());
        
        // Set reminder type (default to STANDARD if not specified)
        ReminderType reminderType = dto.getReminderType() != null ? dto.getReminderType() : ReminderType.STANDARD;
        reminder.setReminderType(reminderType);

        // For CUSTOM type, create schedule entries
        if (reminderType == ReminderType.CUSTOM && dto.getScheduledTimes() != null && !dto.getScheduledTimes().isEmpty()) {
            List<ReminderSchedule> schedules = new ArrayList<>();
            for (Instant scheduledTime : dto.getScheduledTimes()) {
                ReminderSchedule schedule = new ReminderSchedule();
                schedule.setReminder(reminder);
                schedule.setScheduledTime(scheduledTime);
                schedule.setSent(false);
                schedules.add(schedule);
            }
            reminder.setSchedules(schedules);
        }

        Reminder saved = reminderRepository.save(reminder);
        log.info("Created reminder with ID: {} and type: {}", saved.getId(), reminderType);
        return toDto(saved);
    }

    /**
     * Get all reminders.
     */
    @Transactional(readOnly = true)
    public List<ReminderDto> getAllReminders() {
        return reminderRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get reminder by ID.
     */
    @Transactional(readOnly = true)
    public ReminderDto getReminderById(UUID id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found with id: " + id));
        return toDto(reminder);
    }

    /**
     * Get reminders by grouping.
     */
    @Transactional(readOnly = true)
    public List<ReminderDto> getRemindersByGrouping(String grouping) {
        return reminderRepository.findByGrouping(grouping).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete reminder by ID.
     */
    @Transactional
    public void deleteReminder(UUID id) {
        if (!reminderRepository.existsById(id)) {
            throw new IllegalArgumentException("Reminder not found with id: " + id);
        }
        reminderRepository.deleteById(id);
        log.info("Deleted reminder with ID: {}", id);
    }

    /**
     * Update reminder's email sending status.
     */
    @Transactional
    public void markWarningEmailSent(Reminder reminder) {
        reminder.setEmailsSentCount(reminder.getEmailsSentCount() + 1);
        reminder.setLastEmailSentAt(Instant.now());
        reminderRepository.save(reminder);
    }

    /**
     * Mark reminder as expired email sent (first time).
     * Note: Expired reminders will continue sending emails at the same interval until manually stopped.
     */
    @Transactional
    public void markExpiredEmailSent(Reminder reminder) {
        if (!reminder.getExpiredEmailSent()) {
            reminder.setExpiredEmailSent(true);
        }
        reminder.setLastEmailSentAt(Instant.now());
        reminderRepository.save(reminder);
    }

    /**
     * Update reminder details.
     */
    @Transactional
    public ReminderDto updateReminder(UUID id, UpdateReminderDto dto) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found with id: " + id));

        // Update only fields that are provided (not null)
        if (dto.getEmailTitle() != null) {
            reminder.setEmailTitle(dto.getEmailTitle());
        }
        if (dto.getEmailMessage() != null) {
            reminder.setEmailMessage(dto.getEmailMessage());
        }
        if (dto.getExpirationDate() != null) {
            reminder.setExpirationDate(dto.getExpirationDate());
        }
        if (dto.getWarningStartDate() != null) {
            reminder.setWarningStartDate(dto.getWarningStartDate());
        }
        if (dto.getWarningEmailCount() != null) {
            reminder.setWarningEmailCount(dto.getWarningEmailCount());
        }
        if (dto.getRecipientEmail() != null) {
            reminder.setRecipientEmail(dto.getRecipientEmail());
        }
        if (dto.getGroupings() != null) {
            reminder.setGroupings(dto.getGroupings());
        }
        if (dto.getReminderType() != null) {
            reminder.setReminderType(dto.getReminderType());
        }

        // Update schedules if provided (only for CUSTOM type)
        if (dto.getScheduledTimes() != null && reminder.getReminderType() == ReminderType.CUSTOM) {
            // Clear existing schedules
            reminder.getSchedules().clear();
            // Add new schedules
            for (Instant scheduledTime : dto.getScheduledTimes()) {
                ReminderSchedule schedule = new ReminderSchedule();
                schedule.setReminder(reminder);
                schedule.setScheduledTime(scheduledTime);
                schedule.setSent(false);
                reminder.getSchedules().add(schedule);
            }
        }

        Reminder saved = reminderRepository.save(reminder);
        log.info("Updated reminder with ID: {}", saved.getId());
        return toDto(saved);
    }

    /**
     * Stop or activate a reminder manually.
     */
    @Transactional
    public ReminderDto setReminderActive(UUID id, boolean active) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found with id: " + id));
        reminder.setActive(active);
        Reminder saved = reminderRepository.save(reminder);
        log.info("Reminder {} set to active={}", id, active);
        return toDto(saved);
    }

    /**
     * Convert entity to DTO.
     */
    private ReminderDto toDto(Reminder reminder) {
        ReminderDto dto = new ReminderDto();
        dto.setId(reminder.getId());
        dto.setEmailTitle(reminder.getEmailTitle());
        dto.setEmailMessage(reminder.getEmailMessage());
        dto.setExpirationDate(reminder.getExpirationDate());
        dto.setWarningStartDate(reminder.getWarningStartDate());
        dto.setWarningEmailCount(reminder.getWarningEmailCount());
        dto.setRecipientEmail(reminder.getRecipientEmail());
        // Create a new ArrayList to detach from Hibernate proxy and avoid lazy initialization issues
        dto.setGroupings(reminder.getGroupings() != null ? new ArrayList<>(reminder.getGroupings()) : new ArrayList<>());
        dto.setEmailsSentCount(reminder.getEmailsSentCount());
        dto.setLastEmailSentAt(reminder.getLastEmailSentAt());
        dto.setExpiredEmailSent(reminder.getExpiredEmailSent());
        dto.setActive(reminder.getActive());
        dto.setCreatedAt(reminder.getCreatedAt());
        dto.setUpdatedAt(reminder.getUpdatedAt());
        dto.setReminderType(reminder.getReminderType() != null ? reminder.getReminderType() : ReminderType.STANDARD);
        
        // Include scheduled times for CUSTOM type
        if (reminder.getReminderType() == ReminderType.CUSTOM && reminder.getSchedules() != null) {
            List<Instant> scheduledTimes = reminder.getSchedules().stream()
                    .map(ReminderSchedule::getScheduledTime)
                    .sorted()
                    .collect(Collectors.toList());
            dto.setScheduledTimes(scheduledTimes);
        }
        
        return dto;
    }
}

