package com.work.total_app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for email sending.
 * Maps properties from application.properties with prefix "emailer".
 */
@Data
@Component
@ConfigurationProperties(prefix = "emailer")
public class EmailerProperties {
    
    /**
     * Email address to use as sender (FROM field)
     */
    private String from;
    
    /**
     * Password or app password for email authentication
     */
    private String password;
    
    /**
     * SMTP server configuration
     */
    private Server server = new Server();
    
    @Data
    public static class Server {
        /**
         * SMTP server address (e.g., smtp.gmail.com)
         */
        private String address;
        
        /**
         * SMTP server port (typically 587 for TLS or 465 for SSL)
         */
        private Integer port;
    }
}


