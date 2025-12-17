package com.work.total_app.services.security;

import com.work.total_app.repositories.LoginAttemptRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service pentru blacklisting IP-uri suspicioase
 * Blochează temporar IP-uri cu comportament de atac persistent
 */
@Service
@Log4j2
public class IpBlacklistService {
    
    @Autowired
    private LoginAttemptRepository loginAttemptRepository;
    
    // Cache pentru IP-uri blacklisted temporar
    private final Map<String, Instant> blacklistedIps = new ConcurrentHashMap<>();
    
    // Threshold-uri
    private static final int BLACKLIST_THRESHOLD = 20; // 20 failed attempts in window
    private static final int BLACKLIST_WINDOW_MINUTES = 30;
    private static final int BLACKLIST_DURATION_MINUTES = 60; // Ban for 1 hour
    
    /**
     * Verifică dacă un IP este blacklisted
     */
    public boolean isBlacklisted(String ip) {
        Instant unblockTime = blacklistedIps.get(ip);
        
        if (unblockTime == null) {
            return false;
        }
        
        // Check if blacklist period expired
        if (Instant.now().isAfter(unblockTime)) {
            blacklistedIps.remove(ip);
            log.info("IP {} removed from blacklist (time expired)", ip);
            return false;
        }
        
        long minutesLeft = Duration.between(Instant.now(), unblockTime).toMinutes();
        log.warn("IP {} is blacklisted. Will be unblocked in {} minutes", ip, minutesLeft);
        return true;
    }
    
    /**
     * Verifică și eventual blacklist-uiește un IP bazat pe comportament
     */
    public void checkAndBlacklistIfNeeded(String ip) {
        // Check recent failed attempts
        Instant since = Instant.now().minus(Duration.ofMinutes(BLACKLIST_WINDOW_MINUTES));
        long recentFailures = loginAttemptRepository.countFailedAttemptsByIpSince(ip, since);
        
        if (recentFailures >= BLACKLIST_THRESHOLD && !blacklistedIps.containsKey(ip)) {
            blacklistIp(ip);
        }
    }
    
    /**
     * Blacklist un IP pentru o perioadă de timp
     */
    public void blacklistIp(String ip) {
        Instant unblockTime = Instant.now().plus(Duration.ofMinutes(BLACKLIST_DURATION_MINUTES));
        blacklistedIps.put(ip, unblockTime);
        
        log.warn("SECURITY ALERT: IP {} has been blacklisted until {} due to excessive failed login attempts", 
            ip, unblockTime);
    }
    
    /**
     * Remove manual un IP din blacklist (admin action)
     */
    public boolean removeFromBlacklist(String ip) {
        boolean wasBlacklisted = blacklistedIps.remove(ip) != null;
        if (wasBlacklisted) {
            log.info("IP {} manually removed from blacklist", ip);
        }
        return wasBlacklisted;
    }
    
    /**
     * Obține timpul rămas până la unblock pentru un IP
     */
    public long getMinutesUntilUnblock(String ip) {
        Instant unblockTime = blacklistedIps.get(ip);
        if (unblockTime == null) {
            return 0;
        }
        
        return Math.max(0, Duration.between(Instant.now(), unblockTime).toMinutes());
    }
    
    /**
     * Obține toate IP-urile blacklisted
     */
    public Map<String, Instant> getAllBlacklistedIps() {
        // Cleanup expired entries
        blacklistedIps.entrySet().removeIf(entry -> 
            Instant.now().isAfter(entry.getValue()));
        
        return Map.copyOf(blacklistedIps);
    }
    
    /**
     * Curăță blacklist-ul de IP-uri expirate
     */
    public void cleanupExpiredEntries() {
        int removed = 0;
        var iterator = blacklistedIps.entrySet().iterator();
        
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (Instant.now().isAfter(entry.getValue())) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            log.info("Cleaned up {} expired IP blacklist entries", removed);
        }
    }
}
