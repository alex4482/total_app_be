package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.runtime_errors.NotFoundException;
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
    public ResponseEntity<ApiResponse<Tenant>> getTenant(@PathVariable Long id)
    {
        if (id == null)
        {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid tenant ID"));
        }
        Tenant t = tenantService.getTenant(id);
        if (t == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Tenant not found with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(t));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Tenant>> addTenant(@RequestBody CreateTenantDto tenant)
    {
        if (tenant == null)
        {
            throw new ValidationException("Body is null when creating new tenant");
        }

        Tenant t = tenantService.addTenant(tenant);
        if (t == null)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create tenant"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tenant created successfully", t));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Tenant>> updateTenant(@PathVariable Long id, @RequestBody CreateTenantDto tenant)
    {
        if (tenant == null)
        {
            throw new ValidationException("Body is null when updating tenant");
        }

        Tenant t = tenantService.updateTenantDetails(id, tenant);
        if (t == null)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update tenant"));
        }
        return ResponseEntity.ok(ApiResponse.success("Tenant updated successfully", t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable Long id)
    {
        // Validate input early
        if (id == null)
        {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid tenant ID"));
        }
        try
        {
            tenantService.deleteTenant(id);
            return ResponseEntity.ok(ApiResponse.success("Tenant deleted successfully", null));
        }
        catch (NotFoundException e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
        catch (org.springframework.dao.DataIntegrityViolationException e)
        {
            log.warn("Delete tenant {} failed due to referential integrity: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Cannot delete tenant: it is still referenced by other records"));
        }
        catch (Exception e)
        {
            log.error("Unexpected error deleting tenant {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred while deleting tenant"));
        }
    }

    @PostMapping("/new-rental-agreement")
    public ResponseEntity<ApiResponse<TenantRentalData>> TenantRentSpace(@RequestBody TenantRentalDto dto)
    {
        TenantRentalData trd = tenantRentalService.startNewRent(dto);
        if (trd != null)
        {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Rental agreement created successfully", trd));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create rental agreement"));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<TenantImportResultDto>> importTenants(@RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            log.warn("Import tenants called with empty file");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No file provided or file is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")))
        {
            log.warn("Import tenants called with non-Excel file: {}", filename);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid file type. Only .xlsx and .xls files are allowed"));
        }

        try
        {
            TenantImportResultDto result = tenantImportService.importTenantsFromExcel(file);
            log.info("Tenant import completed: {} saved, {} skipped out of {} total rows",
                result.getSavedCount(), result.getSkippedCount(), result.getTotalRows());
            return ResponseEntity.ok(ApiResponse.success("Import completed successfully", result));
        }
        catch (Exception e)
        {
            log.error("Error importing tenants from Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to import tenants: " + e.getMessage()));
        }
    }

    @DeleteMapping("/bulk-delete")
    public ResponseEntity<ApiResponse<TenantBulkDeleteResultDto>> bulkDeleteTenants(@RequestBody List<Long> tenantIds)
    {
        if (tenantIds == null || tenantIds.isEmpty())
        {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No tenant IDs provided"));
        }

        try
        {
            TenantBulkDeleteResultDto result = tenantService.bulkDeleteTenants(tenantIds);
            return ResponseEntity.ok(ApiResponse.success("Bulk delete completed", result));
        }
        catch (Exception e)
        {
            log.error("Error during bulk delete of tenants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete tenants: " + e.getMessage()));
        }
    }

}
