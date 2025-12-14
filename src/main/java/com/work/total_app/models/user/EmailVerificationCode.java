package com.work.total_app.models.user;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_codes")
@Data
public class EmailVerificationCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 10)
    private String code;
    
    @Column(nullable = false, length = 255)
    private String email;
    
    @Column(nullable = false)
    private Instant expiresAt;
    
    @Column(nullable = false)
    private boolean used = false;
    
    @Column
    private Instant usedAt;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(length = 45)
    private String requestIp;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

