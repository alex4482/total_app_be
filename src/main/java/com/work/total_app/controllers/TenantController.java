package com.work.total_app.controllers;

import com.work.total_app.constants.AuthenticationConstants;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.models.tenant.TenantRentalData;
import com.work.total_app.models.tenant.TenantRentalDto;
import com.work.total_app.services.TenantRentalService;
import com.work.total_app.services.TenantService;
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
    public ResponseEntity<List<Tenant>> listTenants(@RequestParam Boolean isActive,
                                                    @RequestParam BuildingLocation buildingLocation,
                                                    @RequestParam String buildingId,
                                                    @RequestParam Boolean groundLevel)
    {
        List<Tenant> TenantList = tenantService.getTenants(isActive, buildingLocation, buildingId, groundLevel);
        HttpStatus status = HttpStatus.OK;
        if (TenantList.isEmpty())
        {
            status = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(TenantList, status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenant(@PathVariable String id)
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
    public ResponseEntity<Tenant> addTenant(@RequestBody Tenant Tenant)
    {
        if (Tenant == null)
        {
            throw new ValidationException("Body is null when creating new tenant");
        }

        try {
            Tenant t = tenantService.addTenant(Tenant);
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

    @DeleteMapping("/{id}")
    public void deleteTenant(@PathVariable String id)
    {
        tenantService.deleteTenant(id);
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
