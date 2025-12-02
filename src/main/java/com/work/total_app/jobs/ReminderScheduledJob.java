package com.work.total_app.jobs;

import com.work.total_app.config.ReminderProperties;
import com.work.total_app.helpers.EmailHelper;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.email.EEmailSendStatus;
import com.work.total_app.models.reminder.Reminder;
import com.work.total_app.models.reminder.ReminderSchedule;
import com.work.total_app.models.reminder.ReminderType;
import com.work.total_app.repositories.ReminderRepository;
import com.work.total_app.repositories.ReminderScheduleRepository;
import com.work.total_app.services.ReminderService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job that checks for reminders that need emails sent.
 * Runs once a day at 9:00 AM to check for:
 * 1. Reminders that need warning emails (between warning start date and expiration date)
 * 2. Reminders that have expired and need expiration email sent
 */
@Component
@Log4j2
public class ReminderScheduledJob {

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private EmailHelper emailHelper;

    @Autowired
    private ReminderProperties reminderProperties;

    @Autowired
    private ReminderScheduleRepository reminderScheduleRepository;

    /**
     * Check for reminders that need emails sent.
     * Runs once a day at 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void processReminders() {
        log.info("Starting reminder processing job...");
        Instant now = Instant.now();

        // Process STANDARD type reminders
        processWarningEmails(now);
        processExpiredReminders(now);

        // Process CUSTOM type reminders
        processCustomReminders(now);

        log.info("Reminder processing job completed");
    }

    /**
     * Process reminders that need warning emails (STANDARD type only).
     */
    private void processWarningEmails(Instant now) {
        List<Reminder> reminders = reminderRepository.findRemindersNeedingWarningEmails(now);
        // Filter only STANDARD type reminders
        reminders = reminders.stream()
                .filter(r -> r.getReminderType() == null || r.getReminderType() == ReminderType.STANDARD)
                .toList();
        log.info("Found {} STANDARD reminders needing warning emails", reminders.size());

        for (Reminder reminder : reminders) {
            try {
                // Check if enough time has passed since the last email (or start date)
                if (shouldSendWarningEmail(reminder, now)) {
                    sendWarningEmail(reminder);
                }
            } catch (Exception e) {
                log.error("Error processing warning email for reminder {}: {}", reminder.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Determine if a warning email should be sent now.
     * Distributes emails evenly across the warning period.
     */
    private boolean shouldSendWarningEmail(Reminder reminder, Instant now) {
        // If we've already sent all warning emails, don't send more
        if (reminder.getEmailsSentCount() >= reminder.getWarningEmailCount()) {
            return false;
        }

        // Calculate the warning period duration
        long totalSeconds = ChronoUnit.SECONDS.between(reminder.getWarningStartDate(), reminder.getExpirationDate());
        if (totalSeconds <= 0) {
            log.warn("Invalid date range for reminder {}: warning start date >= expiration date", reminder.getId());
            return false;
        }

        // Calculate interval between emails (in seconds)
        // Divide the period into (count+1) intervals so emails are evenly distributed
        long intervalSeconds = totalSeconds / (reminder.getWarningEmailCount() + 1);

        // Calculate when the next email should be sent
        Instant nextEmailTime;
        if (reminder.getLastEmailSentAt() != null) {
            // Next email should be sent after the interval from last email
            nextEmailTime = reminder.getLastEmailSentAt().plusSeconds(intervalSeconds);
        } else {
            // First email should be sent at the start date + first interval
            // This ensures emails are evenly distributed before expiration
            nextEmailTime = reminder.getWarningStartDate().plusSeconds(intervalSeconds);
        }

        // Check if it's time to send the next email
        return now.isAfter(nextEmailTime) || now.equals(nextEmailTime);
    }

    /**
     * Process reminders that have expired (STANDARD type only).
     * These reminders will continue sending emails at the same interval until manually stopped.
     */
    private void processExpiredReminders(Instant now) {
        List<Reminder> expiredReminders = reminderRepository.findExpiredReminders(now);
        // Filter only STANDARD type reminders
        expiredReminders = expiredReminders.stream()
                .filter(r -> r.getReminderType() == null || r.getReminderType() == ReminderType.STANDARD)
                .toList();
        log.info("Found {} expired STANDARD reminders that are still active", expiredReminders.size());

        for (Reminder reminder : expiredReminders) {
            try {
                // Check if enough time has passed since the last email (using same interval as before expiration)
                if (shouldSendExpiredEmail(reminder, now)) {
                    sendExpirationEmail(reminder);
                }
            } catch (Exception e) {
                log.error("Error processing expiration email for reminder {}: {}", reminder.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Process CUSTOM type reminders that have scheduled times due.
     */
    private void processCustomReminders(Instant now) {
        List<Reminder> customReminders = reminderRepository.findByReminderTypeAndActiveTrue(ReminderType.CUSTOM);
        log.info("Found {} active CUSTOM reminders", customReminders.size());

        for (Reminder reminder : customReminders) {
            try {
                // Find unsent schedules that are due
                List<ReminderSchedule> dueSchedules = reminderScheduleRepository.findUnsentSchedulesForReminder(reminder.getId(), now);
                
                for (ReminderSchedule schedule : dueSchedules) {
                    sendCustomReminderEmail(reminder, schedule);
                }
            } catch (Exception e) {
                log.error("Error processing custom reminder {}: {}", reminder.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Send email for a custom reminder at a specific scheduled time.
     */
    private void sendCustomReminderEmail(Reminder reminder, ReminderSchedule schedule) {
        String subject = reminderProperties.getEmailPrefix() + reminder.getEmailTitle();
        String message = reminder.getEmailMessage() + reminderProperties.getExtraMessageSuffix();
        
        // Add context about when this was scheduled
        if (schedule.getScheduledTime().isBefore(Instant.now())) {
            long daysAgo = ChronoUnit.DAYS.between(schedule.getScheduledTime(), Instant.now());
            message += "\n\n📅 Acest reminder a fost programat pentru " + schedule.getScheduledTime() + 
                      " (acum " + daysAgo + " zile).";
        } else {
            message += "\n\n📅 Acest reminder a fost programat pentru " + schedule.getScheduledTime() + ".";
        }

        EmailData emailData = new EmailData();
        emailData.setSubject(subject);
        emailData.setMessage(message);
        emailData.setRecipients(new String[]{reminder.getRecipientEmail()});

        EEmailSendStatus status = emailHelper.createAndSendMail(emailData);
        
        if (status == EEmailSendStatus.OK) {
            // Mark schedule as sent
            schedule.setSent(true);
            schedule.setSentAt(Instant.now());
            reminderScheduleRepository.save(schedule);
            
            // Update reminder's email count
            reminder.setEmailsSentCount(reminder.getEmailsSentCount() + 1);
            reminder.setLastEmailSentAt(Instant.now());
            reminderRepository.save(reminder);
            
            log.info("Custom reminder email sent successfully for reminder {} at scheduled time {}", 
                    reminder.getId(), schedule.getScheduledTime());
        } else {
            log.error("Failed to send custom reminder email for reminder {} at scheduled time {}", 
                    reminder.getId(), schedule.getScheduledTime());
        }
    }

    /**
     * Determine if an expired reminder email should be sent now.
     * Uses the same interval calculation as before expiration.
     */
    private boolean shouldSendExpiredEmail(Reminder reminder, Instant now) {
        // Calculate the original warning period duration (same as before expiration)
        long totalSeconds = ChronoUnit.SECONDS.between(reminder.getWarningStartDate(), reminder.getExpirationDate());
        if (totalSeconds <= 0) {
            log.warn("Invalid date range for reminder {}: warning start date >= expiration date", reminder.getId());
            return false;
        }

        // Calculate interval between emails (same as before expiration)
        long intervalSeconds = totalSeconds / (reminder.getWarningEmailCount() + 1);

        // Calculate when the next email should be sent
        Instant nextEmailTime;
        if (reminder.getLastEmailSentAt() != null) {
            // Next email should be sent after the interval from last email
            nextEmailTime = reminder.getLastEmailSentAt().plusSeconds(intervalSeconds);
        } else {
            // If no email was sent yet after expiration, send immediately
            // Then continue at the same interval
            return true;
        }

        // Check if it's time to send the next email
        return now.isAfter(nextEmailTime) || now.equals(nextEmailTime);
    }

    /**
     * Send warning email for a reminder.
     */
    private void sendWarningEmail(Reminder reminder) {
        String subject = reminderProperties.getEmailPrefix() + reminder.getEmailTitle();
        String message = reminder.getEmailMessage() + reminderProperties.getExtraMessageSuffix();
        
        // Add warning details
        long daysUntilExpiration = ChronoUnit.DAYS.between(Instant.now(), reminder.getExpirationDate());
        message += "\n\n⚠️ ATENȚIE: Acest reminder expiră în " + daysUntilExpiration + " zile (data: " + reminder.getExpirationDate() + ").";

        EmailData emailData = new EmailData();
        emailData.setSubject(subject);
        emailData.setMessage(message);
        emailData.setRecipients(new String[]{reminder.getRecipientEmail()});

        EEmailSendStatus status = emailHelper.createAndSendMail(emailData);
        
        if (status == EEmailSendStatus.OK) {
            reminderService.markWarningEmailSent(reminder);
            log.info("Warning email sent successfully for reminder {}", reminder.getId());
        } else {
            log.error("Failed to send warning email for reminder {}", reminder.getId());
        }
    }

    /**
     * Send expiration email for a reminder.
     * This will be sent repeatedly at the same interval until the reminder is manually stopped.
     */
    private void sendExpirationEmail(Reminder reminder) {
        String subject = reminderProperties.getEmailPrefix() + reminder.getEmailTitle();
        String message = reminder.getEmailMessage() + reminderProperties.getExtraMessageSuffix();
        
        // Add expiration notice
        long daysSinceExpiration = ChronoUnit.DAYS.between(reminder.getExpirationDate(), Instant.now());
        message += "\n\n🚨 EXPIRAT: Acest reminder a expirat la data de " + reminder.getExpirationDate() + 
                   " (acum " + daysSinceExpiration + " zile).";

        EmailData emailData = new EmailData();
        emailData.setSubject(subject);
        emailData.setMessage(message);
        emailData.setRecipients(new String[]{reminder.getRecipientEmail()});

        EEmailSendStatus status = emailHelper.createAndSendMail(emailData);
        
        if (status == EEmailSendStatus.OK) {
            reminderService.markExpiredEmailSent(reminder);
            log.info("Expiration email sent successfully for reminder {} (expired {} days ago)", 
                    reminder.getId(), daysSinceExpiration);
        } else {
            log.error("Failed to send expiration email for reminder {}", reminder.getId());
        }
    }
}

