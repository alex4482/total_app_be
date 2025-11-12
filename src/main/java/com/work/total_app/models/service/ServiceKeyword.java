package com.work.total_app.models.service;

/**
 * DTO for service keyword (used in formulas).
 */
public record ServiceKeyword(
    Long serviceId,
    String serviceName,
    String keyword  // Lowercase, spaces replaced with underscore, diacritics removed
) {}

