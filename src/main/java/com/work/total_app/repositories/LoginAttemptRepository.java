package com.work.total_app.repositories;

import com.work.total_app.models.user.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.ipAddress = :ip AND l.createdAt > :since AND l.successful = false")
    long countFailedAttemptsByIpSince(String ip, Instant since);
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.username = :username AND l.createdAt > :since AND l.successful = false")
    long countFailedAttemptsByUsernameSince(String username, Instant since);
}

