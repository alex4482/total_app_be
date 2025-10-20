package com.work.total_app.controllers;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.CreateTenantDto;
import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.models.tenant.TenantRentalData;
import com.work.total_app.models.tenant.TenantRentalDto;
import com.work.total_app.models.tenant.TenantImportResultDto;
import com.work.total_app.models.tenant.TenantBulkDeleteResultDto;
import com.work.total_app.services.TenantRentalService;
import com.work.total_app.services.TenantService;
import com.work.total_app.services.TenantImportService;
import jakarta.annotation.Nullable;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/tenants")
@Log4j2
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRentalService tenantRentalService;

    @Autowired
    private TenantImportService tenantImportService;

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

    @PostMapping("/import")
    public ResponseEntity<TenantImportResultDto> importTenants(@RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            log.warn("Import tenants called with empty file");
            return ResponseEntity.badRequest().build();
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")))
        {
            log.warn("Import tenants called with non-Excel file: {}", filename);
            return ResponseEntity.badRequest().build();
        }

        try
        {
            TenantImportResultDto result = tenantImportService.importTenantsFromExcel(file);
            log.info("Tenant import completed: {} saved, {} skipped out of {} total rows",
                result.getSavedCount(), result.getSkippedCount(), result.getTotalRows());
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("Error importing tenants from Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/bulk-delete")
    public ResponseEntity<TenantBulkDeleteResultDto> bulkDeleteTenants(@RequestBody List<Long> tenantIds)
    {
        if (tenantIds == null || tenantIds.isEmpty())
        {
            return ResponseEntity.badRequest().build();
        }

        try
        {
            TenantBulkDeleteResultDto result = tenantService.bulkDeleteTenants(tenantIds);
            return ResponseEntity.ok(result);
        }
        catch (Exception e)
        {
            log.error("Error during bulk delete of tenants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
