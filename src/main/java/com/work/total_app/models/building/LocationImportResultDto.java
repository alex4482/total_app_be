package com.work.total_app.models.building;

import lombok.Data;

import java.util.List;

@Data
public class LocationImportResultDto {
    private int totalRows;
    private int created;
    private int updated;
    private int skipped;
    private List<String> errors;
}

