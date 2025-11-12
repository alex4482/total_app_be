package com.work.total_app.models.service;

import java.util.List;

/**
 * Response for service keywords endpoint.
 */
public record ServiceKeywordsResponse(
    List<ServiceKeyword> keywords
) {}

