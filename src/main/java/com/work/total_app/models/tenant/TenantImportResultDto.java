package com.work.total_app.models.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TenantImportResultDto {
    private int totalRows;
    private int savedCount;
    private int skippedCount;
    private List<String> skippedNames = new ArrayList<>();
}

