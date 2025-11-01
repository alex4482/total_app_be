package com.work.total_app.helpers;

import com.work.total_app.config.EmailerProperties;
import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.helpers.files.FileSystemHelper;
import com.work.total_app.models.email.EEmailSendStatus;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.file.TempUpload;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

@Log4j2
@Component
public class EmailHelper {

    @Autowired
    private EmailerProperties emailerProperties;

    @Autowired
    @Lazy
    private FileSystemHelper fileHelper;
    @Autowired
    private DatabaseHelper databaseHelper;

    public EEmailSendStatus createAndSendMail(EmailData data) {

        String subject = data.getSubject();

        EmailPopulatingBuilder emailBuilder = EmailBuilder.startingBlank()
                .from(emailerProperties.getFrom())
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
                    
                    // NU ștergem temp files încă - avem nevoie de ele pentru salvarea în Sent
                }
            }
            catch (IOException e)
            {
                log.error("Failed to read attachment files: {}", e.getMessage(), e);
                return EEmailSendStatus.ERROR;
            }
        }

        Email email = emailBuilder.buildEmail();

        // Trimite emailul prin SMTP
        try (Mailer mailer = MailerBuilder
                .withSMTPServer(emailerProperties.getServer().getAddress(), 
                               emailerProperties.getServer().getPort(), 
                               emailerProperties.getFrom(), 
                               emailerProperties.getPassword())
                .withTransportStrategy(TransportStrategy.SMTPS)
                .buildMailer())
        {
            mailer.sendMail(email);
        }
        catch (Exception e)
        {
            log.error("Failed to send email with subject '{}': {}", data.getSubject(), e.getMessage(), e);
            return EEmailSendStatus.ERROR;
        }

        // Salvează în folderul Sent prin IMAP (dacă este activat)
        if (emailerProperties.getImap().isEnabled()) {
            try {
                saveToSentFolder(data);
            } catch (Exception e) {
                // Log error dar nu eșuează trimiterea - emailul a fost deja trimis cu succes
                log.warn("Email sent successfully but failed to save to Sent folder: {}", e.getMessage(), e);
            }
        }

        // Acum putem șterge fișierele temporare (după trimitere și salvare în Sent)
        if (data.getAttachedFilesIds() != null && data.getAttachedFilesIds().length > 0) {
            for (UUID tempId : data.getAttachedFilesIds()) {
                try {
                    databaseHelper.deleteTemp(tempId);
                } catch (Exception e) {
                    log.warn("Failed to delete temp file {}: {}", tempId, e.getMessage());
                }
            }
        }

        return EEmailSendStatus.OK;
    }

    /**
     * Salvează emailul în folderul Sent prin IMAP
     */
    private void saveToSentFolder(EmailData data) throws Exception {
        // Configurare proprietăți Jakarta Mail pentru IMAP
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", emailerProperties.getImap().getAddress());
        props.setProperty("mail.imaps.port", String.valueOf(emailerProperties.getImap().getPort()));
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.ssl.trust", "*");

        Session session = Session.getInstance(props, null);
        Store store = null;
        Folder sentFolder = null;

        try {
            // Conectare la IMAP
            store = session.getStore("imaps");
            store.connect(
                emailerProperties.getImap().getAddress(),
                emailerProperties.getFrom(),
                emailerProperties.getPassword()
            );

            // Deschide folderul Sent
            sentFolder = store.getFolder(emailerProperties.getImap().getSentFolderName());
            
            // Dacă folderul nu există, încearcă alte variante comune
            if (!sentFolder.exists()) {
                String[] possibleNames = {"Sent", "Sent Items", "Sent Messages", "INBOX.Sent"};
                for (String name : possibleNames) {
                    sentFolder = store.getFolder(name);
                    if (sentFolder.exists()) {
                        log.info("Found Sent folder with name: {}", name);
                        break;
                    }
                }
            }

            if (!sentFolder.exists()) {
                log.error("Sent folder not found. Available folders:");
                Folder[] folders = store.getDefaultFolder().list();
                for (Folder f : folders) {
                    log.error("  - {}", f.getFullName());
                }
                throw new MessagingException("Sent folder not found");
            }

            sentFolder.open(Folder.READ_WRITE);

            // Creează MimeMessage
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailerProperties.getFrom()));
            
            // Adaugă destinatari
            if (data.getRecipients() != null) {
                for (String recipient : data.getRecipients()) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                }
            }
            
            message.setSubject(data.getSubject(), "UTF-8");
            message.setSentDate(new Date());

            // Setează conținutul (cu sau fără atașamente)
            if (data.getAttachedFilesIds() != null && data.getAttachedFilesIds().length > 0) {
                // Email cu atașamente
                MimeMultipart multipart = new MimeMultipart();
                
                // Partea text
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(data.getMessage(), "UTF-8");
                multipart.addBodyPart(textPart);

                // Adaugă atașamente (trebuie recitite)
                for (UUID tempId : data.getAttachedFilesIds()) {
                    try {
                        TempUpload tempUpload = databaseHelper.findTempById(tempId).orElse(null);
                        if (tempUpload != null && Files.exists(Path.of(tempUpload.getTempPath()))) {
                            MimeBodyPart attachmentPart = new MimeBodyPart();
                            byte[] content = Files.readAllBytes(Path.of(tempUpload.getTempPath()));
                            attachmentPart.setDataHandler(new jakarta.activation.DataHandler(
                                new jakarta.mail.util.ByteArrayDataSource(content, tempUpload.getContentType())
                            ));
                            attachmentPart.setFileName(tempUpload.getOriginalFilename());
                            multipart.addBodyPart(attachmentPart);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to attach file to sent message: {}", e.getMessage());
                    }
                }
                
                message.setContent(multipart);
            } else {
                // Email simplu, doar text
                message.setText(data.getMessage(), "UTF-8");
            }

            message.saveChanges();

            // Salvează în folderul Sent
            sentFolder.appendMessages(new Message[]{message});
            
            log.info("Email saved to Sent folder successfully");

        } finally {
            // Închide resursele
            if (sentFolder != null && sentFolder.isOpen()) {
                sentFolder.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
    }
}
