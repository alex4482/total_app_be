package com.work.total_app.config;

import com.work.total_app.models.file.OwnerType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Data
public class StorageProperties {
    private String baseDir;
    private Map<OwnerType, String> templates = new EnumMap<>(OwnerType.class);
}
