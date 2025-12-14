package com.work.total_app.services.security;

import com.work.total_app.helpers.EmailHelper;
import com.work.total_app.models.email.EEmailSendStatus;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.user.EmailVerificationCode;
import com.work.total_app.models.user.User;
import com.work.total_app.repositories.EmailVerificationCodeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@Log4j2
public class EmailVerificationService {
    
    @Autowired
    private EmailVerificationCodeRepository verificationCodeRepository;
    
    @Autowired
    private EmailHelper emailHelper;
    
    @Autowired
    private EmailWhitelistService emailWhitelistService;
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_EXPIRY_MINUTES = 15;
    private static final int MAX_CODES_PER_HOUR = 5;
    
    /**
     * Generează și trimite un cod de verificare pe email
     */
    @Transactional
    public boolean generateAndSendCode(User user, String email, String requestIp) {
        // Verifică dacă emailul este în whitelist
        if (!emailWhitelistService.isEmailWhitelisted(email)) {
            log.warn("Attempt to send verification code to non-whitelisted email: {}", email);
            return false;
        }
        
        // Verifică rate limiting (max 5 coduri per oră)
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        long recentCodes = verificationCodeRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);
        
        if (recentCodes >= MAX_CODES_PER_HOUR) {
            log.warn("Rate limit exceeded for user: {} (email: {})", user.getUsername(), email);
            return false;
        }
        
        // Generează cod de 6 cifre
        String code = generateCode();
        
        // Creează entitatea
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCode(code);
        verificationCode.setEmail(email);
        verificationCode.setExpiresAt(Instant.now().plus(Duration.ofMinutes(CODE_EXPIRY_MINUTES)));
        verificationCode.setUsed(false);
        verificationCode.setRequestIp(requestIp);
        
        verificationCodeRepository.save(verificationCode);
        
        // Trimite emailul
        boolean sent = sendVerificationEmail(email, code, user.getUsername());
        
        if (sent) {
            log.info("Verification code sent to {} for user {}", email, user.getUsername());
        } else {
            log.error("Failed to send verification code to {} for user {}", email, user.getUsername());
        }
        
        return sent;
    }
    
    /**
     * Verifică un cod de verificare
     */
    @Transactional
    public boolean verifyCode(User user, String code) {
        Optional<EmailVerificationCode> verificationOpt = verificationCodeRepository
            .findByUserAndCodeAndUsedFalseAndExpiresAtAfter(user, code, Instant.now());
        
        if (verificationOpt.isEmpty()) {
            log.warn("Invalid or expired verification code for user: {}", user.getUsername());
            return false;
        }
        
        EmailVerificationCode verification = verificationOpt.get();
        verification.setUsed(true);
        verification.setUsedAt(Instant.now());
        verificationCodeRepository.save(verification);
        
        log.info("Verification code validated for user: {}", user.getUsername());
        return true;
    }
    
    /**
     * Generează un cod random de 6 cifre
     */
    private String generateCode() {
        int code = RANDOM.nextInt(900000) + 100000; // Între 100000 și 999999
        return String.valueOf(code);
    }
    
    /**
     * Trimite emailul cu codul de verificare
     */
    private boolean sendVerificationEmail(String email, String code, String username) {
        EmailData emailData = new EmailData();
        emailData.setRecipients(new String[]{email});
        emailData.setSubject("[SECURITATE] Cod de verificare pentru autentificare");
        
        String message = String.format(
            "Bună %s,\n\n" +
            "Ai solicitat autentificare cu cod de verificare prin email.\n\n" +
            "Codul tău de verificare este: %s\n\n" +
            "Acest cod expiră în %d minute.\n\n" +
            "Dacă nu ai solicitat acest cod, te rugăm să ignori acest email.\n\n" +
            "---\n" +
            "Acest email a fost generat automat de sistem.",
            username, code, CODE_EXPIRY_MINUTES
        );
        
        emailData.setMessage(message);
        
        EEmailSendStatus status = emailHelper.createAndSendMail(emailData);
        return status == EEmailSendStatus.OK;
    }
}

