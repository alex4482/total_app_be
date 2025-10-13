package com.work.total_app.models.email;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class EmailSendData {
    private String address;
    private String message;
    private String title;
    private MultipartFile[] attachedFiles;
}
