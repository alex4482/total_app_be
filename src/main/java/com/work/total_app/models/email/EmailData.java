package com.work.total_app.models.email;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EmailData {

    private String subject;
    private String message;
    private UUID[] attachedFilesIds;
    private String[] recipients;
}
