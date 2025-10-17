package com.work.total_app.services;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.building.RentalSpaceFilter;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.CreateTenantDto;
import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.repositories.RentalSpaceRepository;
import com.work.total_app.repositories.TenantRentalDataRepository;
import com.work.total_app.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private FileDeletionService fileDeletionService;

    public Tenant addTenant(CreateTenantDto tenantDto) {
        Tenant tenant = new Tenant();
        tenant.getDataFromDto(tenantDto);

        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(Long id) {
        if (!tenantRepository.existsById(id))
        {
            throw new NotFoundException(
                    String.format("Cant delete tenant, if tenant with id %d doesnt exist!", id));
        }

        // Delete all files associated with this tenant
        fileDeletionService.deleteAllFilesForTenant(id);

        // Delete the tenant
        tenantRepository.deleteById(id);
    }

    public Tenant getTenant(Long id) {
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

    public Tenant updateTenantDetails(Long id, CreateTenantDto updatedTenantDto) {
        if (id == null)
        {
            throw new ValidationException("Given updated tenant has no id");
        }
        Tenant oldTenant = tenantRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Cant update tenant, if tenant doesnt exist?"));

        oldTenant.getDataFromDto(updatedTenantDto);

        return tenantRepository.save(oldTenant);
    }
}
