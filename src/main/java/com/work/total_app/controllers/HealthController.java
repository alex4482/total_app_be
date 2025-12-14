package com.work.total_app.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint pentru monitoring
 */
@RestController
@RequestMapping("/health")
@Log4j2
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    /**
     * Basic health check
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("application", "total_app");
        
        // Version info (if available)
        if (buildProperties != null) {
            health.put("version", buildProperties.getVersion());
            health.put("build_time", buildProperties.getTime().toString());
        }
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with database connectivity
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> checks = new HashMap<>();
        
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("application", "total_app");
        
        // Version info
        if (buildProperties != null) {
            health.put("version", buildProperties.getVersion());
        }
        
        // Database check
        Map<String, String> dbCheck = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(2); // 2 seconds timeout
            dbCheck.put("status", isValid ? "UP" : "DOWN");
            dbCheck.put("database", conn.getMetaData().getDatabaseProductName());
            dbCheck.put("version", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbCheck.put("status", "DOWN");
            dbCheck.put("error", e.getMessage());
            health.put("status", "DEGRADED");
        }
        checks.put("database", dbCheck);
        
        // Memory check
        Map<String, Object> memoryCheck = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memoryCheck.put("status", "UP");
        memoryCheck.put("used_mb", usedMemory / (1024 * 1024));
        memoryCheck.put("free_mb", freeMemory / (1024 * 1024));
        memoryCheck.put("total_mb", totalMemory / (1024 * 1024));
        memoryCheck.put("max_mb", maxMemory / (1024 * 1024));
        memoryCheck.put("usage_percent", (usedMemory * 100) / totalMemory);
        
        checks.put("memory", memoryCheck);
        
        health.put("checks", checks);
        
        return ResponseEntity.ok(health);
    }
}

