package com.work.total_app.repositories;

import com.work.total_app.models.building.RentalSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalSpaceRepository extends JpaRepository<RentalSpace, Long>, JpaSpecificationExecutor<RentalSpace> {
    List<RentalSpace> findByBuildingId(Long bid);
}
