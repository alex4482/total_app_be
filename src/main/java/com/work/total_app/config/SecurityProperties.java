package com.work.total_app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityProperties {
    
    private CookieProperties cookies = new CookieProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();
    private EmailVerificationProperties emailVerification = new EmailVerificationProperties();
    
    @Data
    public static class CookieProperties {
        private boolean secure = true;
        private String sameSite = "Strict";
    }
    
    @Data
    public static class RateLimitProperties {
        private int maxFailedAttempts = 6;
        private int maxIpAttempts = 10;
        private int lockDurationMinutes = 30;
        private int windowMinutes = 15;
    }
    
    @Data
    public static class EmailVerificationProperties {
        private int codeExpiryMinutes = 15;
        private int maxCodesPerHour = 5;
    }
}

