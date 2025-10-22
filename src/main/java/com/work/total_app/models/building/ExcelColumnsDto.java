package com.work.total_app.models.building;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExcelColumnsDto {
    private List<String> columns;
}

