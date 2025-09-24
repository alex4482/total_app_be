package com.work.total_app.repositories;

import com.work.total_app.models.email.EmailFileKeywordPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailFileKeywordPairRepository extends JpaRepository<EmailFileKeywordPair, Integer> {
}
