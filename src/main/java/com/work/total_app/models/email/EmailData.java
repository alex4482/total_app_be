package com.work.total_app.models.email;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailData {

    private String subject;
    private String message;
    private String[] attachedFilesIds;
    private String[] recipients;
}
