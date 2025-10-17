package com.work.total_app.helpers;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.helpers.files.FileSystemHelper;
import com.work.total_app.models.email.EEmailSendStatus;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.file.FileAsset;
import com.work.total_app.models.file.TempUpload;
import lombok.extern.log4j.Log4j2;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

@Log4j2
@Component
public class EmailHelper {

    private static final String fromEmailKey = "emailer.from";
    private static final String fromEmailPasswordKey = "emailer.password";
    private static final String mailServerKey = "emailer.server.address";
    private static final String mailServerPortKey = "emailer.server.port";

    @Autowired
    @Lazy
    private FileSystemHelper fileHelper;
    @Autowired
    private DatabaseHelper databaseHelper;

    public EEmailSendStatus createAndSendMail(EmailData data) {

        String subject = data.getSubject();
        String fromEmail = PropertiesHelper.getProp(fromEmailKey);
        String fromPassword = PropertiesHelper.getProp(fromEmailPasswordKey);

        String mailServer = PropertiesHelper.getProp(mailServerKey);
        Integer mailServerPort = Integer.valueOf(PropertiesHelper.getProp(mailServerPortKey));

        EmailPopulatingBuilder emailBuilder = EmailBuilder.startingBlank()
                .from(fromEmail)
                .to(Arrays.toString(data.getRecipients()))
                .withSubject(subject)
                .withPlainText(data.getMessage());
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
