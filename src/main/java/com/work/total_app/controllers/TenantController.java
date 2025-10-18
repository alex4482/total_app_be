package com.work.total_app.controllers;

import com.work.total_app.constants.AuthenticationConstants;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.CreateTenantDto;
import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.models.tenant.TenantRentalData;
import com.work.total_app.models.tenant.TenantRentalDto;
import com.work.total_app.services.TenantRentalService;
import com.work.total_app.services.TenantService;
import jakarta.annotation.Nullable;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/tenants")
@CrossOrigin(origins = {AuthenticationConstants.PROD_WEBSITE_URL, }, originPatterns = {AuthenticationConstants.LOCAL_WEBSITE_PATTERN, AuthenticationConstants.STAGING_WEBSITE_PATTERN})
@Log4j2
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRentalService tenantRentalService;

    @GetMapping
    public ResponseEntity<List<Tenant>> listTenants(@Nullable @RequestParam Boolean active,
                                                    @Nullable@RequestParam BuildingLocation buildingLocation,
                                                    @Nullable@RequestParam String buildingId,
                                                    @Nullable@RequestParam Boolean groundLevel)
    {
        List<Tenant> tenantList = tenantService.getTenants(active, buildingLocation, buildingId, groundLevel);
        HttpStatus status = HttpStatus.OK;
        if (tenantList.isEmpty())
        {
            status = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(tenantList, status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenant(@PathVariable Long id)
    {
        if (id == null)
        {
            return ResponseEntity.badRequest().build();
        }
        Tenant t = tenantService.getTenant(id);
        if (t == null)
        {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(t, HttpStatus.FOUND);
    }

    @PostMapping
    public ResponseEntity<Tenant> addTenant(@RequestBody CreateTenantDto tenant)
    {
        if (tenant == null)
        {
            throw new ValidationException("Body is null when creating new tenant");
        }

        try {
            Tenant t = tenantService.addTenant(tenant);
            if (t == null)
            {
                return ResponseEntity.internalServerError().build();
            }
            return new ResponseEntity<>(t, HttpStatus.CREATED);
        }
        catch (Exception e)
        {
            // TODO: log e
            throw e;
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable Long id, @RequestBody CreateTenantDto tenant)
    {
        if (tenant == null)
        {
            throw new ValidationException("Body is null when updating tenant");
        }

        try {
            Tenant t = tenantService.updateTenantDetails(id, tenant);
            if (t == null)
            {
                return ResponseEntity.internalServerError().build();
            }
            return new ResponseEntity<>(t, HttpStatus.OK);
        }
        catch (Exception e)
        {
            // TODO: log e
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id)
    {
        // Validate input early
        if (id == null)
        {
            return ResponseEntity.badRequest().build();
        }
        try
        {
            tenantService.deleteTenant(id);
            // 204 No Content is the conventional success status for DELETE
            return ResponseEntity.noContent().build();
        }
        catch (com.work.total_app.models.runtime_errors.NotFoundException e)
        {
            // Service signals that the tenant doesn't exist
            log.info("Delete tenant {} requested but not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        catch (org.springframework.dao.DataIntegrityViolationException e)
        {
            // Likely FK constraint: tenant still referenced by other records
            log.warn("Delete tenant {} failed due to referential integrity: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        catch (Exception e)
        {
            // Unexpected error path; log with stacktrace for diagnostics
            log.error("Unexpected error deleting tenant {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/new-rental-agreement")
    public ResponseEntity<TenantRentalData> TenantRentSpace(@RequestBody TenantRentalDto dto)
    {
        TenantRentalData trd = tenantRentalService.startNewRent(dto);
        if (trd != null)
        {
            return new ResponseEntity<>(trd, HttpStatus.CREATED);
        }

        return ResponseEntity.internalServerError().build();
    }

}
