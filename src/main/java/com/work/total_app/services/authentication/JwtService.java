package com.work.total_app.services.authentication;

import com.work.total_app.models.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {
    private final SecretKey key;
    private final long accessTtlMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-minutes:15}") long accessTtlMinutes
    ) {
        if (secret == null || secret.length() < 32)
            throw new IllegalStateException("app.jwt.secret must be at least 32 chars");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public String issueAccess(String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("anon")
                .claim("sid", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(accessTtlMinutes))))
                .signWith(key)
                .compact();
    }
    
    /**
     * Issues an access token with role-based expiration.
     * 
     * @param sessionId The session ID
     * @param role The user role (determines expiration time)
     * @return JWT access token
     */
    public String issueAccessWithRole(String sessionId, UserRole role) {
        Instant now = Instant.now();
        
        // Get role-specific expiration time
        long expirationMinutes = role.getSessionTimeoutMinutes();
        
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("user")
                .claim("sid", sessionId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String jwt) {
        return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(jwt);  // throws on bad sig/expired/etc
    }
}
