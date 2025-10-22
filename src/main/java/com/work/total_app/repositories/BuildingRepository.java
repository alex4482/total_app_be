package com.work.total_app.repositories;

import com.work.total_app.models.building.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {
    boolean existsByName(String name);
    Building findByName(String name);
}
