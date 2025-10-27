package com.work.total_app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for application authentication.
 * Maps properties from application.properties with prefix "app.auth".
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {
    
    /**
     * BCrypt hash of the universal password for authentication.
     * Can be overridden via UNIVERSAL_PASSWORD_HASH environment variable.
     */
    private String universalPasswordHash;
}


