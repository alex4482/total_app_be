package com.work.total_app.repositories;

import com.work.total_app.models.user.EmailWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailWhitelistRepository extends JpaRepository<EmailWhitelist, UUID> {
    
    Optional<EmailWhitelist> findByEmailAndActiveTrue(String email);
    
    boolean existsByEmailAndActiveTrue(String email);
}

