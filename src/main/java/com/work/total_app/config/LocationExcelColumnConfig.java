package com.work.total_app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.excel.locations.columns")
@Data
public class LocationExcelColumnConfig {
    private String locationType;
    private String name;
    private String officialName;
    private String buildingLocation;
    private String mp;
    private String groundLevel;
    private String buildingName;
}

