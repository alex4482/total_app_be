package com.work.total_app.repositories;

import com.work.total_app.models.tenant.TenantRentalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRentalDataRepository extends JpaRepository<TenantRentalData, Long> {
}
