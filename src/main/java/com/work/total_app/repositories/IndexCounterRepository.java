package com.work.total_app.repositories;

import com.work.total_app.models.reading.IndexCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexCounterRepository
        extends JpaRepository<IndexCounter, Long>, JpaSpecificationExecutor<IndexCounter> {}

