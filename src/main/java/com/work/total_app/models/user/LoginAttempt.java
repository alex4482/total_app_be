package com.work.total_app.models.user;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_ip_timestamp", columnList = "ip_address,created_at"),
    @Index(name = "idx_username_timestamp", columnList = "username,created_at")
})
@Data
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(length = 100)
    private String username;
    
    @Column(nullable = false, length = 45)
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    @Column(nullable = false)
    private boolean successful;
    
    @Column(length = 100)
    private String failureReason;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

