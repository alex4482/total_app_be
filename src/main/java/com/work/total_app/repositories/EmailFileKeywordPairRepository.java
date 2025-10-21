package com.work.total_app.repositories;

import com.work.total_app.models.email.EmailPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailFileKeywordPairRepository extends JpaRepository<EmailPreset, Integer> {
    Optional<EmailPreset> findByName(String name);
}
