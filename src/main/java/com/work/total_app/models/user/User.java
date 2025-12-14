package com.work.total_app.models.user;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String username;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Column(length = 255)
    private String email;
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    @Column(nullable = false)
    private boolean accountLocked = false;
    
    @Column(nullable = false)
    private int failedLoginAttempts = 0;
    
    @Column
    private Instant lastFailedLoginAt;
    
    @Column
    private Instant accountLockedUntil;
    
    @Column
    private Instant lastSuccessfulLoginAt;
    
    @Column
    private String lastLoginIp;
    
    @Column(nullable = false)
    private boolean requiresEmailVerification = false;
    
    @Column
    private Instant createdAt;
    
    @Column
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

