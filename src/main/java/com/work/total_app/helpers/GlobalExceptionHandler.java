package com.work.total_app.helpers;

import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler pentru răspunsuri uniforme de eroare
 */
@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    /**
     * DTO pentru răspunsuri de eroare uniforme
     */
    public record ErrorResponse(
        String error,
        String message,
        int status,
        String timestamp,
        Map<String, String> details
    ) {
        public ErrorResponse(String error, String message, int status) {
            this(error, message, status, Instant.now().toString(), null);
        }
        
        public ErrorResponse(String error, String message, int status, Map<String, String> details) {
            this(error, message, status, Instant.now().toString(), details);
        }
    }

    /**
     * Handler pentru ResponseStatusException (cel mai folosit în aplicație)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: {} - {}", ex.getStatusCode(), ex.getReason());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getStatusCode().toString(),
            ex.getReason() != null ? ex.getReason() : "An error occurred",
            ex.getStatusCode().value()
        );
        
        return ResponseEntity
            .status(ex.getStatusCode())
            .body(errorResponse);
    }

    /**
     * Handler pentru validări @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error: {}", validationErrors);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            HttpStatus.BAD_REQUEST.value(),
            validationErrors
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handler pentru ValidationException custom
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.warn("ValidationException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handler pentru NotFoundException custom
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        log.warn("NotFoundException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value()
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }

    /**
     * Handler pentru excepții generice (fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please contact support if the problem persists.",
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    /**
     * Handler pentru IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "BAD_REQUEST",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handler pentru IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "Application is in an invalid state: " + ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }
}

