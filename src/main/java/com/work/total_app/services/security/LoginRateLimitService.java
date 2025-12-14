package com.work.total_app.services.security;

import com.work.total_app.models.user.LoginAttempt;
import com.work.total_app.models.user.User;
import com.work.total_app.repositories.LoginAttemptRepository;
import com.work.total_app.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Log4j2
public class LoginRateLimitService {
    
    @Autowired
    private LoginAttemptRepository loginAttemptRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private static final int MAX_FAILED_ATTEMPTS_FOR_EMAIL_VERIFICATION = 6;
    private static final int MAX_FAILED_ATTEMPTS_BY_IP = 10;
    private static final int ACCOUNT_LOCK_MINUTES = 30;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;
    
    /**
     * Înregistrează o încercare de login
     */
    @Transactional
    public void recordLoginAttempt(String username, String ip, String userAgent, boolean successful, String failureReason) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(ip);
        attempt.setUserAgent(userAgent);
        attempt.setSuccessful(successful);
        attempt.setFailureReason(failureReason);
        
        loginAttemptRepository.save(attempt);
        
        // Dacă login-ul a eșuat, actualizează contorul pe user
        if (!successful && username != null) {
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                user.setLastFailedLoginAt(Instant.now());
                
                // Dacă a ajuns la 6 încercări eșuate, marchează că trebuie verificare prin email
                if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS_FOR_EMAIL_VERIFICATION) {
                    user.setRequiresEmailVerification(true);
                    log.warn("User {} requires email verification after {} failed attempts", 
                        username, user.getFailedLoginAttempts());
                }
                
                // Dacă a ajuns la 10 încercări eșuate, blochează contul pentru 30 minute
                if (user.getFailedLoginAttempts() >= 10) {
                    user.setAccountLocked(true);
                    user.setAccountLockedUntil(Instant.now().plus(Duration.ofMinutes(ACCOUNT_LOCK_MINUTES)));
                    log.warn("User {} account locked until {} after {} failed attempts", 
                        username, user.getAccountLockedUntil(), user.getFailedLoginAttempts());
                }
                
                userRepository.save(user);
            });
        }
        
        // Dacă login-ul a reușit, resetează contoarele
        if (successful && username != null) {
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setFailedLoginAttempts(0);
                user.setLastFailedLoginAt(null);
                user.setRequiresEmailVerification(false);
                user.setAccountLocked(false);
                user.setAccountLockedUntil(null);
                user.setLastSuccessfulLoginAt(Instant.now());
                user.setLastLoginIp(ip);
                userRepository.save(user);
            });
        }
    }
    
    /**
     * Verifică dacă un IP a depășit limita de încercări eșuate
     */
    public boolean isIpRateLimited(String ip) {
        Instant since = Instant.now().minus(Duration.ofMinutes(RATE_LIMIT_WINDOW_MINUTES));
        long failedAttempts = loginAttemptRepository.countFailedAttemptsByIpSince(ip, since);
        
        boolean limited = failedAttempts >= MAX_FAILED_ATTEMPTS_BY_IP;
        
        if (limited) {
            log.warn("IP {} is rate limited with {} failed attempts in last {} minutes", 
                ip, failedAttempts, RATE_LIMIT_WINDOW_MINUTES);
        }
        
        return limited;
    }
    
    /**
     * Verifică dacă un user necesită verificare prin email
     */
    public boolean requiresEmailVerification(User user) {
        // Verifică dacă contul este blocat temporar
        if (user.isAccountLocked() && user.getAccountLockedUntil() != null) {
            if (Instant.now().isBefore(user.getAccountLockedUntil())) {
                return true; // Încă blocat
            } else {
                // Deblocare automată
                user.setAccountLocked(false);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
            }
        }
        
        return user.isRequiresEmailVerification();
    }
    
    /**
     * Verifică dacă contul este blocat
     */
    public boolean isAccountLocked(User user) {
        if (user.isAccountLocked() && user.getAccountLockedUntil() != null) {
            if (Instant.now().isBefore(user.getAccountLockedUntil())) {
                return true; // Încă blocat
            } else {
                // Deblocare automată
                user.setAccountLocked(false);
                user.setAccountLockedUntil(null);
                user.setRequiresEmailVerification(false);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                return false;
            }
        }
        return false;
    }
    
    /**
     * Resetează manual flag-ul de verificare email pentru un user
     */
    @Transactional
    public void resetEmailVerificationRequirement(User user) {
        user.setRequiresEmailVerification(false);
        user.setFailedLoginAttempts(0);
        user.setLastFailedLoginAt(null);
        userRepository.save(user);
        log.info("Email verification requirement reset for user: {}", user.getUsername());
    }
}

