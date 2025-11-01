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
    
    /**
     * IMAP server configuration for saving sent emails
     */
    private Imap imap = new Imap();
    
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
    
    @Data
    public static class Imap {
        /**
         * IMAP server address (e.g., imap.gmail.com)
         */
        private String address;
        
        /**
         * IMAP server port (typically 993 for SSL)
         */
        private Integer port = 993;
        
        /**
         * Enable/disable IMAP sent folder saving
         */
        private boolean enabled = true;
        
        /**
         * Name of the Sent folder (default "Sent")
         */
        private String sentFolderName = "Sent";
    }
}


