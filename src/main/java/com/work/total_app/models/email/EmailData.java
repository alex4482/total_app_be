package com.work.total_app.models.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class EmailData {

    private String title;
    private String message;
    private MultipartFile[] attachedFiles;
    private String[] destinationAddresses;
}
