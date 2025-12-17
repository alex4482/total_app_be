package com.work.total_app.models.tenant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.work.total_app.models.service.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DTO for TenantRentalData with resolved includeInReport values.
 * Used for serialization to frontend.
 */
public class TenantRentalDataDto {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private String rentalSpaceId;
    private String rentalSpaceName;
    private Long buildingId;
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date endDate;
    
    private Double rent;
    private Currency currency;
    private String contractNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date contractDate;
    
    private List<ActiveServiceWithResolvedIncludeInReport> activeServices = new ArrayList<>();
    
    // Nested class for active service with resolved includeInReport
    public static class ActiveServiceWithResolvedIncludeInReport {
        private Long serviceId;
        private String serviceName;
        private Double customMonthlyCost;
        private Boolean includeInReport; // Resolved value (null becomes service.defaultIncludeInReport)
        private Boolean isIncludeInReportManuallySet; // true if user explicitly set it
        private Date activeFrom;
        private Date activeUntil;
        private String notes;
        
        // Getters and setters
        public Long getServiceId() { return serviceId; }
        public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
        
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public Double getCustomMonthlyCost() { return customMonthlyCost; }
        public void setCustomMonthlyCost(Double customMonthlyCost) { this.customMonthlyCost = customMonthlyCost; }
        
        public Boolean getIncludeInReport() { return includeInReport; }
        public void setIncludeInReport(Boolean includeInReport) { this.includeInReport = includeInReport; }
        
        public Boolean getIsIncludeInReportManuallySet() { return isIncludeInReportManuallySet; }
        public void setIsIncludeInReportManuallySet(Boolean isIncludeInReportManuallySet) { 
            this.isIncludeInReportManuallySet = isIncludeInReportManuallySet; 
        }
        
        public Date getActiveFrom() { return activeFrom; }
        public void setActiveFrom(Date activeFrom) { this.activeFrom = activeFrom; }
        
        public Date getActiveUntil() { return activeUntil; }
        public void setActiveUntil(Date activeUntil) { this.activeUntil = activeUntil; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    
    public String getRentalSpaceId() { return rentalSpaceId; }
    public void setRentalSpaceId(String rentalSpaceId) { this.rentalSpaceId = rentalSpaceId; }
    
    public String getRentalSpaceName() { return rentalSpaceName; }
    public void setRentalSpaceName(String rentalSpaceName) { this.rentalSpaceName = rentalSpaceName; }
    
    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
    
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    
    public Double getRent() { return rent; }
    public void setRent(Double rent) { this.rent = rent; }
    
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    
    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
    
    public Date getContractDate() { return contractDate; }
    public void setContractDate(Date contractDate) { this.contractDate = contractDate; }
    
    public List<ActiveServiceWithResolvedIncludeInReport> getActiveServices() { return activeServices; }
    public void setActiveServices(List<ActiveServiceWithResolvedIncludeInReport> activeServices) { 
        this.activeServices = activeServices; 
    }
    
    /**
     * Convert TenantRentalData to DTO with resolved includeInReport values.
     * @param rentalData The TenantRentalData entity
     * @param serviceMap Map of serviceId -> Service for resolving defaultIncludeInReport
     */
    public static TenantRentalDataDto fromTenantRentalData(TenantRentalData rentalData, 
                                                            Map<Long, Service> serviceMap) {
        TenantRentalDataDto dto = new TenantRentalDataDto();
        dto.setId(rentalData.getId());
        dto.setTenantId(rentalData.getTenant() != null ? rentalData.getTenant().getId() : null);
        dto.setTenantName(rentalData.getTenant() != null ? rentalData.getTenant().getName() : null);
        dto.setRentalSpaceId(rentalData.getRentalSpace() != null ? rentalData.getRentalSpace().getName() : null);
        dto.setRentalSpaceName(rentalData.getRentalSpace() != null ? rentalData.getRentalSpace().getName() : null);
        dto.setBuildingId(rentalData.getRentalSpace() != null && rentalData.getRentalSpace().getBuilding() != null 
            ? rentalData.getRentalSpace().getBuilding().getId() : null);
        dto.setStartDate(rentalData.getStartDate());
        dto.setEndDate(rentalData.getEndDate());
        dto.setRent(rentalData.getRent());
        dto.setCurrency(rentalData.getCurrency());
        dto.setContractNumber(rentalData.getContractNumber());
        dto.setContractDate(rentalData.getContractDate());
        
        // Convert active services with resolved includeInReport
        List<ActiveServiceWithResolvedIncludeInReport> activeServices = new ArrayList<>();
        for (ActiveService activeService : rentalData.getActiveServices()) {
            ActiveServiceWithResolvedIncludeInReport serviceDto = new ActiveServiceWithResolvedIncludeInReport();
            serviceDto.setServiceId(activeService.getServiceId());
            
            Service service = serviceMap.get(activeService.getServiceId());
            if (service != null) {
                serviceDto.setServiceName(service.getName());
                
                // Resolve includeInReport: if null in ActiveService, use service.defaultIncludeInReport
                Boolean includeInReport = activeService.getIncludeInReport();
                Boolean isManuallySet = includeInReport != null;
                
                if (includeInReport == null) {
                    includeInReport = service.getDefaultIncludeInReport() != null 
                        ? service.getDefaultIncludeInReport() 
                        : false;
                }
                
                serviceDto.setIncludeInReport(includeInReport);
                serviceDto.setIsIncludeInReportManuallySet(isManuallySet);
            }
            
            serviceDto.setCustomMonthlyCost(activeService.getCustomMonthlyCost());
            serviceDto.setActiveFrom(activeService.getActiveFrom());
            serviceDto.setActiveUntil(activeService.getActiveUntil());
            serviceDto.setNotes(activeService.getNotes());
            
            activeServices.add(serviceDto);
        }
        dto.setActiveServices(activeServices);
        
        return dto;
    }
}

