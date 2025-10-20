package com.work.total_app.helpers;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.helpers.files.FileSystemHelper;
import com.work.total_app.models.email.EEmailSendStatus;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.file.TempUpload;
import lombok.extern.log4j.Log4j2;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Log4j2
@Component
public class EmailHelper {

    @Value("${emailer.from}")
    private String fromEmail;

    @Value("${emailer.password}")
    private String fromPassword;

    @Value("${emailer.server.address}")
    private String mailServer;

    @Value("${emailer.server.port}")
    private Integer mailServerPort;

    @Autowired
    @Lazy
    private FileSystemHelper fileHelper;
    @Autowired
    private DatabaseHelper databaseHelper;

    public EEmailSendStatus createAndSendMail(EmailData data) {

        String subject = data.getSubject();

        EmailPopulatingBuilder emailBuilder = EmailBuilder.startingBlank()
                .from(fromEmail)
                .withSubject(subject)
                .withPlainText(data.getMessage());

        // Add each recipient individually
        if (data.getRecipients() != null) {
            for (String recipient : data.getRecipients()) {
                emailBuilder = emailBuilder.to(recipient);
            }
        }
        if (data.getAttachedFilesIds() != null && data.getAttachedFilesIds().length > 0)
        {
            try
            {
                String fileName;
                String fileType;
                byte[] content;
                for (int i = 0; i < data.getAttachedFilesIds().length; i++)
                {
                    UUID tempId = data.getAttachedFilesIds()[i];
                    TempUpload tempUpload = databaseHelper.findTempById(tempId)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid tempId: " + tempId));

                    content = Files.readAllBytes(Path.of(tempUpload.getTempPath()));

                    fileName = tempUpload.getOriginalFilename();
                    fileType = tempUpload.getContentType();
                    emailBuilder = emailBuilder.withAttachment(fileName, content, fileType);

                    // 7) Remove temp row now (file stays on disk as TEMP until afterCommit)
                    databaseHelper.deleteTemp(tempId);
                }
            }
            catch (IOException e)
            {
                return EEmailSendStatus.ERROR;
                //TODO: LOG SHOW ERROR
            }
        }

        Email email = emailBuilder.buildEmail();

        // TODO: move mailer outside of method to have only 1 ??
        try (Mailer mailer = MailerBuilder
                .withSMTPServer(mailServer, mailServerPort, fromEmail, fromPassword)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .buildMailer())
        {
            mailer.sendMail(email);
        }
        catch (Exception e)
        {
           // log.error(e.getMessage());

            return EEmailSendStatus.ERROR;
        }

        return EEmailSendStatus.OK;
    }
}
