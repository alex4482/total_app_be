package com.work.total_app.models.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response wrapper for all API endpoints.
 * Frontend should check 'success' field and display 'message' to users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    /**
     * Whether the operation was successful
     */
    private boolean success;
    
    /**
     * Message for the user (e.g., "Preset updated successfully" or "Failed to send email")
     */
    private String message;
    
    /**
     * Data payload (can be null on errors)
     */
    private T data;

    // Static factory methods for convenience
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation completed successfully", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}

