package com.work.total_app.services;

import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.Location;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.building.Room;
import com.work.total_app.models.reading.CounterType;
import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.reading.IndexData;
import com.work.total_app.models.tenant.ActiveService;
import com.work.total_app.models.tenant.ServiceMonthlyValue;
import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.models.tenant.TenantRentalData;
import com.work.total_app.repositories.TenantRentalDataRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Log4j2
public class ExcelReportService {

    @Autowired
    private TenantRentalDataRepository rentalDataRepository;

    @Autowired
    private com.work.total_app.repositories.ServiceRepository serviceRepository;

    @Autowired
    private com.work.total_app.repositories.ServiceMonthlyValueRepository serviceMonthlyValueRepository;
    
    // No need for a singleton - exp4j is lightweight and thread-safe

    private static final String[] MONTHS = {
        "IANUARIE", "FEBRUARIE", "MARTIE", "APRILIE", "MAI", "IUNIE",
        "IULIE", "AUGUST", "SEPTEMBRIE", "OCTOMBRIE", "NOIEMBRIE", "DECEMBRIE"
    };

    /**
     * Generate Excel report for a specific rental agreement and year.
     * Creates one sheet per year with monthly consumption data.
     */
    public byte[] generateConsumptionReport(Long rentalAgreementId, int year) throws IOException {
        TenantRentalData rentalData = rentalDataRepository.findById(rentalAgreementId)
            .orElseThrow(() -> new RuntimeException("Rental agreement not found"));
        
        Workbook workbook = new XSSFWorkbook();
        
        // Create sheet for the specified year
        Sheet sheet = workbook.createSheet("ANUL " + year);
        
        // Get all counters for the rental space
        RentalSpace space = rentalData.getRentalSpace();
        List<IndexCounter> counters = space.getCounters();
        
        // Build report structure
        createReportHeader(workbook, sheet, rentalData.getTenant(), rentalData, space);
        createConsumptionTable(workbook, sheet, counters, year, rentalData);
        
        // Auto-size columns
        for (int i = 0; i < 15; i++) {
            sheet.autoSizeColumn(i);
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream.toByteArray();
    }

    /**
     * Generate multi-year report with one sheet per year.
     */
    public byte[] generateMultiYearReport(Long rentalAgreementId, int startYear, int endYear) throws IOException {
        TenantRentalData rentalData = rentalDataRepository.findById(rentalAgreementId)
            .orElseThrow(() -> new RuntimeException("Rental agreement not found"));
        
        Workbook workbook = new XSSFWorkbook();
        
        RentalSpace space = rentalData.getRentalSpace();
        List<IndexCounter> counters = space.getCounters();
        
        // Create one sheet per year
        for (int year = startYear; year <= endYear; year++) {
            Sheet sheet = workbook.createSheet("ANUL " + year);
            createReportHeader(workbook, sheet, rentalData.getTenant(), rentalData, space);
            createConsumptionTable(workbook, sheet, counters, year, rentalData);
            
            // Auto-size columns
            for (int i = 0; i < 15; i++) {
                sheet.autoSizeColumn(i);
            }
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream.toByteArray();
    }

    private void createReportHeader(Workbook workbook, Sheet sheet, Tenant tenant, 
                                     TenantRentalData rentalData, RentalSpace space) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);

        // Row 1: Building/Company name (would need to be fetched)
        Row row0 = sheet.createRow(0);
        Cell cell0 = row0.createCell(0);
        cell0.setCellValue(space.getBuilding() != null ? space.getBuilding().getName() : "");
        cell0.setCellStyle(headerStyle);

        // Row 2: CUI
        Row row1 = sheet.createRow(1);
        Cell cell1 = row1.createCell(0);
        cell1.setCellValue("CUI " + (tenant.getCui() != null ? tenant.getCui() : ""));
        
        // Tenant info on the right
        Cell tenantCell = row1.createCell(8);
        tenantCell.setCellValue("Chirias");
        tenantCell.setCellStyle(headerStyle);
        
        Cell tenantNameCell = row1.createCell(9);
        tenantNameCell.setCellValue(tenant.getName());
        
        // Row 3: Contract info
        Row row2 = sheet.createRow(2);
        Cell contractCell = row2.createCell(8);
        contractCell.setCellValue("Contract nr.");
        contractCell.setCellStyle(headerStyle);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        // Use contractNumber if available, otherwise leave empty
        String contractNumber = rentalData.getContractNumber() != null && !rentalData.getContractNumber().isEmpty()
            ? rentalData.getContractNumber()
            : "";
        String contractDateStr = rentalData.getContractDate() != null
            ? dateFormat.format(rentalData.getContractDate())
            : "";
        
        String contractInfo = contractNumber;
        if (!contractNumber.isEmpty() && !contractDateStr.isEmpty()) {
            contractInfo = contractNumber + "/" + contractDateStr;
        } else if (!contractDateStr.isEmpty()) {
            contractInfo = contractDateStr;
        }
        
        Cell contractValueCell = row2.createCell(9);
        contractValueCell.setCellValue(contractInfo);
        
        // Row 4: Space info
        Row row3 = sheet.createRow(3);
        Cell spaceCell = row3.createCell(8);
        spaceCell.setCellValue(space.getName());
        Cell spaceDetailCell = row3.createCell(9);
        spaceDetailCell.setCellValue(space.getOfficialName() != null ? space.getOfficialName() : "");
        
        // Empty row
        sheet.createRow(4);
    }

    private void createConsumptionTable(Workbook workbook, Sheet sheet, List<IndexCounter> counters, int year, TenantRentalData rentalData) {
        int currentRow = 5;
        
        // Title row
        Row titleRow = sheet.createRow(currentRow++);
        CellStyle titleStyle = createCenterBoldStyle(workbook);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ANEXA FACTURA");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, 0, 13));
        
        // Empty row
        sheet.createRow(currentRow++);
        
        // Header row with months
        Row headerRow = sheet.createRow(currentRow++);
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        headerRow.createCell(0).setCellValue("");
        headerRow.createCell(1).setCellValue("");
        
        for (int i = 0; i < MONTHS.length; i++) {
            Cell cell = headerRow.createCell(i + 2);
            cell.setCellValue(MONTHS[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows for each counter type
        Map<CounterType, IndexCounter> counterMap = counters.stream()
            .collect(Collectors.toMap(IndexCounter::getCounterType, c -> c, (c1, c2) -> c1));
        
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        
        // E-ON (Electricity)
        if (counterMap.containsKey(CounterType.ELECTRICITY_220) || counterMap.containsKey(CounterType.ELECTRICITY_380)) {
            IndexCounter elecCounter = counterMap.getOrDefault(CounterType.ELECTRICITY_220, 
                                                              counterMap.get(CounterType.ELECTRICITY_380));
            if (elecCounter != null) {
                createCounterRows(sheet, currentRow, "E-ON", "kw", "lei", elecCounter, year, numberStyle);
                currentRow += 2;
            }
        }
        
        // APA (Water)
        if (counterMap.containsKey(CounterType.WATER)) {
            createCounterRows(sheet, currentRow, "APA", "mc", "lei", counterMap.get(CounterType.WATER), year, numberStyle);
            currentRow += 2;
        }
        
        // GAZ (Gas)
        if (counterMap.containsKey(CounterType.GAS)) {
            createCounterRows(sheet, currentRow, "GAZ", "mc", "lei", counterMap.get(CounterType.GAS), year, numberStyle);
            currentRow += 2;
        }
        
        // Load custom monthly values for this rental agreement and year
        Map<String, Double> customValuesMap = new HashMap<>();
        List<ServiceMonthlyValue> customValues = 
            serviceMonthlyValueRepository.findByRentalDataIdAndYear(rentalData.getId(), year);
        for (ServiceMonthlyValue customValue : customValues) {
            String key = customValue.getServiceId() + "_" + customValue.getMonth();
            customValuesMap.put(key, customValue.getCustomValue());
        }
        
        // Build service keyword map (service name -> service ID) for formula evaluation
        // Include all services that should be in report (from activeServices + implicit services)
        Map<String, Long> serviceKeywords = new HashMap<>();
        
        // Add keywords for services in activeServices
        for (ActiveService activeService : rentalData.getActiveServices()) {
            com.work.total_app.models.service.Service service = serviceRepository.findById(activeService.getServiceId())
                .orElse(null);
            if (service != null && service.getName() != null) {
                // Normalize service name to keyword
                String keyword = normalizeToKeyword(service.getName());
                if (keyword != null && !keyword.isEmpty()) {
                    serviceKeywords.put(keyword, service.getId());
                }
            }
        }
        
        // Add keywords for implicit services (defaultIncludeInReport = true, not in activeServices or IMPLICIT)
        List<com.work.total_app.models.service.Service> allServices = serviceRepository.findAll();
        for (com.work.total_app.models.service.Service service : allServices) {
            if (service.getActive() != null && !service.getActive()) {
                continue; // Skip inactive services
            }
            
            // Check if service should be included implicitly
            if (service.getDefaultIncludeInReport() != null && service.getDefaultIncludeInReport()) {
                // Check if service is in activeServices
                ActiveService activeService = rentalData.getActiveServices().stream()
                    .filter(as -> as.getServiceId().equals(service.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (activeService == null) {
                    // Service not in activeServices but has defaultIncludeInReport = true → include it
                    String keyword = normalizeToKeyword(service.getName());
                    if (keyword != null && !keyword.isEmpty()) {
                        serviceKeywords.put(keyword, service.getId());
                    }
                } else {
                    // Service is in activeServices, check if it's IMPLICIT (includeInReport == null)
                    if (activeService.getIncludeInReport() == null) {
                        // IMPLICIT → already added above, but ensure keyword is there
                        String keyword = normalizeToKeyword(service.getName());
                        if (keyword != null && !keyword.isEmpty()) {
                            serviceKeywords.put(keyword, service.getId());
                        }
                    }
                    // If includeInReport is false (MANUAL_OFF), don't add keyword
                    // If includeInReport is true (MANUAL_ON), already added above
                }
            }
        }
        
        // Get all services that should be included in report (from activeServices + implicit services)
        List<ServiceToInclude> servicesToInclude = getServicesToInclude(rentalData);
        
        // Calculate service values for all services that should be included (needed for formula dependencies)
        // We need to calculate in multiple passes to handle dependencies
        Map<Long, Map<Integer, Double>> allServiceValues = new HashMap<>();
        
        // First pass: calculate services without formulas or with simple formulas
        // Second pass: calculate services with dependencies on other services
        for (int pass = 0; pass < 3; pass++) { // Max 3 passes to handle dependencies
            boolean allCalculated = true;
            for (ServiceToInclude serviceToInclude : servicesToInclude) {
                com.work.total_app.models.service.Service service = serviceToInclude.service;
                ActiveService activeService = serviceToInclude.activeService;
                
                if (service == null) continue;
                
                if (!allServiceValues.containsKey(service.getId())) {
                    Map<Integer, Double> monthValues = new HashMap<>();
                    boolean canCalculate = true;
                    
                    // Check if this service depends on other services
                    if (service.getFormula() != null && service.getFormula().getExpression() != null) {
                        String expr = service.getFormula().getExpression();
                        // Check if formula references other services (both legacy format and keyword format)
                        for (ServiceToInclude otherServiceToInclude : servicesToInclude) {
                            if (!otherServiceToInclude.service.getId().equals(service.getId())) {
                                com.work.total_app.models.service.Service otherServiceEntity = otherServiceToInclude.service;
                                
                                // Check legacy format (service_1, service_2, etc.)
                                if (expr.contains("service_" + otherServiceEntity.getId())) {
                                    if (!allServiceValues.containsKey(otherServiceEntity.getId())) {
                                        canCalculate = false;
                                        break;
                                    }
                                }
                                
                                // Check keyword format (salubrizare, cota_intretinere, etc.)
                                if (otherServiceEntity.getName() != null) {
                                    String keyword = normalizeToKeyword(otherServiceEntity.getName());
                                    if (keyword != null && !keyword.isEmpty() && expr.contains(keyword)) {
                                        if (!allServiceValues.containsKey(otherServiceEntity.getId())) {
                                            canCalculate = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (canCalculate || pass > 0) {
                        for (int month = 0; month < 12; month++) {
                            Calendar cal = Calendar.getInstance();
                            cal.set(year, month, 1, 0, 0, 0);
                            cal.set(Calendar.MILLISECOND, 0);
                            Date monthStart = cal.getTime();
                            
                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                            cal.set(Calendar.HOUR_OF_DAY, 23);
                            cal.set(Calendar.MINUTE, 59);
                            cal.set(Calendar.SECOND, 59);
                            Date monthEnd = cal.getTime();
                            
                            // Check if service is active for this month
                            boolean isActive;
                            if (activeService != null) {
                                // Service from activeServices - check activeFrom/activeUntil
                                isActive = isServiceActiveForMonth(activeService, monthStart, monthEnd);
                            } else {
                                // Implicit service - active for entire rental period
                                isActive = isMonthInRentalPeriod(rentalData, monthStart, monthEnd);
                            }
                            
                            if (isActive) {
                                // Check for custom value first
                                String customKey = service.getId() + "_" + month;
                                if (customValuesMap.containsKey(customKey)) {
                                    monthValues.put(month, customValuesMap.get(customKey));
                                } else {
                                    // Calculate value (need to create a temporary ActiveService for implicit services)
                                    ActiveService tempActiveService = activeService;
                                    if (tempActiveService == null) {
                                        // Create temporary ActiveService for implicit service
                                        tempActiveService = new ActiveService();
                                        tempActiveService.setServiceId(service.getId());
                                        tempActiveService.setActiveFrom(rentalData.getStartDate());
                                        tempActiveService.setActiveUntil(rentalData.getEndDate());
                                        tempActiveService.setIncludeInReport(null); // IMPLICIT
                                    }
                                    Double value = calculateServiceValue(tempActiveService, service, rentalData, counters, year, month, allServiceValues);
                                    monthValues.put(month, value);
                                }
                            } else {
                                monthValues.put(month, 0.0);
                            }
                        }
                        allServiceValues.put(service.getId(), monthValues);
                    } else {
                        allCalculated = false;
                    }
                }
            }
            if (allCalculated) break;
        }
        
        // Create rows for all services that should be included in report
        for (ServiceToInclude serviceToInclude : servicesToInclude) {
            com.work.total_app.models.service.Service service = serviceToInclude.service;
            ActiveService activeService = serviceToInclude.activeService;
            
            if (service == null) {
                log.warn("Service is null in servicesToInclude");
                continue;
            }
            
            Map<Integer, Double> monthValues = allServiceValues.get(service.getId());
            
            // If service values were not calculated, calculate them now
            if (monthValues == null) {
                log.debug("Service {} not found in allServiceValues, calculating values now", service.getId());
                monthValues = new HashMap<>();
                // Calculate values for all months
                for (int month = 0; month < 12; month++) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(year, month, 1, 0, 0, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date monthStart = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date monthEnd = cal.getTime();
                    
                    // Check if service is active for this month
                    boolean isActive;
                    if (activeService != null) {
                        // Service from activeServices - check activeFrom/activeUntil
                        isActive = isServiceActiveForMonth(activeService, monthStart, monthEnd);
                    } else {
                        // Implicit service - active for entire rental period
                        isActive = isMonthInRentalPeriod(rentalData, monthStart, monthEnd);
                    }
                    
                    if (isActive) {
                        // Check for custom value first
                        String customKey = service.getId() + "_" + month;
                        if (customValuesMap.containsKey(customKey)) {
                            monthValues.put(month, customValuesMap.get(customKey));
                        } else {
                            // Calculate value (need to create a temporary ActiveService for implicit services)
                            ActiveService tempActiveService = activeService;
                            if (tempActiveService == null) {
                                // Create temporary ActiveService for implicit service
                                tempActiveService = new ActiveService();
                                tempActiveService.setServiceId(service.getId());
                                tempActiveService.setActiveFrom(rentalData.getStartDate());
                                tempActiveService.setActiveUntil(rentalData.getEndDate());
                                tempActiveService.setIncludeInReport(null); // IMPLICIT
                            }
                            Double value = calculateServiceValue(tempActiveService, service, rentalData, counters, year, month, allServiceValues);
                            monthValues.put(month, value != null ? value : 0.0);
                        }
                    } else {
                        monthValues.put(month, 0.0);
                    }
                }
                // Add to allServiceValues for potential future use
                allServiceValues.put(service.getId(), monthValues);
            }
            
            // Create temporary ActiveService for implicit services when creating row
            ActiveService rowActiveService = activeService;
            if (rowActiveService == null) {
                // Create temporary ActiveService for implicit service
                rowActiveService = new ActiveService();
                rowActiveService.setServiceId(service.getId());
                rowActiveService.setActiveFrom(rentalData.getStartDate());
                rowActiveService.setActiveUntil(rentalData.getEndDate());
                rowActiveService.setIncludeInReport(null); // IMPLICIT
            }
            
            createActiveServiceRow(sheet, currentRow, rowActiveService, service, rentalData, counters, year, numberStyle, monthValues);
            currentRow++;
        }
    }

    /**
     * Helper class to represent a service that should be included in the report.
     * Can be from activeServices or an implicit service (defaultIncludeInReport = true).
     */
    private static class ServiceToInclude {
        com.work.total_app.models.service.Service service;
        ActiveService activeService; // null for implicit services
        
        ServiceToInclude(com.work.total_app.models.service.Service service, ActiveService activeService) {
            this.service = service;
            this.activeService = activeService;
        }
    }
    
    /**
     * Get all services that should be included in the report.
     * Includes:
     * 1. Services from activeServices that are MANUAL_ON (includeInReport == true)
     * 2. Services from activeServices that are IMPLICIT (includeInReport == null) 
     *    and have defaultIncludeInReport == true at general level
     * 3. Services that are NOT in activeServices but have defaultIncludeInReport == true at general level
     * 
     * Excludes:
     * - Services from activeServices that are MANUAL_OFF (includeInReport == false)
     * - Services from activeServices that are IMPLICIT but have defaultIncludeInReport == false
     */
    private List<ServiceToInclude> getServicesToInclude(TenantRentalData rentalData) {
        List<ServiceToInclude> result = new ArrayList<>();
        Set<Long> processedServiceIds = new HashSet<>();
        
        // First, add services from activeServices that should be included
        for (ActiveService activeService : rentalData.getActiveServices()) {
            com.work.total_app.models.service.Service service = serviceRepository.findById(activeService.getServiceId())
                .orElse(null);
            
            if (service == null) {
                continue;
            }
            
            // Check if service should be included in report
            Boolean includeInReport = activeService.getIncludeInReport();
            if (includeInReport == null) {
                // IMPLICIT - use service default (defaultIncludeInReport from general level)
                includeInReport = service.getDefaultIncludeInReport() != null ? service.getDefaultIncludeInReport() : false;
            }
            
            // Add if MANUAL_ON (includeInReport == true) or IMPLICIT with defaultIncludeInReport == true
            // Exclude if MANUAL_OFF (includeInReport == false) or IMPLICIT with defaultIncludeInReport == false
            if (includeInReport != null && includeInReport) {
                result.add(new ServiceToInclude(service, activeService));
                processedServiceIds.add(service.getId());
            }
        }
        
        // Then, add implicit services (defaultIncludeInReport = true) that are NOT in activeServices
        // These are services that should be included by default but haven't been explicitly added to the rental agreement
        List<com.work.total_app.models.service.Service> allServices = serviceRepository.findAll();
        for (com.work.total_app.models.service.Service service : allServices) {
            if (service.getActive() != null && !service.getActive()) {
                continue; // Skip inactive services
            }
            
            // Skip if already processed (already in activeServices)
            if (processedServiceIds.contains(service.getId())) {
                continue;
            }
            
            // Check if service should be included implicitly (defaultIncludeInReport == true at general level)
            if (service.getDefaultIncludeInReport() != null && service.getDefaultIncludeInReport()) {
                // Service not in activeServices but has defaultIncludeInReport = true → include it implicitly
                result.add(new ServiceToInclude(service, null));
            }
        }
        
        return result;
    }
    
    /**
     * Helper method to convert Date (java.util.Date or java.sql.Date) to LocalDate.
     * Handles both types safely.
     */
    private java.time.LocalDate dateToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            // java.sql.Date has toLocalDate() method
            return ((java.sql.Date) date).toLocalDate();
        } else {
            // java.util.Date - use toInstant()
            return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        }
    }
    
    /**
     * Check if a month is within the rental period.
     * Used for implicit services that don't have activeFrom/activeUntil.
     * A month is considered in the rental period if it overlaps with the rental period.
     */
    private boolean isMonthInRentalPeriod(TenantRentalData rentalData, Date monthStart, Date monthEnd) {
        if (rentalData.getStartDate() == null) {
            return false;
        }
        
        // Normalize dates to LocalDate for comparison
        java.time.LocalDate startDateLocal = dateToLocalDate(rentalData.getStartDate());
        java.time.LocalDate monthStartLocal = dateToLocalDate(monthStart);
        java.time.LocalDate monthEndLocal = dateToLocalDate(monthEnd);
        
        // If no end date, month must start on or after rental start
        if (rentalData.getEndDate() == null) {
            return !monthStartLocal.isBefore(startDateLocal);
        }
        
        java.time.LocalDate endDateLocal = dateToLocalDate(rentalData.getEndDate());
        
        // Check if month overlaps with rental period
        // Month overlaps if: monthStart <= endDate AND monthEnd >= startDate
        return !monthStartLocal.isAfter(endDateLocal) && !monthEndLocal.isBefore(startDateLocal);
    }

    private void createCounterRows(Sheet sheet, int rowIndex, String serviceName, String unit1, String unit2,
                                   IndexCounter counter, int year, CellStyle numberStyle) {
        // Row for consumption (kw, mc, etc.)
        Row consumptionRow = sheet.createRow(rowIndex);
        consumptionRow.createCell(0).setCellValue(serviceName);
        consumptionRow.createCell(1).setCellValue(unit1);
        
        // Row for cost (lei)
        Row costRow = sheet.createRow(rowIndex + 1);
        costRow.createCell(1).setCellValue(unit2);
        
        // Fill in monthly data
        for (int month = 0; month < 12; month++) {
            MonthlyConsumption mc = getMonthlyConsumption(counter, year, month);
            
            Cell consumptionCell = consumptionRow.createCell(month + 2);
            if (mc.consumption > 0) {
                consumptionCell.setCellValue(mc.consumption);
                consumptionCell.setCellStyle(numberStyle);
            }
            
            Cell costCell = costRow.createCell(month + 2);
            if (mc.cost > 0) {
                costCell.setCellValue(mc.cost);
                costCell.setCellStyle(numberStyle);
            }
        }
    }


    /**
     * Create a row for an active service.
     * For each month, uses the pre-calculated value or custom value.
     */
    private void createActiveServiceRow(Sheet sheet, int rowIndex, 
                                        com.work.total_app.models.tenant.ActiveService activeService,
                                        com.work.total_app.models.service.Service service,
                                        TenantRentalData rentalData, 
                                        List<IndexCounter> counters,
                                        int year, 
                                        CellStyle numberStyle,
                                        Map<Integer, Double> monthValues) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(service.getName());
        // Use unit of measure from service
        row.createCell(1).setCellValue(service.getUnitOfMeasure() != null ? service.getUnitOfMeasure() : "");
        
        // For each month, use pre-calculated value
        for (int month = 0; month < 12; month++) {
            Cell cell = row.createCell(month + 2);
            Double value = monthValues != null ? monthValues.getOrDefault(month, 0.0) : 0.0;
            cell.setCellValue(value != null ? value : 0.0);
            cell.setCellStyle(numberStyle);
        }
    }

    /**
     * Check if a service is active for a specific month.
     * Normalizes dates to start of day for comparison to avoid timezone issues.
     */
    private boolean isServiceActiveForMonth(com.work.total_app.models.tenant.ActiveService activeService, 
                                           Date monthStart, Date monthEnd) {
        Date activeFrom = activeService.getActiveFrom();
        Date activeUntil = activeService.getActiveUntil();
        
        if (activeFrom == null) {
            return false;
        }
        
        // Normalize dates to LocalDate for comparison (ignore time and timezone)
        java.time.LocalDate activeFromLocal = dateToLocalDate(activeFrom);
        java.time.LocalDate monthStartLocal = dateToLocalDate(monthStart);
        java.time.LocalDate monthEndLocal = dateToLocalDate(monthEnd);
        
        // Service must start before or during the month
        if (activeFromLocal.isAfter(monthEndLocal)) {
            return false;
        }
        
        // If activeUntil is set, it must be after or during the month
        if (activeUntil != null) {
            java.time.LocalDate activeUntilLocal = dateToLocalDate(activeUntil);
            if (activeUntilLocal.isBefore(monthStartLocal)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Calculate service value for a specific month.
     * Priority: customMonthlyCost > defaultMonthlyCost > formula
     * 
     * @param allServiceValues Map of serviceId -> Map of month -> value (for formula dependencies)
     */
    private Double calculateServiceValue(com.work.total_app.models.tenant.ActiveService activeService,
                                         com.work.total_app.models.service.Service service,
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
            
            // Build service keyword map for formula evaluation
            Map<String, Long> serviceKeywordsForMonth = new HashMap<>();
            if (allServiceValues != null) {
                for (Long serviceId : allServiceValues.keySet()) {
                    if (!serviceId.equals(activeService.getServiceId())) {
                        com.work.total_app.models.service.Service otherService = serviceRepository.findById(serviceId)
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
            
            return evaluateFormula(service.getFormula().getExpression(), rentalData, counters, year, month, 
                serviceValuesForMonth, serviceKeywordsForMonth);
        }
        
        return 0.0;
    }

    /**
     * Evaluate a formula expression.
     * Supported variables: 
     * - rent, waterConsumption, gasConsumption, electricityConsumption220V, electricityConsumption380V
     * - costConsumApa, costConsumGaz, costConsumCurent220V, costConsumCurent380V
     * - service_1, service_2, ... (other services by ID - legacy format)
     * - salubrizare, cota_intretinere, ... (service keywords - normalized service names)
     * Supported operators: +, -, *, /, (, )
     */
    public Double evaluateFormula(String expression, TenantRentalData rentalData, 
                                   List<IndexCounter> counters, int year, int month,
                                   Map<Long, Double> serviceValues) {
        return evaluateFormula(expression, rentalData, counters, year, month, serviceValues, null);
    }
    
    /**
     * Evaluate a formula expression with service keyword map.
     * Supported variables: 
     * - rent, waterConsumption, gasConsumption, electricityConsumption220V, electricityConsumption380V
     * - costConsumApa, costConsumGaz, costConsumCurent220V, costConsumCurent380V
     * - service_1, service_2, ... (other services by ID - legacy format)
     * - salubrizare, cota_intretinere, ... (service keywords - normalized service names)
     * Supported operators: +, -, *, /, (, )
     */
    public Double evaluateFormula(String expression, TenantRentalData rentalData, 
                                   List<IndexCounter> counters, int year, int month,
                                   Map<Long, Double> serviceValues,
                                   Map<String, Long> serviceKeywords) {
        try {
            // Get variable values
            double rent = rentalData.getRent() != null ? rentalData.getRent() : 0.0;
            
            // Calculate consumptions and costs for the month
            double waterConsumption = 0.0;
            double gasConsumption = 0.0;
            double electricityConsumption220V = 0.0;
            double electricityConsumption380V = 0.0;
            double costConsumApa = 0.0;
            double costConsumGaz = 0.0;
            double costConsumCurent220V = 0.0;
            double costConsumCurent380V = 0.0;
            
            for (IndexCounter counter : counters) {
                MonthlyConsumption mc = getMonthlyConsumption(counter, year, month);
                
                switch (counter.getCounterType()) {
                    case WATER:
                        waterConsumption += mc.consumption;
                        costConsumApa += mc.cost;
                        break;
                    case GAS:
                        gasConsumption += mc.consumption;
                        costConsumGaz += mc.cost;
                        break;
                    case ELECTRICITY_220:
                        electricityConsumption220V += mc.consumption;
                        costConsumCurent220V += mc.cost;
                        break;
                    case ELECTRICITY_380:
                        electricityConsumption380V += mc.consumption;
                        costConsumCurent380V += mc.cost;
                        break;
                }
            }
            
            // Replace variables in expression
            String expr = expression
                .replaceAll("\\brent\\b", String.valueOf(rent))
                .replaceAll("\\bwaterConsumption\\b", String.valueOf(waterConsumption))
                .replaceAll("\\bgasConsumption\\b", String.valueOf(gasConsumption))
                .replaceAll("\\belectricityConsumption220V\\b", String.valueOf(electricityConsumption220V))
                .replaceAll("\\belectricityConsumption380V\\b", String.valueOf(electricityConsumption380V))
                .replaceAll("\\bcostConsumApa\\b", String.valueOf(costConsumApa))
                .replaceAll("\\bcostConsumGaz\\b", String.valueOf(costConsumGaz))
                .replaceAll("\\bcostConsumCurent220V\\b", String.valueOf(costConsumCurent220V))
                .replaceAll("\\bcostConsumCurent380V\\b", String.valueOf(costConsumCurent380V));
            
            // Replace service variables - support both legacy format (service_1) and keyword format (salubrizare)
            if (serviceValues != null) {
                // First, try to replace by keyword if serviceKeywords map is provided
                if (serviceKeywords != null) {
                    for (Map.Entry<String, Long> keywordEntry : serviceKeywords.entrySet()) {
                        String keyword = keywordEntry.getKey();
                        Long serviceId = keywordEntry.getValue();
                        if (serviceValues.containsKey(serviceId)) {
                            // Replace keyword with service value
                            expr = expr.replaceAll("\\b" + java.util.regex.Pattern.quote(keyword) + "\\b", 
                                String.valueOf(serviceValues.get(serviceId)));
                        }
                    }
                }
                
                // Then, replace legacy format (service_1, service_2, etc.) for backward compatibility
                for (Map.Entry<Long, Double> entry : serviceValues.entrySet()) {
                    String serviceVar = "service_" + entry.getKey();
                    expr = expr.replaceAll("\\b" + java.util.regex.Pattern.quote(serviceVar) + "\\b", 
                        String.valueOf(entry.getValue()));
                }
            }
            
            // Simple expression evaluator using exp4j (mathematical expression evaluator)
            // exp4j is lightweight, thread-safe, and perfect for simple mathematical formulas
            // If any variables remain in the expression (not replaced), replace them with 0
            // This handles cases where a service variable is missing
            // Pattern matches identifiers (letters, underscores) that might be variables
            expr = expr.replaceAll("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b", "0");
            
            try {
                net.objecthunter.exp4j.Expression exp4jExpression = new net.objecthunter.exp4j.ExpressionBuilder(expr)
                    .build();
                
                double result = exp4jExpression.evaluate();
                return result;
            } catch (Exception e) {
                log.warn("Error evaluating formula '{}' (processed: '{}'): {}", expression, expr, e.getMessage());
                return 0.0;
            }
        } catch (Exception e) {
            log.warn("Error evaluating formula '{}': {}", expression, e.getMessage());
            return 0.0;
        }
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
    
    private MonthlyConsumption getMonthlyConsumption(IndexCounter counter, int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date monthStart = cal.getTime();
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date monthEnd = cal.getTime();
        
        List<IndexData> monthReadings = counter.getIndexData().stream()
            .filter(r -> !r.getReadingDate().before(monthStart) && !r.getReadingDate().after(monthEnd))
            .sorted(Comparator.comparing(IndexData::getReadingDate))
            .toList();
        
        if (monthReadings.isEmpty()) {
            return new MonthlyConsumption(0, 0);
        }
        
        double consumption = monthReadings.stream()
            .filter(r -> r.getConsumption() != null)
            .mapToDouble(IndexData::getConsumption)
            .sum();
        
        double cost = monthReadings.stream()
            .filter(r -> r.getTotalCost() != null)
            .mapToDouble(IndexData::getTotalCost)
            .sum();
        
        return new MonthlyConsumption(consumption, cost);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11); // Mărime font pentru header-e
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCenterBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14); // Mărime font pentru titlu
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(false);
        font.setFontHeightInPoints((short) 10); // Mărime font pentru date normale
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(false);
        font.setFontHeightInPoints((short) 10); // Mărime font pentru numere
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00"));
        return style;
    }

    private String getColumnLetter(int columnIndex) {
        StringBuilder column = new StringBuilder();
        while (columnIndex >= 0) {
            column.insert(0, (char) ('A' + (columnIndex % 26)));
            columnIndex = (columnIndex / 26) - 1;
        }
        return column.toString();
    }

    private static class MonthlyConsumption {
        double consumption;
        double cost;
        
        MonthlyConsumption(double consumption, double cost) {
            this.consumption = consumption;
            this.cost = cost;
        }
    }

    /**
     * Generate Excel report with counters history.
     * Creates separate sheets per counter type per year.
     * Format: 1 row per space, columns for each month (index + consumption)
     * Example sheets: "APA 2025", "GAZ 2025", "CURENT 220V 2025"
     * 
     * @param counters List of counters to include
     * @param startYear Starting year (inclusive)
     * @param endYear Ending year (inclusive), if null uses startYear only
     * @return Excel file as byte array
     */
    public byte[] generateCountersHistoryReport(List<IndexCounter> counters, int startYear, Integer endYear) throws IOException {
        if (endYear == null) {
            endYear = startYear;
        }
        
        Workbook workbook = new XSSFWorkbook();
        
        // Group counters by type
        Map<CounterType, List<IndexCounter>> countersByType = counters.stream()
            .collect(Collectors.groupingBy(IndexCounter::getCounterType));
        
        // For each year and counter type, create a sheet
        for (int year = startYear; year <= endYear; year++) {
            for (CounterType counterType : CounterType.values()) {
                List<IndexCounter> typeCounters = countersByType.getOrDefault(counterType, new ArrayList<>());
                
                if (typeCounters.isEmpty()) {
                    continue; // Skip if no counters of this type
                }
                
                // Create sheet name
                String sheetName = getCounterTypeDisplayName(counterType) + " " + year;
                Sheet sheet = workbook.createSheet(sheetName);
                
                // Create the history table for this type and year (new format)
                createHistoryTableGrouped(workbook, sheet, typeCounters, year, counterType);
                
                // Auto-size columns
                for (int i = 0; i <= 25; i++) { // 2 columns (zone, space) + 24 columns (12 months × 2)
                    sheet.autoSizeColumn(i);
                }
            }
        }
        
        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream.toByteArray();
    }
    
    /**
     * Create a table with counter readings history grouped by building/zone.
     * Format: 1 row per space, columns for each month (index + consumption).
     * Includes Total rows per zone with Excel formulas.
     */
    private void createHistoryTableGrouped(Workbook workbook, Sheet sheet, List<IndexCounter> counters, 
                                          int year, CounterType counterType) {
        int rowNum = 0;
        
        // Title row
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ISTORIC CITIRI CONTORI - " + getCounterTypeDisplayName(counterType) + " " + year);
        CellStyle titleStyle = createCenterBoldStyle(workbook);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 25));
        
        rowNum++; // Empty row
        
        // Header rows
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);
        
        // First header row: months
        Row headerRow1 = sheet.createRow(rowNum++);
        Cell zoneHeader = headerRow1.createCell(0);
        zoneHeader.setCellValue("Zonă");
        zoneHeader.setCellStyle(headerStyle);
        
        Cell spaceHeader = headerRow1.createCell(1);
        spaceHeader.setCellValue("Spațiu");
        spaceHeader.setCellStyle(headerStyle);
        
        for (int month = 0; month < 12; month++) {
            Cell monthCell1 = headerRow1.createCell(2 + month * 2);
            monthCell1.setCellValue((month + 1) + "." + year);
            monthCell1.setCellStyle(headerStyle);
            
            Cell monthCell2 = headerRow1.createCell(2 + month * 2 + 1);
            monthCell2.setCellValue("mc");
            monthCell2.setCellStyle(headerStyle);
        }
        
        // Group counters by building (zone)
        // IndexCounter -> Location -> if Room/RentalSpace, get Building
        Map<String, List<IndexCounter>> countersByZone = counters.stream()
            .collect(Collectors.groupingBy(c -> {
                if (c.getLocation() != null) {
                    Location location = c.getLocation();
                    
                    // If location is a Building, use it directly
                    if (location instanceof Building) {
                        return location.getName();
                    }
                    
                    // If location is a Room or RentalSpace, get its parent Building
                    if (location instanceof Room) {
                        Room room = (Room) location;
                        if (room.getBuilding() != null) {
                            return room.getBuilding().getName();
                        }
                    }
                    
                    return "UNKNOWN";
                }
                return "UNKNOWN";
            }, LinkedHashMap::new, Collectors.toList()));
        
        Calendar cal = Calendar.getInstance();
        
        // For each zone, create rows
        for (Map.Entry<String, List<IndexCounter>> zoneEntry : countersByZone.entrySet()) {
            String zone = zoneEntry.getKey();
            List<IndexCounter> zoneCounters = zoneEntry.getValue();
            
            int zoneStartRow = rowNum;
            
            // Create a row for each counter in this zone
            for (IndexCounter counter : zoneCounters) {
                Row row = sheet.createRow(rowNum++);
                
                // Zone
                Cell zoneCell = row.createCell(0);
                zoneCell.setCellValue(zone);
                zoneCell.setCellStyle(dataStyle);
                
                // Space name
                Cell spaceCell = row.createCell(1);
                String spaceName = counter.getLocation() != null ? counter.getLocation().getName() : counter.getName();
                spaceCell.setCellValue(spaceName);
                spaceCell.setCellStyle(dataStyle);
                
                // For each month, find the reading
                for (int month = 0; month < 12; month++) {
                    // Get last reading of the month
                    cal.set(year, month, 1, 0, 0, 0);
                    Date monthStart = cal.getTime();
                    cal.set(year, month, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                    Date monthEnd = cal.getTime();
                    
                    IndexData lastReading = counter.getIndexData().stream()
                        .filter(r -> !r.getReadingDate().before(monthStart) && !r.getReadingDate().after(monthEnd))
                        .max(Comparator.comparing(IndexData::getReadingDate))
                        .orElse(null);
                    
                    // Index cell
                    Cell indexCell = row.createCell(2 + month * 2);
                    if (lastReading != null && lastReading.getIndex() != null) {
                        indexCell.setCellValue(lastReading.getIndex());
                    } else {
                        indexCell.setCellValue("");
                    }
                    indexCell.setCellStyle(numberStyle);
                    
                    // Consumption cell
                    Cell consumptionCell = row.createCell(2 + month * 2 + 1);
                    if (lastReading != null && lastReading.getConsumption() != null) {
                        consumptionCell.setCellValue(lastReading.getConsumption());
                    } else {
                        consumptionCell.setCellValue(0);
                    }
                    consumptionCell.setCellStyle(numberStyle);
                }
            }
            
            // Add Total row for this zone with Excel formulas
            if (zoneCounters.size() > 0) {
                Row totalRow = sheet.createRow(rowNum++);
                
                // Empty zone cell
                Cell emptyZone = totalRow.createCell(0);
                emptyZone.setCellValue("");
                emptyZone.setCellStyle(headerStyle);
                
                // Total label
                Cell totalLabel = totalRow.createCell(1);
                totalLabel.setCellValue("Total " + zone);
                totalLabel.setCellStyle(headerStyle);
                
                // For each month, add SUM formula for consumption
                for (int month = 0; month < 12; month++) {
                    // Empty index cell
                    Cell indexCell = totalRow.createCell(2 + month * 2);
                    indexCell.setCellValue("");
                    indexCell.setCellStyle(headerStyle);
                    
                    // Consumption SUM formula
                    Cell consumptionCell = totalRow.createCell(2 + month * 2 + 1);
                    String columnLetter = getColumnLetter(2 + month * 2 + 1);
                    String formula = "SUM(" + columnLetter + zoneStartRow + ":" + columnLetter + (rowNum - 1) + ")";
                    consumptionCell.setCellFormula(formula);
                    consumptionCell.setCellStyle(headerStyle);
                }
            }
        }
    }
    
    /**
     * Get display name for counter type in Romanian.
     */
    private String getCounterTypeDisplayName(CounterType type) {
        return switch (type) {
            case WATER -> "APA";
            case GAS -> "GAZ";
            case ELECTRICITY_220 -> "CURENT 220V";
            case ELECTRICITY_380 -> "CURENT 380V";
        };
    }

    /**
     * Process modified Excel file and update service values in database.
     * Compares values from Excel with calculated values and updates ServiceMonthlyValue for differences.
     * 
     * @param rentalAgreementId Rental agreement ID
     * @param year Year of the report
     * @param excelFile Modified Excel file
     * @return UpdateFromExcelResult with updated values
     */
    public com.work.total_app.models.tenant.UpdateFromExcelResult updateServiceValuesFromExcel(
            Long rentalAgreementId, int year, MultipartFile excelFile) throws IOException {
        
        TenantRentalData rentalData = rentalDataRepository.findById(rentalAgreementId)
            .orElseThrow(() -> new RuntimeException("Rental agreement not found"));
        
        // Load existing service values for comparison
        Map<String, Double> customValuesMap = new HashMap<>();
        List<com.work.total_app.models.tenant.ServiceMonthlyValue> existingCustomValues = 
            serviceMonthlyValueRepository.findByRentalDataIdAndYear(rentalData.getId(), year);
        for (com.work.total_app.models.tenant.ServiceMonthlyValue customValue : existingCustomValues) {
            String key = customValue.getServiceId() + "_" + customValue.getMonth();
            customValuesMap.put(key, customValue.getCustomValue());
        }
        
        // Get counters for calculation
        RentalSpace space = rentalData.getRentalSpace();
        List<IndexCounter> counters = space.getCounters();
        
        // Calculate expected service values (similar to createConsumptionTable)
        // We need to calculate in multiple passes to handle dependencies
        Map<Long, Map<Integer, Double>> expectedServiceValues = new HashMap<>();
        
        // Initialize all services
        for (com.work.total_app.models.tenant.ActiveService activeService : rentalData.getActiveServices()) {
            expectedServiceValues.put(activeService.getServiceId(), new HashMap<>());
        }
        
        // Multiple passes to handle dependencies (similar to createConsumptionTable)
        for (int pass = 0; pass < 10; pass++) { // Max 10 passes to avoid infinite loops
            boolean allCalculated = true;
            
            for (com.work.total_app.models.tenant.ActiveService activeService : rentalData.getActiveServices()) {
                com.work.total_app.models.service.Service service = serviceRepository.findById(activeService.getServiceId())
                    .orElse(null);
                if (service == null) continue;
                
                Map<Integer, Double> monthValues = expectedServiceValues.get(activeService.getServiceId());
                if (monthValues == null) {
                    monthValues = new HashMap<>();
                    expectedServiceValues.put(activeService.getServiceId(), monthValues);
                }
                
                boolean canCalculate = true;
                // Check if all dependencies are calculated (for formulas that use other services)
                if (service.getFormula() != null && service.getFormula().getExpression() != null) {
                    String expression = service.getFormula().getExpression();
                    // Check if expression references other services
                    for (Long otherServiceId : expectedServiceValues.keySet()) {
                        if (!otherServiceId.equals(activeService.getServiceId()) && 
                            expression.contains("service_" + otherServiceId)) {
                            Map<Integer, Double> otherValues = expectedServiceValues.get(otherServiceId);
                            if (otherValues == null || otherValues.isEmpty()) {
                                canCalculate = false;
                                break;
                            }
                        }
                    }
                }
                
                if (canCalculate || pass > 0) {
                    // Calculate values for all months
                    for (int month = 0; month < 12; month++) {
                        if (!monthValues.containsKey(month)) {
                            Calendar cal = Calendar.getInstance();
                            cal.set(year, month, 1, 0, 0, 0);
                            cal.set(Calendar.MILLISECOND, 0);
                            Date monthStart = cal.getTime();
                            
                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                            cal.set(Calendar.HOUR_OF_DAY, 23);
                            cal.set(Calendar.MINUTE, 59);
                            cal.set(Calendar.SECOND, 59);
                            Date monthEnd = cal.getTime();
                            
                            boolean isActive = isServiceActiveForMonth(activeService, monthStart, monthEnd);
                            if (isActive) {
                                // Check for existing custom value first
                                String customKey = activeService.getServiceId() + "_" + month;
                                if (customValuesMap.containsKey(customKey)) {
                                    monthValues.put(month, customValuesMap.get(customKey));
                                } else {
                                    // Calculate expected value
                                    Double value = calculateServiceValue(activeService, service, rentalData, counters, year, month, expectedServiceValues);
                                    monthValues.put(month, value != null ? value : 0.0);
                                }
                            } else {
                                monthValues.put(month, 0.0);
                            }
                        }
                    }
                } else {
                    allCalculated = false;
                }
            }
            
            if (allCalculated) break;
        }
        
        // Parse Excel file
        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelFile.getBytes()));
        Sheet sheet = workbook.getSheetAt(0); // First sheet
        
        // Find header row (row with months)
        int headerRowIndex = -1;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            Cell cell = row.getCell(2); // First month column
            if (cell != null) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && cellValue.contains("IANUARIE")) {
                    headerRowIndex = i;
                    break;
                }
            }
        }
        
        if (headerRowIndex == -1) {
            workbook.close();
            throw new RuntimeException("Could not find header row in Excel file");
        }
        
        // Build service name to ID mapping
        Map<String, Long> serviceNameToId = new HashMap<>();
        for (com.work.total_app.models.tenant.ActiveService activeService : rentalData.getActiveServices()) {
            com.work.total_app.models.service.Service service = serviceRepository.findById(activeService.getServiceId())
                .orElse(null);
            if (service != null) {
                serviceNameToId.put(service.getName().trim(), activeService.getServiceId());
            }
        }
        
        // Known counter names (to skip)
        Set<String> counterNames = Set.of("E-ON", "APA", "GAZ");
        
        // Process rows after header
        com.work.total_app.models.tenant.UpdateFromExcelResult result = 
            new com.work.total_app.models.tenant.UpdateFromExcelResult();
        
        int dataStartRow = headerRowIndex + 1;
        for (int rowIndex = dataStartRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            
            // Check if this is an empty row
            Cell firstCell = row.getCell(0);
            if (firstCell == null) continue;
            
            String serviceName = getCellValueAsString(firstCell);
            if (serviceName == null || serviceName.trim().isEmpty()) continue;
            
            // Skip counter rows (they have specific names)
            if (counterNames.contains(serviceName.trim().toUpperCase())) {
                // Skip consumption row (next row might be cost row)
                continue;
            }
            
            // Check if this is a service row
            Long serviceId = serviceNameToId.get(serviceName.trim());
            if (serviceId == null) {
                // Not a known service, skip
                continue;
            }
            
            // Read values for each month (columns 2-13)
            Map<Integer, Double> expectedValues = expectedServiceValues.get(serviceId);
            if (expectedValues == null) continue;
            
            com.work.total_app.models.service.Service service = serviceRepository.findById(serviceId).orElse(null);
            if (service == null) continue;
            
            for (int month = 0; month < 12; month++) {
                int columnIndex = month + 2; // Columns 2-13
                Cell cell = row.getCell(columnIndex);
                if (cell == null) continue;
                
                Double excelValue = getCellNumericValue(cell);
                if (excelValue == null) continue;
                
                Double expectedValue = expectedValues.get(month);
                if (expectedValue == null) expectedValue = 0.0;
                
                // Check if value differs from expected (with small tolerance for floating point)
                double tolerance = 0.01;
                if (Math.abs(excelValue - expectedValue) > tolerance) {
                    // Value was modified, update ServiceMonthlyValue
                    com.work.total_app.models.tenant.ServiceMonthlyValue monthlyValue = 
                        serviceMonthlyValueRepository.findByRentalDataIdAndServiceIdAndYearAndMonth(
                            rentalData.getId(), serviceId, year, month)
                        .orElse(null);
                    
                    if (monthlyValue == null) {
                        monthlyValue = new com.work.total_app.models.tenant.ServiceMonthlyValue();
                        monthlyValue.setRentalData(rentalData);
                        monthlyValue.setServiceId(serviceId);
                        monthlyValue.setYear(year);
                        monthlyValue.setMonth(month);
                    }
                    
                    Double oldValue = monthlyValue.getCustomValue();
                    monthlyValue.setCustomValue(excelValue);
                    
                    serviceMonthlyValueRepository.save(monthlyValue);
                    
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                    com.work.total_app.models.tenant.UpdateFromExcelResult.ServiceMonthlyValueInfo info = 
                        new com.work.total_app.models.tenant.UpdateFromExcelResult.ServiceMonthlyValueInfo();
                    info.setServiceId(serviceId);
                    info.setServiceName(service.getName());
                    info.setMonth(month);
                    info.setOldValue(oldValue);
                    info.setNewValue(excelValue);
                    result.getUpdatedValues().add(info);
                    
                    log.info("Updated service {} (ID: {}) month {} from {} to {}", 
                        service.getName(), serviceId, month, oldValue, excelValue);
                }
            }
        }
        
        workbook.close();
        return result;
    }
    
    /**
     * Get cell value as string.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Check if cell is date formatted
                try {
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    }
                } catch (Exception e) {
                    // Not a date, continue
                }
                // Convert to string without decimals if it's a whole number
                double numValue = cell.getNumericCellValue();
                if (numValue == (long) numValue) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // For formulas, get the calculated value
                return getCellNumericValue(cell) != null ? String.valueOf(getCellNumericValue(cell)) : null;
            case BLANK:
                return null;
            default:
                return null;
        }
    }
    
    /**
     * Get cell value as numeric (Double).
     */
    private Double getCellNumericValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case FORMULA:
                // For formulas, evaluate and get numeric value
                try {
                    if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                        return cell.getNumericCellValue();
                    } else if (cell.getCachedFormulaResultType() == CellType.STRING) {
                        try {
                            return Double.parseDouble(cell.getStringCellValue());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error reading formula value: {}", e.getMessage());
                }
                return null;
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }
}

