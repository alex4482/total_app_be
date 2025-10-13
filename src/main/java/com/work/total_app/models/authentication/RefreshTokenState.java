package com.work.total_app.models.authentication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "refresh_token_state",
        indexes = {
                @Index(name="idx_rts_token_hash", columnList="tokenHash", unique = true),
                @Index(name="idx_rts_prev_hash", columnList="previousTokenHash")
        })
@Getter
@Setter
public class RefreshTokenState {

    @Id
    @Column(length = 36)
    private String sessionId;                 // UUID per login/device

    @Column(nullable=false, length=64)
    private String tokenHash;                 // current refresh hash (sha256 hex)

    @Column(length=64)
    private String previousTokenHash;         // last refresh hash (for idempotent retry)

    @Column(nullable=false)
    private Instant expiresAt;                // refresh absolute expiry

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Column(nullable=false)
    private Instant revokedAfter = Instant.EPOCH; // access cutoff (iat must be > this)

    @Version
    private long version;                     // optimistic locking on refresh

    // optional metadata
    @Column(length=45)   private String ip;
    @Column(length=200)  private String userAgent;
}
