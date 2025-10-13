package com.work.total_app.repositories;

import com.work.total_app.models.authentication.RefreshTokenState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<RefreshTokenState, String> {
    Optional<RefreshTokenState> findByTokenHash(String hash);

    // Find session by matching either current or previous refresh hash
    @Query("""
         select s from RefreshTokenState s
          where s.tokenHash = :hash or s.previousTokenHash = :hash
         """)
    Optional<RefreshTokenState> findByAnyHash(@Param("hash") String hash);

    // Lightweight fetch for revokedAfter only (used in request filter)
    @Query("select s.revokedAfter from RefreshTokenState s where s.sessionId = :sid")
    Optional<Instant> findRevokedAfter(@Param("sid") String sessionId);
}
