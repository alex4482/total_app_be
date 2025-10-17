package com.work.total_app.models.file;

/**
 * Enumerates domain owner types that can own files.
 * Used both for persistence (as string) and for API queries.
 * Note: When parsed from request parameters, valueOf is case-sensitive.
 */
public enum OwnerType {
    TENANT,
    BUILDING,
    ROOM,
    RENTAL_SPACE,
    EMAIL_DATA,
    BUILDING_LOCATION,
    FIRM,
    CAR,
    OTHER
}
