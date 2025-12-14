package com.work.total_app.services.security;

import com.work.total_app.config.SecurityProperties;
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
    
    @Autowired
    private SecurityProperties securityProperties;
    @Autowired
    private SecurityProperties securityProperties;
    
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
                
                // Dacă a ajuns la maxFailedAttempts, marchează că trebuie verificare prin email
                if (user.getFailedLoginAttempts() >= securityProperties.getRateLimit().getMaxFailedAttempts()) {
                    user.setRequiresEmailVerification(true);
                    log.warn("User {} requires email verification after {} failed attempts", 
                        username, user.getFailedLoginAttempts());
                }
                
                // Dacă a ajuns la maxIpAttempts, blochează contul pentru lockDurationMinutes
                if (user.getFailedLoginAttempts() >= securityProperties.getRateLimit().getMaxIpAttempts()) {
                    user.setAccountLocked(true);
                    user.setAccountLockedUntil(Instant.now().plus(Duration.ofMinutes(
                        securityProperties.getRateLimit().getLockDurationMinutes())));
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
        Instant since = Instant.now().minus(Duration.ofMinutes(
            securityProperties.getRateLimit().getWindowMinutes()));
        long failedAttempts = loginAttemptRepository.countFailedAttemptsByIpSince(ip, since);
        
        boolean limited = failedAttempts >= securityProperties.getRateLimit().getMaxIpAttempts();
        
        if (limited) {
            log.warn("IP {} is rate limited with {} failed attempts in last {} minutes", 
                ip, failedAttempts, securityProperties.getRateLimit().getWindowMinutes());
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

