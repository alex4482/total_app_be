package com.work.total_app.services.security;

import com.work.total_app.helpers.EmailHelper;
import com.work.total_app.models.email.EEmailSendStatus;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.user.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service pentru trimiterea de notificări de securitate către utilizatori
 */
@Service
@Log4j2
public class SecurityNotificationService {
    
    @Autowired
    private EmailHelper emailHelper;
    
    @Autowired
    private EmailWhitelistService emailWhitelistService;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    
    /**
     * Trimite notificare când contul este blocat după încercări eșuate
     */
    public void sendAccountLockedNotification(User user, String lastAttemptIp, int failedAttempts, Instant lockedUntil) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Cannot send account locked notification to user {} - no email address", user.getUsername());
            return;
        }
        
        // Check if email is whitelisted
        if (!emailWhitelistService.isEmailWhitelisted(user.getEmail())) {
            log.warn("Cannot send account locked notification to {} - email not whitelisted", user.getEmail());
            return;
        }
        
        EmailData emailData = new EmailData();
        emailData.setRecipients(new String[]{user.getEmail()});
        emailData.setSubject("[SECURITATE] Contul tău a fost blocat temporar");
        
        String message = String.format(
            "Bună %s,\n\n" +
            "Contul tău a fost blocat temporar din cauza mai multor încercări de autentificare eșuate.\n\n" +
            "Detalii:\n" +
            "- Număr încercări eșuate: %d\n" +
            "- Ultima încercare de pe IP: %s\n" +
            "- Blocat până la: %s\n\n" +
            "Dacă nu ai fost tu care a încercat să se autentifice, contul tău ar putea fi ținta unui atac.\n\n" +
            "Ce poți face:\n" +
            "1. Așteaptă până când blocarea expiră\n" +
            "2. Schimbă-ți parola după deblocare\n" +
            "3. Contactează administratorul dacă suspectezi activitate neautorizată\n\n" +
            "Dacă ai fost tu, poți aștepta până la expirarea blocării sau contacta administratorul " +
            "pentru deblocare manuală.\n\n" +
            "---\n" +
            "Acest email a fost generat automat de sistemul de securitate.",
            user.getUsername(),
            failedAttempts,
            lastAttemptIp,
            DATE_FORMATTER.format(lockedUntil)
        );
        
        emailData.setMessage(message);
        
        EEmailSendStatus status = emailHelper.createAndSendMail(emailData);
        if (status == EEmailSendStatus.OK) {
            log.info("Account locked notification sent to user: {} (email: {})", 
                user.getUsername(), user.getEmail());
        } else {
            log.error("Failed to send account locked notification to user: {}", user.getUsername());
        }
    }
    
    /**
     * Trimite notificare când contul necesită verificare email (după 6 eșecuri)
     */
    public void sendEmailVerificationRequiredNotification(User user, String lastAttemptIp, int failedAttempts) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Cannot send verification required notification to user {} - no email address", 
                user.getUsername());
            return;
        }
        
        if (!emailWhitelistService.isEmailWhitelisted(user.getEmail())) {
            log.warn("Cannot send verification required notification to {} - email not whitelisted", 
                user.getEmail());
            return;
        }
        
        EmailData emailData = new EmailData();
        emailData.setRecipients(new String[]{user.getEmail()});
        emailData.setSubject("[SECURITATE] Verificare prin email necesară pentru autentificare");
        
        String message = String.format(
            "Bună %s,\n\n" +
            "Din cauza mai multor încercări de autentificare eșuate, următoarea ta autentificare " +
            "va necesita verificare prin email.\n\n" +
            "Detalii:\n" +
            "- Număr încercări eșuate: %d\n" +
            "- Ultima încercare de pe IP: %s\n" +
            "- Data: %s\n\n" +
            "La următoarea autentificare va trebui să:\n" +
            "1. Introduci username și parola\n" +
            "2. Soliciți un cod de verificare prin email\n" +
            "3. Introduci codul primit pentru a te autentifica\n\n" +
            "Dacă nu ai fost tu care a încercat să se autentifice, te rugăm să îți schimbi parola " +
            "cât mai curând posibil.\n\n" +
            "---\n" +
            "Acest email a fost generat automat de sistemul de securitate.",
            user.getUsername(),
            failedAttempts,
            lastAttemptIp,
            DATE_FORMATTER.format(Instant.now())
        );
        
        emailData.setMessage(message);
        
        EEmailSendStatus status = emailHelper.createAndSendMail(emailData);
        if (status == EEmailSendStatus.OK) {
            log.info("Email verification required notification sent to user: {} (email: {})", 
                user.getUsername(), user.getEmail());
        } else {
            log.error("Failed to send email verification required notification to user: {}", 
                user.getUsername());
        }
    }
    
    /**
     * Trimite notificare când IP-ul este blacklisted
     */
    public void sendIpBlacklistedNotification(String email, String ip, Instant unblockTime) {
        if (email == null || email.isEmpty()) {
            return;
        }
        
        if (!emailWhitelistService.isEmailWhitelisted(email)) {
            return;
        }
        
        EmailData emailData = new EmailData();
        emailData.setRecipients(new String[]{email});
        emailData.setSubject("[SECURITATE CRITICĂ] IP-ul tău a fost blocat");
        
        String message = String.format(
            "ALERTĂ DE SECURITATE\n\n" +
            "IP-ul %s a fost blocat temporar din cauza activității suspicioase și a încercărilor " +
            "excesive de autentificare eșuată.\n\n" +
            "Blocare până la: %s\n\n" +
            "Dacă nu recunoști această activitate, cineva ar putea încerca să acceseze contul tău.\n\n" +
            "Acțiuni recomandate:\n" +
            "- Schimbă-ți parola imediat după deblocare\n" +
            "- Verifică dacă există activitate neautorizată în cont\n" +
            "- Contactează administratorul dacă suspectezi o breșă de securitate\n\n" +
            "---\n" +
            "Acest email a fost generat automat de sistemul de securitate.",
            ip,
            DATE_FORMATTER.format(unblockTime)
        );
        
        emailData.setMessage(message);
        
        EEmailSendStatus status = emailHelper.createAndSendMail(emailData);
        if (status == EEmailSendStatus.OK) {
            log.info("IP blacklisted notification sent to: {}", email);
        } else {
            log.error("Failed to send IP blacklisted notification to: {}", email);
        }
    }
    
    /**
     * Trimite notificare de succes când contul este deblocat
     */
    public void sendAccountUnlockedNotification(User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            return;
        }
        
        if (!emailWhitelistService.isEmailWhitelisted(user.getEmail())) {
            return;
        }
        
        EmailData emailData = new EmailData();
        emailData.setRecipients(new String[]{user.getEmail()});
        emailData.setSubject("[INFO] Contul tău a fost deblocat");
        
        String message = String.format(
            "Bună %s,\n\n" +
            "Contul tău a fost deblocat cu succes. Poți să te autentifici din nou.\n\n" +
            "Dacă nu ai solicitat tu deblocarea, te rugăm să contactezi administratorul.\n\n" +
            "---\n" +
            "Acest email a fost generat automat de sistem.",
            user.getUsername()
        );
        
        emailData.setMessage(message);
        
        emailHelper.createAndSendMail(emailData);
    }
}
