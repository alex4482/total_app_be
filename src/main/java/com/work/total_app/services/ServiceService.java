package com.work.total_app.services;

import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.service.Service;
import com.work.total_app.models.service.ServiceDto;
import com.work.total_app.models.service.ServiceFormula;
import com.work.total_app.models.service.ServiceKeyword;
import com.work.total_app.models.service.ServiceKeywordsResponse;
import com.work.total_app.repositories.ServiceRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Log4j2
public class ServiceService {

    @Autowired
    private ServiceRepository serviceRepository;

    /**
     * Get all active services.
     */
    public List<Service> getAllActiveServices() {
        return serviceRepository.findByActiveTrue();
    }

    /**
     * Get all services (including inactive).
     */
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    /**
     * Get service by ID.
     */
    public Service getServiceById(Long id) {
        return serviceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Service not found with id: " + id));
    }

    /**
     * Create a new service.
     * If a service with the same name exists but is inactive, it will be reactivated and updated.
     */
    @Transactional
    public Service createService(ServiceDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ValidationException("Service name is required");
        }

        String serviceName = dto.getName().trim();
        
        // Check if service with same name already exists
        java.util.Optional<Service> existingService = serviceRepository.findByNameIgnoreCase(serviceName);
        
        if (existingService.isPresent()) {
            Service service = existingService.get();
            
            // If service is active, throw error
            if (service.getActive() != null && service.getActive()) {
                throw new ValidationException("Service with name '" + serviceName + "' already exists");
            }
            
            // If service is inactive, reactivate it and update with new data
            log.info("Reactivating inactive service: {}", serviceName);
            service.setActive(dto.getActive() != null ? dto.getActive() : true);
            service.setDescription(dto.getDescription());
            service.setUnitOfMeasure(dto.getUnitOfMeasure());
            service.setDefaultMonthlyCost(dto.getDefaultMonthlyCost());
            service.setDefaultIncludeInReport(dto.getDefaultIncludeInReport() != null ? dto.getDefaultIncludeInReport() : false);

            // Update formula
            if (dto.getFormula() != null && dto.getFormula().getExpression() != null 
                && !dto.getFormula().getExpression().trim().isEmpty()) {
                if (service.getFormula() == null) {
                    service.setFormula(new ServiceFormula());
                }
                service.getFormula().setExpression(dto.getFormula().getExpression().trim());
                service.getFormula().setDescription(dto.getFormula().getDescription());
            } else {
                // Remove formula if expression is empty
                service.setFormula(null);
            }

            Service saved = serviceRepository.save(service);
            log.info("Reactivated and updated service: {}", saved.getName());
            return saved;
        }

        // Create new service
        Service service = new Service();
        service.setName(serviceName);
        service.setDescription(dto.getDescription());
        service.setUnitOfMeasure(dto.getUnitOfMeasure());
        service.setDefaultMonthlyCost(dto.getDefaultMonthlyCost());
        service.setDefaultIncludeInReport(dto.getDefaultIncludeInReport() != null ? dto.getDefaultIncludeInReport() : false);
        service.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Create formula if provided
        if (dto.getFormula() != null && dto.getFormula().getExpression() != null 
            && !dto.getFormula().getExpression().trim().isEmpty()) {
            ServiceFormula formula = new ServiceFormula();
            formula.setExpression(dto.getFormula().getExpression().trim());
            formula.setDescription(dto.getFormula().getDescription());
            service.setFormula(formula);
        }

        Service saved = serviceRepository.save(service);
        log.info("Created service: {}", saved.getName());
        return saved;
    }

    /**
     * Update an existing service.
     */
    @Transactional
    public Service updateService(Long id, ServiceDto dto) {
        Service service = getServiceById(id);

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            // Check if another service with same name exists (only active services)
            String newName = dto.getName().trim();
            java.util.Optional<Service> existingService = serviceRepository.findByNameIgnoreCase(newName);
            
            if (existingService.isPresent()) {
                Service otherService = existingService.get();
                // Only check if it's a different service and it's active
                if (!otherService.getId().equals(id) && otherService.getActive() != null && otherService.getActive()) {
                    throw new ValidationException("Service with name '" + newName + "' already exists");
                }
            }
            service.setName(newName);
        }

        if (dto.getDescription() != null) {
            service.setDescription(dto.getDescription());
        }

        if (dto.getUnitOfMeasure() != null) {
            service.setUnitOfMeasure(dto.getUnitOfMeasure());
        }

        if (dto.getDefaultMonthlyCost() != null) {
            service.setDefaultMonthlyCost(dto.getDefaultMonthlyCost());
        }

        if (dto.getDefaultIncludeInReport() != null) {
            service.setDefaultIncludeInReport(dto.getDefaultIncludeInReport());
        }

        if (dto.getActive() != null) {
            service.setActive(dto.getActive());
        }

        // Update formula
        if (dto.getFormula() != null) {
            if (dto.getFormula().getExpression() != null && !dto.getFormula().getExpression().trim().isEmpty()) {
                if (service.getFormula() == null) {
                    service.setFormula(new ServiceFormula());
                }
                service.getFormula().setExpression(dto.getFormula().getExpression().trim());
                service.getFormula().setDescription(dto.getFormula().getDescription());
            } else {
                // Remove formula if expression is empty
                service.setFormula(null);
            }
        }

        Service saved = serviceRepository.save(service);
        log.info("Updated service: {}", saved.getName());
        return saved;
    }

    /**
     * Delete a service (soft delete - sets active to false).
     */
    @Transactional
    public void deleteService(Long id) {
        Service service = getServiceById(id);
        service.setActive(false);
        serviceRepository.save(service);
        log.info("Deleted (deactivated) service: {}", service.getName());
    }

    /**
     * Get all service keywords for use in formulas.
     * Returns service names transformed to keywords: lowercase, spaces replaced with underscore, diacritics removed.
     */
    public ServiceKeywordsResponse getServiceKeywords() {
        List<Service> services = serviceRepository.findAll();
        List<ServiceKeyword> keywords = services.stream()
            .map(service -> new ServiceKeyword(
                service.getId(),
                service.getName(),
                normalizeToKeyword(service.getName())
            ))
            .collect(Collectors.toList());
        return new ServiceKeywordsResponse(keywords);
    }
    
    /**
     * Normalize service name to keyword format.
     * - Convert to lowercase
     * - Replace spaces with underscores
     * - Remove diacritics (ă -> a, î -> i, etc.)
     * - Remove special characters (keep only letters, numbers, and underscores)
     * 
     * Examples:
     * - "Salubrizare" -> "salubrizare"
     * - "Cota întreținere" -> "cota_intretinere"
     * - "Serviciu de alarmă" -> "serviciu_de_alarma"
     * - "Serviciu 123" -> "serviciu_123"
     */
    private String normalizeToKeyword(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return "";
        }
        
        // Remove diacritics (ă -> a, î -> i, etc.)
        String normalized = Normalizer.normalize(serviceName, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // Convert to lowercase
        normalized = normalized.toLowerCase();
        
        // Replace spaces and special characters with underscore
        normalized = normalized.replaceAll("[^a-z0-9_]+", "_");
        
        // Remove multiple consecutive underscores
        normalized = normalized.replaceAll("_{2,}", "_");
        
        // Remove leading and trailing underscores
        normalized = normalized.replaceAll("^_+|_+$", "");
        
        return normalized;
    }
}

