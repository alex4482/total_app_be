package com.work.total_app.services.security;

import com.work.total_app.helpers.EmailHelper;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.user.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Email verification service for hard delete operations.
 * 
 * <p>Security features:
 * <ul>
 *   <li>Sends verification code to admin's email</li>
 *   <li>Code expires after 15 minutes</li>
 *   <li>Rate limiting: 5 failed attempts → block 30 minutes</li>
 *   <li>Per-user tracking (prevents distributed attacks)</li>
 * </ul>
 */
@Service
@Log4j2
public class HardDeleteVerificationService {
    
    @Autowired
    private EmailHelper emailHelper;
    
    @Value("${app.security.hard-delete.verification-code-expiry-minutes:15}")
    private int codeExpiryMinutes;
    
    @Value("${app.security.hard-delete.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${app.security.hard-delete.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    private final SecureRandom random = new SecureRandom();
    
    // Store verification codes: userId -> VerificationData
    private final ConcurrentMap<UUID, VerificationData> verificationCodes = new ConcurrentHashMap<>();
    
    // Store failed attempts: userId -> FailedAttemptsData
    private final ConcurrentMap<UUID, FailedAttemptsData> failedAttempts = new ConcurrentHashMap<>();
    
    /**
     * Initiates hard delete verification by sending a code to the admin's email.
     * 
     * @param admin The admin user requesting the hard delete
     * @param operationDescription Description of what will be deleted
     * @return Verification session ID
     * @throws SecurityException if user is rate limited
     */
    public String initiateVerification(User admin, String operationDescription) throws SecurityException {
        // Check if user is locked out
        if (isLockedOut(admin.getId())) {
            FailedAttemptsData attempts = failedAttempts.get(admin.getId());
            long remainingMinutes = (attempts.lockedUntil.toEpochMilli() - System.currentTimeMillis()) / 60000;
            throw new SecurityException(
                String.format("Too many failed verification attempts. Locked for %d more minutes.", remainingMinutes)
            );
        }
        
        // Generate 6-digit verification code
        String code = String.format("%06d", random.nextInt(1000000));
        String sessionId = UUID.randomUUID().toString();
        
        // Store verification data
        VerificationData data = new VerificationData();
        data.sessionId = sessionId;
        data.code = code;
        data.expiresAt = Instant.now().plusSeconds(codeExpiryMinutes * 60L);
        data.operationDescription = operationDescription;
        verificationCodes.put(admin.getId(), data);
        
        // Send email
        try {
            EmailData emailData = new EmailData();
            emailData.setRecipients(new String[]{admin.getEmail()});
            emailData.setSubject("[CRITICAL] Hard Delete Verification Required");
            emailData.setMessage(String.format(
                "A hard delete operation has been requested:\n\n" +
                "%s\n\n" +
                "Verification code: %s\n\n" +
                "This code expires in %d minutes.\n\n" +
                "If you did not request this operation, change your password immediately and contact support.",
                operationDescription,
                code,
                codeExpiryMinutes
            ));
            
            emailHelper.createAndSendMail(emailData);
            
            log.info("Hard delete verification code sent to admin {}", admin.getUsername());
        } catch (Exception e) {
            log.error("Failed to send hard delete verification email to {}", admin.getEmail(), e);
            throw new SecurityException("Failed to send verification email. Please try again.");
        }
        
        return sessionId;
    }
    
    /**
     * Verifies the code entered by the admin.
     * 
     * @param admin The admin user
     * @param sessionId The verification session ID
     * @param code The verification code entered by user
     * @return true if verified successfully
     * @throws SecurityException if verification fails
     */
    public boolean verifyCode(User admin, String sessionId, String code) throws SecurityException {
        // Check if locked out
        if (isLockedOut(admin.getId())) {
            FailedAttemptsData attempts = failedAttempts.get(admin.getId());
            long remainingMinutes = (attempts.lockedUntil.toEpochMilli() - System.currentTimeMillis()) / 60000;
            throw new SecurityException(
                String.format("Account locked. Try again in %d minutes.", remainingMinutes)
            );
        }
        
        // Get verification data
        VerificationData data = verificationCodes.get(admin.getId());
        if (data == null || !data.sessionId.equals(sessionId)) {
            throw new SecurityException("Invalid or expired verification session");
        }
        
        // Check if code expired
        if (Instant.now().isAfter(data.expiresAt)) {
            verificationCodes.remove(admin.getId());
            throw new SecurityException("Verification code expired. Please request a new code.");
        }
        
        // Verify code
        if (!data.code.equals(code)) {
            recordFailedAttempt(admin.getId());
            
            int remaining = maxFailedAttempts - getFailedAttemptCount(admin.getId());
            if (remaining > 0) {
                throw new SecurityException(
                    String.format("Invalid verification code. %d attempts remaining.", remaining)
                );
            } else {
                throw new SecurityException(
                    String.format("Too many failed attempts. Account locked for %d minutes.", lockoutDurationMinutes)
                );
            }
        }
        
        // Success - cleanup
        verificationCodes.remove(admin.getId());
        failedAttempts.remove(admin.getId());
        
        log.info("Hard delete verification successful for admin {}", admin.getUsername());
        return true;
    }
    
    /**
     * Cancels an active verification session.
     */
    public void cancelVerification(UUID userId) {
        verificationCodes.remove(userId);
    }
    
    /**
     * Checks if user is currently locked out.
     */
    private boolean isLockedOut(UUID userId) {
        FailedAttemptsData attempts = failedAttempts.get(userId);
        if (attempts == null || attempts.lockedUntil == null) {
            return false;
        }
        
        if (Instant.now().isAfter(attempts.lockedUntil)) {
            // Lockout expired - cleanup
            failedAttempts.remove(userId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Records a failed verification attempt.
     */
    private void recordFailedAttempt(UUID userId) {
        FailedAttemptsData attempts = failedAttempts.computeIfAbsent(userId, k -> new FailedAttemptsData());
        attempts.count++;
        attempts.lastAttemptAt = Instant.now();
        
        if (attempts.count >= maxFailedAttempts) {
            attempts.lockedUntil = Instant.now().plusSeconds(lockoutDurationMinutes * 60L);
            log.warn("User {} locked out due to {} failed hard delete verification attempts", 
                userId, attempts.count);
        }
    }
    
    /**
     * Gets the number of failed attempts for a user.
     */
    private int getFailedAttemptCount(UUID userId) {
        FailedAttemptsData attempts = failedAttempts.get(userId);
        return attempts != null ? attempts.count : 0;
    }
    
    /**
     * Verification data stored per session.
     */
    private static class VerificationData {
        String sessionId;
        String code;
        Instant expiresAt;
        String operationDescription;
    }
    
    /**
     * Failed attempts tracking data.
     */
    private static class FailedAttemptsData {
        int count = 0;
        Instant lastAttemptAt;
        Instant lockedUntil;
    }
}
