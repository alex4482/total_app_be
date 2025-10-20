package com.work.total_app.repositories;

import com.work.total_app.models.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    List<Tenant> findByActive(boolean active);

    boolean existsByName(String name);

    Optional<Tenant> findByName(String name);
}
