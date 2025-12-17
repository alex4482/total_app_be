package com.work.total_app.services;

import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.service.Service;
import com.work.total_app.models.tenant.*;
import com.work.total_app.models.tenant.Currency;
import com.work.total_app.repositories.RentalSpaceRepository;
import com.work.total_app.repositories.ServiceRepository;
import com.work.total_app.repositories.TenantRentalDataRepository;
import com.work.total_app.repositories.TenantRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Log4j2
public class TenantRentalService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RentalSpaceRepository rentalSpaceRepository;

    @Autowired
    private TenantRentalDataRepository tenantRentalRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private com.work.total_app.repositories.ServiceMonthlyValueRepository serviceMonthlyValueRepository;

    @Autowired
    private ExcelReportService excelReportService;

    public TenantRentalData startNewRent(TenantRentalDto dto) {
        TenantRentalData trd = new TenantRentalData();
        if (dto.tenantId() == null || dto.rentalSpaceId() == null)
            throw new ValidationException("Missing id for new rent start: " + dto);

        Tenant t = tenantRepository.findById(dto.tenantId()).orElseThrow(
                () -> new NotFoundException("Cant find tenant with id: " + dto.tenantId() + " for new rent start."));
        trd.setTenant(t);

        RentalSpace rs = rentalSpaceRepository.findById(dto.rentalSpaceId()).orElseThrow(
                () -> new NotFoundException("Cant find rental space with id: " + dto.rentalSpaceId() + " for new rent start."));
        trd.setRentalSpace(rs);

        if (rs.getRentalAgreement() != null)
        {
            throw new ValidationException("Rental space is already occupied. Please select a different space.");
        }

        // Validate end date if provided
        if (dto.endDate() != null && dto.startDate() != null && dto.endDate().before(dto.startDate())) {
            throw new ValidationException(
                String.format("End date (%s) cannot be before start date (%s)", 
                    dto.endDate(), dto.startDate()));
        }

        trd.setStartDate(dto.startDate());
        trd.setEndDate(dto.endDate());
        trd.setRent(dto.price());
        trd.setCurrency(dto.currency() != null ? dto.currency() : Currency.RON);
        trd.addPriceChange(dto.price(), dto.startDate() != null ? dto.startDate() : Date.from(Instant.now()));

        // Set contract information if provided
        if (dto.contractNumber() != null && !dto.contractNumber().isEmpty()) {
            trd.setContractNumber(dto.contractNumber());
        }
        if (dto.contractDate() != null) {
            trd.setContractDate(dto.contractDate());
        }

        // Add active services if provided
        if (dto.activeServices() != null && !dto.activeServices().isEmpty()) {
            Date defaultActiveFrom = dto.startDate() != null ? dto.startDate() : Date.from(Instant.now());
            for (ActiveServiceDto activeServiceDto : dto.activeServices()) {
                Service service = serviceRepository.findById(activeServiceDto.serviceId())
                    .orElseThrow(() -> new NotFoundException("Service not found with id: " + activeServiceDto.serviceId()));
                
                ActiveService activeService = new ActiveService();
                activeService.setServiceId(service.getId());
                activeService.setCustomMonthlyCost(activeServiceDto.customMonthlyCost());
                // Only set includeInReport if explicitly provided in DTO, otherwise leave null
                // This allows us to track if user has manually set it or not
                activeService.setIncludeInReport(activeServiceDto.includeInReport());
                activeService.setActiveFrom(activeServiceDto.activeFrom() != null ? activeServiceDto.activeFrom() : defaultActiveFrom);
                activeService.setActiveUntil(activeServiceDto.activeUntil());
                activeService.setNotes(activeServiceDto.notes());
                
                trd.getActiveServices().add(activeService);
            }
        }

        trd = tenantRentalRepository.save(trd);

        t.addRentalData(trd);
        rs.setRentalAgreement(trd);
        rentalSpaceRepository.save(rs);
        tenantRepository.save(t);

        return trd;
    }

    /**
     * Update rental agreement details.
     */
    @Transactional
    public TenantRentalData updateRentalAgreement(Long rentalAgreementId, UpdateTenantRentalDto dto) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        if (dto.startDate() != null) {
            trd.setStartDate(dto.startDate());
        }
        if (dto.endDate() != null) {
            trd.setEndDate(dto.endDate());
        }
        if (dto.rent() != null) {
            trd.setRent(dto.rent());
        }
        if (dto.currency() != null) {
            trd.setCurrency(dto.currency());
        }

        // Update contract information if provided
        if (dto.contractNumber() != null) {
            trd.setContractNumber(dto.contractNumber().isEmpty() ? null : dto.contractNumber());
        }
        if (dto.contractDate() != null) {
            trd.setContractDate(dto.contractDate());
        }

        // Update active services if provided (replaces all existing)
        if (dto.activeServices() != null) {
            trd.getActiveServices().clear();
            Date defaultActiveFrom = trd.getStartDate() != null ? trd.getStartDate() : Date.from(Instant.now());
            // Normalize defaultActiveFrom to start of day
            LocalDate defaultActiveFromLocal = dateToLocalDate(defaultActiveFrom);
            Date normalizedDefaultActiveFrom = Date.from(defaultActiveFromLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
            
            // Normalize rental dates to start of day for comparison
            LocalDate startDateLocal = dateToLocalDate(trd.getStartDate());
            LocalDate endDateLocal = dateToLocalDate(trd.getEndDate());
            
            for (ActiveServiceDto activeServiceDto : dto.activeServices()) {
                Service service = serviceRepository.findById(activeServiceDto.serviceId())
                    .orElseThrow(() -> new NotFoundException("Service not found with id: " + activeServiceDto.serviceId()));
                
                // Validate dates - normalize to start of day for comparison
                Date activeFrom = activeServiceDto.activeFrom() != null ? activeServiceDto.activeFrom() : normalizedDefaultActiveFrom;
                LocalDate activeFromLocal = dateToLocalDate(activeFrom);
                
                if (startDateLocal != null && activeFromLocal.isBefore(startDateLocal)) {
                    throw new ValidationException(
                        String.format("Data de activare a serviciului (%s) nu poate fi înainte de data de început a contractului (%s)", 
                            activeFromLocal, startDateLocal));
                }
                if (endDateLocal != null && activeFromLocal.isAfter(endDateLocal)) {
                    throw new ValidationException(
                        String.format("Data de activare a serviciului (%s) nu poate fi după data de sfârșit a contractului (%s)", 
                            activeFromLocal, endDateLocal));
                }
                if (activeServiceDto.activeUntil() != null) {
                    LocalDate activeUntilLocal = dateToLocalDate(activeServiceDto.activeUntil());
                    if (activeFromLocal != null && activeUntilLocal.isBefore(activeFromLocal)) {
                        throw new ValidationException(
                            String.format("Data de dezactivare a serviciului (%s) nu poate fi înainte de data de activare (%s)", 
                                activeUntilLocal, activeFromLocal));
                    }
                    if (endDateLocal != null && activeUntilLocal.isAfter(endDateLocal)) {
                        throw new ValidationException(
                            String.format("Data de dezactivare a serviciului (%s) nu poate fi după data de sfârșit a contractului (%s)", 
                                activeUntilLocal, endDateLocal));
                    }
                }
                
                ActiveService activeService = new ActiveService();
                activeService.setServiceId(service.getId());
                activeService.setCustomMonthlyCost(activeServiceDto.customMonthlyCost());
                // Only set includeInReport if explicitly provided in DTO, otherwise leave null
                // This allows us to track if user has manually set it or not
                activeService.setIncludeInReport(activeServiceDto.includeInReport());
                
                // Normalize activeFrom to start of day (00:00:00) to avoid timezone issues
                Date normalizedActiveFrom = Date.from(activeFromLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
                activeService.setActiveFrom(normalizedActiveFrom);
                
                // Normalize activeUntil to start of day if provided
                if (activeServiceDto.activeUntil() != null) {
                    LocalDate activeUntilLocal = dateToLocalDate(activeServiceDto.activeUntil());
                    Date normalizedActiveUntil = Date.from(activeUntilLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    activeService.setActiveUntil(normalizedActiveUntil);
                } else {
                    activeService.setActiveUntil(null);
                }
                activeService.setNotes(activeServiceDto.notes());
                
                trd.getActiveServices().add(activeService);
            }
            
            log.info("Updated active services for rental agreement {}", rentalAgreementId);
        }

        return tenantRentalRepository.save(trd);
    }

    /**
     * Change rental price with effective date.
     * Adds a new entry to priceChanges history and updates current rent.
     */
    @Transactional
    public TenantRentalData changePrice(Long rentalAgreementId, ChangePriceDto dto) {
        if (dto.newPrice() == null || dto.newPrice() <= 0) {
            throw new ValidationException("New price must be greater than 0");
        }
        if (dto.effectiveDate() == null) {
            throw new ValidationException("Effective date is required");
        }

        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        // Validate effective date is between startDate and endDate (if exists)
        if (trd.getStartDate() != null && dto.effectiveDate().before(trd.getStartDate())) {
            throw new ValidationException(
                String.format("Data efectivă (%s) nu poate fi înainte de data de început a contractului (%s)", 
                    dto.effectiveDate(), trd.getStartDate()));
        }
        if (trd.getEndDate() != null && dto.effectiveDate().after(trd.getEndDate())) {
            throw new ValidationException(
                String.format("Data efectivă (%s) nu poate fi după data de sfârșit a contractului (%s)", 
                    dto.effectiveDate(), trd.getEndDate()));
        }

        // Update current rent
        trd.setRent(dto.newPrice());

        // Add to price change history
        trd.addPriceChange(dto.newPrice(), dto.effectiveDate());

        log.info("Changed price for rental agreement {} to {} effective from {}", 
                rentalAgreementId, dto.newPrice(), dto.effectiveDate());

        return tenantRentalRepository.save(trd);
    }

    /**
     * Terminate rental agreement by setting end date.
     */
    @Transactional
    public TenantRentalData terminateRentalAgreement(Long rentalAgreementId, TerminateRentalDto dto) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        if (dto.endDate() == null) {
            throw new ValidationException("End date is required");
        }

        // Validate end date
        if (trd.getStartDate() != null && dto.endDate().before(trd.getStartDate())) {
            throw new ValidationException(
                String.format("End date (%s) cannot be before start date (%s)", 
                    dto.endDate(), trd.getStartDate()));
        }

        trd.setEndDate(dto.endDate());

        // Log message based on whether end date is in the future or already reached
        Date now = Date.from(Instant.now());
        if (dto.endDate().after(now)) {
            log.info("Scheduled future termination for rental agreement {} with end date {}", rentalAgreementId, dto.endDate());
        } else {
            log.info("Terminated rental agreement {} with end date {}", rentalAgreementId, dto.endDate());
        }

        return tenantRentalRepository.save(trd);
    }

    /**
     * Delete rental agreement completely (removes link between tenant and rental space).
     */
    @Transactional
    public void deleteRentalAgreement(Long rentalAgreementId) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        // Remove from tenant
        Tenant tenant = trd.getTenant();
        if (tenant != null) {
            tenant.removeRentalData(trd);
            tenantRepository.save(tenant);
        }

        // Remove from rental space
        RentalSpace rentalSpace = trd.getRentalSpace();
        if (rentalSpace != null) {
            rentalSpace.setRentalAgreement(null);
            rentalSpaceRepository.save(rentalSpace);
        }

        // Delete the rental agreement
        tenantRentalRepository.delete(trd);

        log.info("Deleted rental agreement {}", rentalAgreementId);
    }

    /**
     * Update active services for a rental agreement.
     * Replaces all existing services with the ones provided in the request.
     */
    @Transactional
    public UpdateServicesResponse updateServices(Long rentalAgreementId, UpdateServicesRequest request) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
            .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        List<Long> activatedServices = new ArrayList<>();
        List<Long> deactivatedServices = new ArrayList<>();

        // Build map of existing services to preserve includeInReport if not explicitly set
        Map<Long, ActiveService> existingServicesMap = new HashMap<>();
        for (ActiveService existingService : trd.getActiveServices()) {
            existingServicesMap.put(existingService.getServiceId(), existingService);
        }

        // Clear existing services
        trd.getActiveServices().clear();

        // Process services from request
        if (request.services() != null) {
            Date defaultActiveFrom = trd.getStartDate() != null ? trd.getStartDate() : Date.from(Instant.now());

            for (UpdateServicesRequest.ServiceUpdateDto serviceDto : request.services()) {
                if (serviceDto.active() == null || !serviceDto.active()) {
                    // Service is inactive - skip it
                    if (serviceDto.serviceId() != null) {
                        deactivatedServices.add(serviceDto.serviceId());
                    }
                    continue;
                }

                // Service is active - validate and add it
                Service service = serviceRepository.findById(serviceDto.serviceId())
                    .orElseThrow(() -> new NotFoundException("Service not found with id: " + serviceDto.serviceId()));
                
                // Validate that service is active
                if (service.getActive() == null || !service.getActive()) {
                    throw new ValidationException(
                        String.format("Service with id %d is not active", serviceDto.serviceId()));
                }

                // Validate dates - normalize to start of day for comparison
                Date activeFrom = serviceDto.activeFrom() != null ? serviceDto.activeFrom() : defaultActiveFrom;
                
                // Normalize dates to start of day for comparison (ignore time and timezone)
                LocalDate activeFromLocal = dateToLocalDate(activeFrom);
                LocalDate startDateLocal = dateToLocalDate(trd.getStartDate());
                LocalDate endDateLocal = dateToLocalDate(trd.getEndDate());
                
                if (startDateLocal != null && activeFromLocal.isBefore(startDateLocal)) {
                    throw new ValidationException(
                        String.format("Data de activare a serviciului (%s) nu poate fi înainte de data de început a contractului (%s)",
                            activeFromLocal, startDateLocal));
                }
                if (endDateLocal != null && activeFromLocal.isAfter(endDateLocal)) {
                    throw new ValidationException(
                        String.format("Data de activare a serviciului (%s) nu poate fi după data de sfârșit a contractului (%s)",
                            activeFromLocal, endDateLocal));
                }
                if (serviceDto.activeUntil() != null) {
                    LocalDate activeUntilLocal = dateToLocalDate(serviceDto.activeUntil());
                    if (activeFromLocal != null && activeUntilLocal.isBefore(activeFromLocal)) {
                        throw new ValidationException(
                            String.format("Data de dezactivare a serviciului (%s) nu poate fi înainte de data de activare (%s)",
                                activeUntilLocal, activeFromLocal));
                    }
                    if (endDateLocal != null && activeUntilLocal.isAfter(endDateLocal)) {
                        throw new ValidationException(
                            String.format("Data de dezactivare a serviciului (%s) nu poate fi după data de sfârșit a contractului (%s)",
                                activeUntilLocal, endDateLocal));
                    }
                }

                // Create active service
                ActiveService activeService = new ActiveService();
                activeService.setServiceId(service.getId());
                activeService.setCustomMonthlyCost(serviceDto.customMonthlyCost());
                
                // Validate includeInReport and useDefaultIncludeInReport
                // Rule: If useDefaultIncludeInReport is true, includeInReport must be null
                if (serviceDto.useDefaultIncludeInReport() != null && serviceDto.useDefaultIncludeInReport()) {
                    if (serviceDto.includeInReport() != null) {
                        throw new ValidationException(
                            "If useDefaultIncludeInReport is true, includeInReport must be null");
                    }
                }
                
                // Handle includeInReport with three states: ON MANUAL, OFF MANUAL, IMPLICIT
                // Priority:
                // 1. If useDefaultIncludeInReport is true → set to null (IMPLICIT - use service.defaultIncludeInReport)
                // 2. If includeInReport is explicitly set (true/false) → use that value (ON/OFF MANUAL)
                // 3. If neither is set → preserve existing value if service existed, otherwise null (IMPLICIT)
                if (serviceDto.useDefaultIncludeInReport() != null && serviceDto.useDefaultIncludeInReport()) {
                    // Explicitly set to use default (IMPLICIT)
                    activeService.setIncludeInReport(null);
                } else if (serviceDto.includeInReport() != null) {
                    // Explicitly set to true or false (ON/OFF MANUAL)
                    activeService.setIncludeInReport(serviceDto.includeInReport());
                } else {
                    // Not explicitly set in request - preserve existing value if service existed
                    ActiveService existingService = existingServicesMap.get(serviceDto.serviceId());
                    if (existingService != null) {
                        // Preserve existing value (could be null for IMPLICIT, true for ON MANUAL, false for OFF MANUAL)
                        activeService.setIncludeInReport(existingService.getIncludeInReport());
                    } else {
                        // New service - leave null (IMPLICIT - will use service.defaultIncludeInReport)
                        activeService.setIncludeInReport(null);
                    }
                }
                
                // Normalize activeFrom to start of day (00:00:00) to avoid timezone issues
                Date normalizedActiveFrom = Date.from(activeFromLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
                activeService.setActiveFrom(normalizedActiveFrom);
                
                // Normalize activeUntil to start of day if provided
                if (serviceDto.activeUntil() != null) {
                    LocalDate activeUntilLocal = dateToLocalDate(serviceDto.activeUntil());
                    Date normalizedActiveUntil = Date.from(activeUntilLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    activeService.setActiveUntil(normalizedActiveUntil);
                } else {
                    activeService.setActiveUntil(null);
                }
                activeService.setNotes(serviceDto.notes());

                trd.getActiveServices().add(activeService);
                activatedServices.add(service.getId());
            }
        }

        tenantRentalRepository.save(trd);

        log.info("Updated services for rental agreement {}: {} activated, {} deactivated",
            rentalAgreementId, activatedServices.size(), deactivatedServices.size());

        return new UpdateServicesResponse(
            activatedServices.size() + deactivatedServices.size(),
            activatedServices,
            deactivatedServices
        );
    }
    
    /**
     * Update services and return updated services with resolved includeInReport.
     * This method updates services and returns them with resolved includeInReport values.
     */
    @Transactional
    public GetServicesResponse updateServicesAndGet(Long rentalAgreementId, UpdateServicesRequest request) {
        // Update services
        updateServices(rentalAgreementId, request);
        // Return updated services with resolved includeInReport
        return getServices(rentalAgreementId);
    }

    /**
     * Update service values for a specific month.
     */
    @Transactional
    public UpdateServiceValuesResponse updateServiceValues(Long rentalAgreementId, UpdateServiceValuesRequest request) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
            .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        // Validate year and month
        if (request.year() == null || request.month() == null) {
            throw new ValidationException("Year and month are required");
        }
        if (request.month() < 1 || request.month() > 12) {
            throw new ValidationException("Month must be between 1 and 12");
        }

        // Convert month from 1-12 to 0-11 for internal storage
        int month = request.month() - 1;

        // Validate month is within rental period (compare only year and month, not exact dates)
        // If contract starts on 9 November 2025, November 2025 should be valid
        // If contract ends on 29 November 2025, November 2025 should be valid
        if (trd.getStartDate() != null) {
            LocalDate startDateLocal = dateToLocalDate(trd.getStartDate());
            int startYear = startDateLocal.getYear();
            int startMonth = startDateLocal.getMonthValue() - 1; // 0-based month
            
            // Check if requested month/year is before contract start month/year
            if (request.year() < startYear || (request.year() == startYear && month < startMonth)) {
                throw new ValidationException(
                    String.format("Luna %d/%d este înainte de luna de început a contractului (%d/%d)",
                        request.month(), request.year(), startMonth + 1, startYear));
            }
        }
        
        if (trd.getEndDate() != null) {
            LocalDate endDateLocal = dateToLocalDate(trd.getEndDate());
            int endYear = endDateLocal.getYear();
            int endMonth = endDateLocal.getMonthValue() - 1; // 0-based month
            
            // Check if requested month/year is after contract end month/year
            if (request.year() > endYear || (request.year() == endYear && month > endMonth)) {
                throw new ValidationException(
                    String.format("Luna %d/%d este după luna de sfârșit a contractului (%d/%d)",
                        request.month(), request.year(), endMonth + 1, endYear));
            }
        }

        // Get counters for calculation
        List<IndexCounter> counters = trd.getRentalSpace().getCounters();

        // Build map of active service IDs (from explicit activeServices)
        Set<Long> activeServiceIds = trd.getActiveServices().stream()
            .map(ActiveService::getServiceId)
            .collect(Collectors.toSet());

        // Get all services to check for implicit inclusion (defaultIncludeInReport = true)
        Map<Long, Service> allServices = serviceRepository.findAll().stream()
            .collect(Collectors.toMap(Service::getId, s -> s));

        List<UpdateServiceValuesResponse.UpdatedServiceInfo> updatedServices = new ArrayList<>();

        // Process service values
        if (request.serviceValues() != null) {
            for (UpdateServiceValuesRequest.ServiceValueDto valueDto : request.serviceValues()) {
                // Get service for validation and later use
                Service service = allServices.get(valueDto.serviceId());
                if (service == null) {
                    service = serviceRepository.findById(valueDto.serviceId())
                        .orElseThrow(() -> new NotFoundException("Service not found with id: " + valueDto.serviceId()));
                }
                
                // Validate service is active OR is implicitly included (defaultIncludeInReport = true)
                boolean isExplicitlyActive = activeServiceIds.contains(valueDto.serviceId());
                boolean isImplicitlyIncluded = service.getDefaultIncludeInReport() != null && service.getDefaultIncludeInReport();
                
                if (!isExplicitlyActive && !isImplicitlyIncluded) {
                    throw new ValidationException(
                        String.format("Service with id %d is not active for this rental agreement", valueDto.serviceId()));
                }

                // Validate value
                if (valueDto.value() == null || valueDto.value() < 0) {
                    throw new ValidationException(
                        String.format("Service value must be a positive number for service id %d", valueDto.serviceId()));
                }

                // Get or create ServiceMonthlyValue
                Optional<ServiceMonthlyValue> existingValue = serviceMonthlyValueRepository
                    .findByRentalDataIdAndServiceIdAndYearAndMonth(
                        trd.getId(), valueDto.serviceId(), request.year(), month);

                ServiceMonthlyValue monthlyValue;
                Double oldValue = null;

                if (existingValue.isPresent()) {
                    monthlyValue = existingValue.get();
                    oldValue = monthlyValue.getCustomValue();
                } else {
                    monthlyValue = new ServiceMonthlyValue();
                    monthlyValue.setRentalData(trd);
                    monthlyValue.setServiceId(valueDto.serviceId());
                    monthlyValue.setYear(request.year());
                    monthlyValue.setMonth(month);
                }

                // Update value
                monthlyValue.setCustomValue(valueDto.value());
                monthlyValue.setIsManual(true);
                monthlyValue.setNotes(valueDto.notes());

                serviceMonthlyValueRepository.save(monthlyValue);

                updatedServices.add(new UpdateServiceValuesResponse.UpdatedServiceInfo(
                    valueDto.serviceId(),
                    service.getName(),
                    oldValue,
                    valueDto.value()
                ));

                log.info("Updated service value for rental agreement {} service {} month {}/{}: {} -> {}",
                    rentalAgreementId, service.getName(), request.month(), request.year(), oldValue, valueDto.value());
            }
        }

        return new UpdateServiceValuesResponse(
            request.year(),
            request.month(),
            updatedServices.size(),
            updatedServices
        );
    }

    /**
     * Get service values for a specific month.
     */
    public GetServiceValuesResponse getServiceValues(Long rentalAgreementId, int year, int month) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
            .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        // Validate month
        if (month < 1 || month > 12) {
            throw new ValidationException("Month must be between 1 and 12");
        }

        // Convert month from 1-12 to 0-11 for internal storage
        int monthIndex = month - 1;

        // Get counters for calculation
        List<IndexCounter> counters = trd.getRentalSpace().getCounters();

        // Load custom monthly values
        Map<String, ServiceMonthlyValue> customValuesMap = new HashMap<>();
        List<ServiceMonthlyValue> customValues = serviceMonthlyValueRepository
            .findByRentalDataIdAndYear(trd.getId(), year);
        for (ServiceMonthlyValue customValue : customValues) {
            if (customValue.getMonth() == monthIndex) {
                String key = customValue.getServiceId() + "_" + monthIndex;
                customValuesMap.put(key, customValue);
            }
        }

        // Calculate expected service values for dependencies
        Map<Long, Map<Integer, Double>> allServiceValues = new HashMap<>();
        for (ActiveService activeService : trd.getActiveServices()) {
            allServiceValues.put(activeService.getServiceId(), new HashMap<>());
        }

        // Multiple passes to handle dependencies
        for (int pass = 0; pass < 10; pass++) {
            boolean allCalculated = true;

            for (ActiveService activeService : trd.getActiveServices()) {
                Service service = serviceRepository.findById(activeService.getServiceId())
                    .orElse(null);
                if (service == null) continue;

                Map<Integer, Double> monthValues = allServiceValues.get(activeService.getServiceId());
                if (monthValues == null) {
                    monthValues = new HashMap<>();
                    allServiceValues.put(activeService.getServiceId(), monthValues);
                }

                if (!monthValues.containsKey(monthIndex)) {
                    // Check if service is active for this month
                    Calendar cal = Calendar.getInstance();
                    cal.set(year, monthIndex, 1, 0, 0, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date monthStart = cal.getTime();

                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date monthEnd = cal.getTime();

                    boolean isActive = isServiceActiveForMonth(activeService, monthStart, monthEnd);
                    if (isActive) {
                        // Check for custom value first
                        String customKey = activeService.getServiceId() + "_" + monthIndex;
                        if (customValuesMap.containsKey(customKey)) {
                            ServiceMonthlyValue customValue = customValuesMap.get(customKey);
                            monthValues.put(monthIndex, customValue.getCustomValue());
                        } else {
                            // Calculate value using ExcelReportService logic
                            Double value = calculateServiceValue(activeService, service, trd, counters, year, monthIndex, allServiceValues);
                            monthValues.put(monthIndex, value != null ? value : 0.0);
                        }
                    } else {
                        monthValues.put(monthIndex, 0.0);
                    }
                }
            }

            if (allCalculated) break;
        }

        // Build response
        List<GetServiceValuesResponse.ServiceValueInfo> serviceValues = new ArrayList<>();

        for (ActiveService activeService : trd.getActiveServices()) {
            Service service = serviceRepository.findById(activeService.getServiceId())
                .orElse(null);
            if (service == null) continue;

            // Check if service is active for this month
            Calendar cal = Calendar.getInstance();
            cal.set(year, monthIndex, 1, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date monthStart = cal.getTime();

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date monthEnd = cal.getTime();

            boolean isActive = isServiceActiveForMonth(activeService, monthStart, monthEnd);
            if (!isActive) {
                continue; // Skip inactive services
            }

            // Get values
            String customKey = activeService.getServiceId() + "_" + monthIndex;
            ServiceMonthlyValue customValue = customValuesMap.get(customKey);
            Map<Integer, Double> monthValues = allServiceValues.get(activeService.getServiceId());
            Double calculatedValue = monthValues != null ? monthValues.get(monthIndex) : 0.0;

            Double value;
            Boolean isManual = false;
            String notes = null;

            if (customValue != null && customValue.getIsManual() != null && customValue.getIsManual()) {
                value = customValue.getCustomValue();
                isManual = true;
                notes = customValue.getNotes();
            } else {
                value = calculatedValue;
                isManual = false;
            }

            serviceValues.add(new GetServiceValuesResponse.ServiceValueInfo(
                activeService.getServiceId(),
                service.getName(),
                value,
                calculatedValue,
                isManual,
                notes,
                service.getUnitOfMeasure() != null ? service.getUnitOfMeasure() : ""
            ));
        }

        return new GetServiceValuesResponse(year, month, serviceValues);
    }

    /**
     * Check if a service is active for a specific month.
     */
    /**
     * Normalize service name to keyword format.
     * - Convert to lowercase
     * - Replace spaces with underscores
     * - Remove diacritics (ă -> a, î -> i, etc.)
     * - Remove special characters (keep only letters, numbers, and underscores)
     */
    private String normalizeToKeyword(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return "";
        }
        
        // Remove diacritics (ă -> a, î -> i, etc.)
        String normalized = java.text.Normalizer.normalize(serviceName, java.text.Normalizer.Form.NFD);
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
    
    private boolean isServiceActiveForMonth(ActiveService activeService, Date monthStart, Date monthEnd) {
        Date activeFrom = activeService.getActiveFrom();
        Date activeUntil = activeService.getActiveUntil();

        if (activeFrom == null) {
            return false;
        }

        // Service must start before or during the month
        if (activeFrom.after(monthEnd)) {
            return false;
        }

        // If activeUntil is set, it must be after or during the month
        if (activeUntil != null && activeUntil.before(monthStart)) {
            return false;
        }

        return true;
    }

    /**
     * Calculate service value for a specific month.
     * Priority: customMonthlyCost > defaultMonthlyCost > formula
     */
    private Double calculateServiceValue(ActiveService activeService,
                                         Service service,
                                         TenantRentalData rentalData,
                                         List<IndexCounter> counters,
                                         int year,
                                         int month,
                                         Map<Long, Map<Integer, Double>> allServiceValues) {
        // 1. Use custom monthly cost if provided
        if (activeService.getCustomMonthlyCost() != null) {
            return activeService.getCustomMonthlyCost();
        }

        // 2. Use default monthly cost if provided
        if (service.getDefaultMonthlyCost() != null) {
            return service.getDefaultMonthlyCost();
        }

        // 3. Calculate from formula if provided
        if (service.getFormula() != null && service.getFormula().getExpression() != null) {
            // Build service values map for this month
            Map<Long, Double> serviceValuesForMonth = new HashMap<>();
            if (allServiceValues != null) {
                for (Map.Entry<Long, Map<Integer, Double>> entry : allServiceValues.entrySet()) {
                    // Skip current service to avoid circular dependency
                    if (!entry.getKey().equals(activeService.getServiceId())) {
                        Map<Integer, Double> monthValues = entry.getValue();
                        if (monthValues != null && monthValues.containsKey(month)) {
                            serviceValuesForMonth.put(entry.getKey(), monthValues.get(month));
                        }
                    }
                }
            }
            // Use ExcelReportService to evaluate formula
            // Build service keyword map for formula evaluation
            Map<String, Long> serviceKeywordsForMonth = new HashMap<>();
            if (allServiceValues != null) {
                for (Long serviceId : allServiceValues.keySet()) {
                    if (!serviceId.equals(activeService.getServiceId())) {
                        Service otherService = serviceRepository.findById(serviceId)
                            .orElse(null);
                        if (otherService != null && otherService.getName() != null) {
                            String keyword = normalizeToKeyword(otherService.getName());
                            if (keyword != null && !keyword.isEmpty()) {
                                serviceKeywordsForMonth.put(keyword, serviceId);
                            }
                        }
                    }
                }
            }
            
            return excelReportService.evaluateFormula(
                service.getFormula().getExpression(),
                rentalData,
                counters,
                year,
                month,
                serviceValuesForMonth,
                serviceKeywordsForMonth
            );
        }

        return 0.0;
    }

    /**
     * Get services for a rental agreement with resolved includeInReport values.
     * Returns services with includeInReport calculated from service defaults if not explicitly set.
     */
    public GetServicesResponse getServices(Long rentalAgreementId) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
            .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        // Load all services referenced by active services
        Map<Long, Service> serviceMap = new HashMap<>();
        for (ActiveService activeService : trd.getActiveServices()) {
            if (!serviceMap.containsKey(activeService.getServiceId())) {
                Service service = serviceRepository.findById(activeService.getServiceId())
                    .orElse(null);
                if (service != null) {
                    serviceMap.put(activeService.getServiceId(), service);
                }
            }
        }

        // Convert active services to DTOs with resolved includeInReport
        List<ServiceWithResolvedIncludeInReport> services = new ArrayList<>();
        for (ActiveService activeService : trd.getActiveServices()) {
            Service service = serviceMap.get(activeService.getServiceId());
            ServiceWithResolvedIncludeInReport serviceDto = ServiceWithResolvedIncludeInReport.fromActiveService(
                activeService, service);
            services.add(serviceDto);
        }

        return new GetServicesResponse(rentalAgreementId, services);
    }
    
    /**
     * Safely convert java.util.Date or java.sql.Date to LocalDate.
     * java.sql.Date does not support toInstant() directly, so we need special handling.
     */
    private LocalDate dateToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            // java.sql.Date has toLocalDate() method
            return ((java.sql.Date) date).toLocalDate();
        } else {
            // java.util.Date - use toInstant()
            return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        }
    }
}
