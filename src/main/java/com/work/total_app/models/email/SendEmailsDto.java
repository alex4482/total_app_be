package com.work.total_app.models.email;

import lombok.Getter;

import java.util.List;

@Getter
public class SendEmailsDto {
    private List<EmailData> data;
}
