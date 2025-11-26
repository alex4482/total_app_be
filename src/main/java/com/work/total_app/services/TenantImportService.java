package com.work.total_app.services;

import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.models.tenant.TenantImportResultDto;
import com.work.total_app.repositories.TenantRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Log4j2
public class TenantImportService {

    @Autowired
    private TenantRepository tenantRepository;

    public TenantImportResultDto importTenantsFromExcel(MultipartFile file) throws IOException {
        List<String> skippedNames = new ArrayList<>();
        int savedCount = 0;
        int skippedCount = 0;
        int totalRows = 0;
        int updatedCount = 0;
        int createdCount = 0;

        // Save to temporary file to avoid stream issues
        java.io.File tempFile = java.io.File.createTempFile("tenant-import-", ".xlsx");
        try {
            file.transferTo(tempFile);

            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);

                // Read header row to detect available columns
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    throw new IllegalArgumentException("Excel file must have a header row");
                }

                Map<String, Integer> columnMap = buildColumnMap(headerRow);

                // Start from row 1 (skip header row 0)
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    // First column is always the tenant name
                    String name = getCellValueAsString(row.getCell(0));

                    // Skip empty rows
                    if (name == null || name.trim().isEmpty()) continue;

                    totalRows++;

                    try {
                        // Check if tenant exists by name
                        Optional<Tenant> existingTenantOpt = tenantRepository.findByName(name.trim());

                        Tenant tenant;
                        boolean isUpdate = false;

                        if (existingTenantOpt.isPresent()) {
                            // UPDATE existing tenant
                            tenant = existingTenantOpt.get();
                            isUpdate = true;
                            log.info("Updating existing tenant: {}", name.trim());
                        } else {
                            // CREATE new tenant
                            tenant = new Tenant();
                            tenant.setName(name.trim());
                            tenant.setActive(false); // Default to inactive
                            tenant.setPf(false); // Default to legal entity
                            log.info("Creating new tenant: {}", name.trim());
                        }

                        // Update fields based on available columns
                        updateTenantFromRow(tenant, row, columnMap, isUpdate);

                        tenantRepository.save(tenant);
                        savedCount++;

                        if (isUpdate) {
                            updatedCount++;
                        } else {
                            createdCount++;
                        }

                    } catch (Exception e) {
                        log.error("Error processing tenant: {}", name.trim(), e);
                        skippedNames.add(name.trim() + " (Error: " + e.getMessage() + ")");
                        skippedCount++;
                    }
                }

                TenantImportResultDto result = new TenantImportResultDto();
                result.setTotalRows(totalRows);
                result.setSavedCount(savedCount);
                result.setSkippedCount(skippedCount);
                result.setSkippedNames(skippedNames);

                log.info("Import completed: {} created, {} updated, {} skipped",
                    createdCount, updatedCount, skippedCount);

                return result;
            }
        } finally {
            // Clean up temporary file
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Build a map of column names to their indices.
     * Column names are normalized (lowercase, trimmed).
     */
    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String header = getCellValueAsString(cell);
                if (header != null && !header.trim().isEmpty()) {
                    String normalized = normalizeColumnName(header);
                    map.put(normalized, i);
                    log.debug("Found column: {} -> {}", header, normalized);
                }
            }
        }
        return map;
    }

    /**
     * Normalize column name for matching (lowercase, trim, remove special chars).
     */
    private String normalizeColumnName(String name) {
        return name.trim().toLowerCase()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    /**
     * Update tenant fields based on available columns in Excel.
     * Only updates fields if the corresponding column exists.
     */
    private void updateTenantFromRow(Tenant tenant, Row row, Map<String, Integer> columnMap, boolean isUpdate) {
        // CUI field (column: "CodFiscal" or "CUI")
        Integer cuiCol = columnMap.get("codfiscal");
        if (cuiCol == null) cuiCol = columnMap.get("cui");
        if (cuiCol != null) {
            String cui = getCellValueAsString(row.getCell(cuiCol));
            if (cui != null && !cui.trim().isEmpty()) {
                tenant.setCui(cui.trim());
            }
        }

        // RegNumber field (column: "Nr. Reg" or "NrReg" or "RegNumber")
        Integer regCol = columnMap.get("nrreg");
        if (regCol == null) regCol = columnMap.get("nrreg");
        if (regCol == null) regCol = columnMap.get("regnumber");
        if (regCol != null) {
            String regNumber = getCellValueAsString(row.getCell(regCol));
            if (regNumber != null && !regNumber.trim().isEmpty()) {
                tenant.setRegNumber(regNumber.trim());
            }
        }

        // Email field (column: "Email", "Emails", "Email-uri", "Emailuri", etc.)
        Integer emailCol = columnMap.get("email");
        if (emailCol == null) emailCol = columnMap.get("emails");
        if (emailCol == null) emailCol = columnMap.get("emailuri");
        if (emailCol == null) emailCol = columnMap.get("email-uri");
        if (emailCol != null) {
            String emailStr = getCellValueAsString(row.getCell(emailCol));
            if (emailStr != null && !emailStr.trim().isEmpty()) {
                List<String> emails = parseDelimitedList(emailStr);
                tenant.setEmails(emails);
            } else if (!isUpdate) {
                // For new tenants, initialize empty list
                tenant.setEmails(new ArrayList<>());
            }
        }

        // Phone Numbers field (column: "Telefoane" or "Telefon" or "PhoneNumbers")
        Integer phoneCol = columnMap.get("telefoane");
        if (phoneCol == null) phoneCol = columnMap.get("telefon");
        if (phoneCol == null) phoneCol = columnMap.get("phonenumbers");
        if (phoneCol != null) {
            String phoneStr = getCellValueAsString(row.getCell(phoneCol));
            if (phoneStr != null && !phoneStr.trim().isEmpty()) {
                List<String> phones = parseDelimitedList(phoneStr);
                tenant.setPhoneNumbers(phones);
            } else if (!isUpdate) {
                // For new tenants, initialize empty list
                tenant.setPhoneNumbers(new ArrayList<>());
            }
        }
    }

    /**
     * Parse a delimited string into a list (supports comma, semicolon, pipe).
     */
    private List<String> parseDelimitedList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] parts = value.split("[,;|]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Handle numbers as strings (for codes like CUI, reg numbers)
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Convert to long to avoid decimal points for whole numbers
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
}
