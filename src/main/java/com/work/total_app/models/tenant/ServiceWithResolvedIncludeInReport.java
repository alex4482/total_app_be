package com.work.total_app.models.tenant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.work.total_app.models.service.Service;

import java.util.Date;

/**
 * DTO for an active service with resolved includeInReport value.
 * Used for serialization to frontend.
 */
public class ServiceWithResolvedIncludeInReport {
    private Long serviceId;
    private String serviceName;
    private String serviceDescription;
    private String unitOfMeasure;
    private Double customMonthlyCost;
    private Boolean includeInReport; // Resolved value (null in ActiveService becomes service.defaultIncludeInReport)
    private String includeInReportMode; // "IMPLICIT", "MANUAL_ON", "MANUAL_OFF"
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date activeFrom;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date activeUntil;
    private String notes;
    
    // Getters and setters
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }
    
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    
    public Double getCustomMonthlyCost() { return customMonthlyCost; }
    public void setCustomMonthlyCost(Double customMonthlyCost) { this.customMonthlyCost = customMonthlyCost; }
    
    public Boolean getIncludeInReport() { return includeInReport; }
    public void setIncludeInReport(Boolean includeInReport) { this.includeInReport = includeInReport; }
    
    public String getIncludeInReportMode() { return includeInReportMode; }
    public void setIncludeInReportMode(String includeInReportMode) { 
        this.includeInReportMode = includeInReportMode; 
    }
    
    public Date getActiveFrom() { return activeFrom; }
    public void setActiveFrom(Date activeFrom) { this.activeFrom = activeFrom; }
    
    public Date getActiveUntil() { return activeUntil; }
    public void setActiveUntil(Date activeUntil) { this.activeUntil = activeUntil; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    /**
     * Convert ActiveService to DTO with resolved includeInReport.
     * @param activeService The ActiveService entity
     * @param service The Service entity (for default values)
     */
    public static ServiceWithResolvedIncludeInReport fromActiveService(ActiveService activeService, Service service) {
        ServiceWithResolvedIncludeInReport dto = new ServiceWithResolvedIncludeInReport();
        dto.setServiceId(activeService.getServiceId());
        dto.setCustomMonthlyCost(activeService.getCustomMonthlyCost());
        dto.setActiveFrom(activeService.getActiveFrom());
        dto.setActiveUntil(activeService.getActiveUntil());
        dto.setNotes(activeService.getNotes());
        
        if (service != null) {
            dto.setServiceName(service.getName());
            dto.setServiceDescription(service.getDescription());
            dto.setUnitOfMeasure(service.getUnitOfMeasure());
            
            // Resolve includeInReport and determine mode:
            // - If null in ActiveService → IMPLICIT (use service.defaultIncludeInReport)
            // - If true in ActiveService → MANUAL_ON
            // - If false in ActiveService → MANUAL_OFF
            Boolean includeInReportRaw = activeService.getIncludeInReport();
            String mode;
            Boolean includeInReportResolved;
            
            if (includeInReportRaw == null) {
                // IMPLICIT - use service default
                mode = "IMPLICIT";
                includeInReportResolved = service.getDefaultIncludeInReport() != null 
                    ? service.getDefaultIncludeInReport() 
                    : false;
            } else if (includeInReportRaw) {
                // MANUAL_ON
                mode = "MANUAL_ON";
                includeInReportResolved = true;
            } else {
                // MANUAL_OFF
                mode = "MANUAL_OFF";
                includeInReportResolved = false;
            }
            
            dto.setIncludeInReport(includeInReportResolved);
            dto.setIncludeInReportMode(mode);
        }
        
        return dto;
    }
}

