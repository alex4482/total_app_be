package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.service.Service;
import com.work.total_app.models.service.ServiceDto;
import com.work.total_app.models.service.ServiceKeywordsResponse;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.services.ServiceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/services")
@Log4j2
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    /**
     * Get all active services.
     * GET /services
     */
    @GetMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<List<Service>>> getAllActiveServices() {
        try {
            List<Service> services = serviceService.getAllActiveServices();
            return ResponseEntity.ok(ApiResponse.success(services));
        } catch (Exception e) {
            log.error("Error getting active services", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get services: " + e.getMessage()));
        }
    }

    /**
     * Get all services (including inactive).
     * GET /services/all
     */
    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<Service>>> getAllServices() {
        try {
            List<Service> services = serviceService.getAllServices();
            return ResponseEntity.ok(ApiResponse.success(services));
        } catch (Exception e) {
            log.error("Error getting all services", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get services: " + e.getMessage()));
        }
    }

    /**
     * Get all service keywords for use in formulas.
     * GET /services/keywords
     * 
     * Returns service names transformed to keywords:
     * - Lowercase
     * - Spaces replaced with underscore
     * - Diacritics removed (ă -> a, î -> i, etc.)
     * 
     * Example:
     * - "Salubrizare" -> "salubrizare"
     * - "Cota întreținere" -> "cota_intretinere"
     * - "Serviciu de alarmă" -> "serviciu_de_alarma"
     * 
     * NOTE: This endpoint must be declared BEFORE /{id} to avoid route conflicts.
     */
    @GetMapping("/keywords")
    @ResponseBody
    public ResponseEntity<ApiResponse<ServiceKeywordsResponse>> getServiceKeywords() {
        try {
            ServiceKeywordsResponse response = serviceService.getServiceKeywords();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting service keywords", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get service keywords: " + e.getMessage()));
        }
    }

    /**
     * Get service by ID.
     * GET /services/{id}
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Service>> getServiceById(@PathVariable Long id) {
        try {
            Service service = serviceService.getServiceById(id);
            return ResponseEntity.ok(ApiResponse.success(service));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting service {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get service: " + e.getMessage()));
        }
    }

    /**
     * Create a new service.
     * POST /services
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<Service>> createService(@RequestBody ServiceDto dto) {
        try {
            Service service = serviceService.createService(dto);
            return ResponseEntity.status(201)
                .body(ApiResponse.success("Service created successfully", service));
        } catch (Exception e) {
            log.error("Error creating service", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to create service: " + e.getMessage()));
        }
    }

    /**
     * Update an existing service.
     * PUT /services/{id}
     */
    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Service>> updateService(
            @PathVariable Long id,
            @RequestBody ServiceDto dto) {
        try {
            Service service = serviceService.updateService(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Service updated successfully", service));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating service {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update service: " + e.getMessage()));
        }
    }

    /**
     * Delete a service (soft delete).
     * DELETE /services/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        try {
            serviceService.deleteService(id);
            return ResponseEntity.ok(ApiResponse.success("Service deleted successfully", null));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting service {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to delete service: " + e.getMessage()));
        }
    }
}

