package com.work.total_app.repositories;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.reading.IndexCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalSpaceRepository extends JpaRepository<RentalSpace, String>, JpaSpecificationExecutor<RentalSpace> {
    List<RentalSpace> findByBuildingId(String bid);
}
