package com.work.total_app.repositories;

import com.work.total_app.models.user.EmailVerificationCode;
import com.work.total_app.models.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {
    
    Optional<EmailVerificationCode> findByUserAndCodeAndUsedFalseAndExpiresAtAfter(
        User user, String code, Instant now
    );
    
    @Query("SELECT COUNT(e) FROM EmailVerificationCode e WHERE e.user = :user AND e.createdAt > :since")
    long countByUserAndCreatedAtAfter(User user, Instant since);
}

