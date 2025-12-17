package com.work.total_app.services.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service pentru delay progresiv la login failures
 * Previne brute-force prin creșterea timpului de răspuns
 */
@Service
@Log4j2
public class LoginDelayService {
    
    // Store pentru tracking failed attempts per IP
    private final Map<String, Integer> failedAttemptsCache = new ConcurrentHashMap<>();
    
    // Cleanup cache entries older than 1 hour
    private final Map<String, Long> lastAttemptTime = new ConcurrentHashMap<>();
    
    private static final long CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(1);
    
    /**
     * Aplică delay progresiv bazat pe numărul de încercări eșuate
     * 
     * Formula: delay = 2^(attempts - 1) * 500ms
     * - 1st failure: 500ms
     * - 2nd failure: 1s
     * - 3rd failure: 2s
     * - 4th failure: 4s
     * - 5th failure: 8s
     * - 6+ failures: 10s (cap)
     */
    public void applyDelay(String ip) {
        cleanupExpiredEntries();
        
        int attempts = failedAttemptsCache.getOrDefault(ip, 0);
        
        if (attempts > 0) {
            // Calculate exponential delay with cap at 10 seconds
            long delayMs = Math.min(
                (long) Math.pow(2, attempts - 1) * 500,
                10000
            );
            
            log.info("Applying {}ms delay for IP {} after {} failed attempts", 
                delayMs, ip, attempts);
            
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Delay interrupted for IP: {}", ip);
            }
        }
    }
    
    /**
     * Înregistrează o încercare eșuată pentru un IP
     */
    public void recordFailedAttempt(String ip) {
        failedAttemptsCache.merge(ip, 1, Integer::sum);
        lastAttemptTime.put(ip, System.currentTimeMillis());
        
        int attempts = failedAttemptsCache.get(ip);
        log.info("Failed attempt recorded for IP {}. Total attempts: {}", ip, attempts);
    }
    
    /**
     * Resetează contorul pentru un IP (după login reușit)
     */
    public void resetAttempts(String ip) {
        if (failedAttemptsCache.remove(ip) != null) {
            lastAttemptTime.remove(ip);
            log.info("Reset failed attempts for IP: {}", ip);
        }
    }
    
    /**
     * Obține numărul de încercări eșuate pentru un IP
     */
    public int getFailedAttempts(String ip) {
        cleanupExpiredEntries();
        return failedAttemptsCache.getOrDefault(ip, 0);
    }
    
    /**
     * Curăță entries expirate (> 1 oră)
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        lastAttemptTime.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue()) > CACHE_EXPIRY_MS;
            if (expired) {
                failedAttemptsCache.remove(entry.getKey());
                log.debug("Cleaned up expired entry for IP: {}", entry.getKey());
            }
            return expired;
        });
    }
    
    /**
     * Calculează următorul delay pentru un IP fără a-l aplica
     */
    public long calculateNextDelay(String ip) {
        int attempts = failedAttemptsCache.getOrDefault(ip, 0);
        if (attempts == 0) return 0;
        
        return Math.min(
            (long) Math.pow(2, attempts - 1) * 500,
            10000
        );
    }
}
