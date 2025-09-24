package com.work.total_app.repositories;

import com.work.total_app.models.reading.IndexData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexDataRepository extends JpaRepository<IndexData, Long> {
}
