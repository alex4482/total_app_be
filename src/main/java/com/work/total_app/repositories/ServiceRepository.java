package com.work.total_app.repositories;

import com.work.total_app.models.service.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByActiveTrue();
    List<Service> findByActive(Boolean active);
    
    /**
     * Find service by name (case-insensitive).
     */
    java.util.Optional<Service> findByNameIgnoreCase(String name);
}

