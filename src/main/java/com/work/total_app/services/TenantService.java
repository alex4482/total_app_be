package com.work.total_app.services;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.building.RentalSpaceFilter;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.repositories.RentalSpaceRepository;
import com.work.total_app.repositories.TenantRentalDataRepository;
import com.work.total_app.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantRentalDataRepository tenantRentalRepository;

    @Autowired
    private RentalSpaceRepository spaceRepository;

    public Tenant addTenant(Tenant tenant) {
        if (tenant.getId() != null)
        {
            return tenantRepository.save(tenant);
        }

        tenant.setId(tenant.getName().toLowerCase(Locale.ROOT).replace(' ', '-'));
        if (tenantRepository.existsById(tenant.getId()))
        {
            throw new ValidationException("Tenant with id " + tenant.getId() + " already exists");
        }
        return tenantRepository.save(tenant);
    }

    public void deleteTenant(String id) {
        tenantRepository.deleteById(id);
    }

    public Tenant getTenant(String id) {
        return tenantRepository.findById(id).orElse(null);
    }

    public List<Tenant> getTenants(Boolean isActive,
                                   BuildingLocation buildingLocation,
                                   String buildingId,
                                   Boolean groundLevel) {

        if (isActive != null && !isActive)
        {
            return tenantRepository.findByActive(false);
        }

        if (buildingId != null || buildingLocation != null || groundLevel != null)
        {
            RentalSpaceFilter f = new RentalSpaceFilter(buildingId, buildingLocation, groundLevel, false, null);
            return spaceRepository.findAll(RentalSpace.byFilter(f))
                    .stream().map(r -> r.getRentalAgreement().getTenant()).toList();
        }

        if (isActive != null)
        {
            return tenantRepository.findByActive(true);
        }

        return tenantRepository.findAll();
    }
}
