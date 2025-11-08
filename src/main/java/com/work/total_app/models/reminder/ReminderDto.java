package com.work.total_app.models.reminder;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReminderDto {

    private UUID id;
    private String emailTitle;
    private String emailMessage;
    private Instant expirationDate;
    private Instant warningStartDate;
    private Integer warningEmailCount;
    private String recipientEmail;
    private List<String> groupings;
    private Integer emailsSentCount;
    private Instant lastEmailSentAt;
    private Boolean expiredEmailSent;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

