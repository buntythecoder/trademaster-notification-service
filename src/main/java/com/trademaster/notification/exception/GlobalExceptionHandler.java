package com.trademaster.notification.exception;

import com.trademaster.notification.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for Notification Service
 * 
 * MANDATORY: Error Handling - Rule #11
 * MANDATORY: Security First - Rule #6
 * MANDATORY: Functional Programming - Rule #3
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handle validation errors
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, 
            WebRequest request) {
        
        log.warn("Validation error on request: {}", request.getDescription(false));
        
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> Optional.ofNullable(error.getDefaultMessage()).orElse("Invalid value"),
                (existing, replacement) -> existing
            ));
        
        return ResponseEntity
            .badRequest()
            .body(createErrorResponse(
                "VALIDATION_ERROR",
                "Input validation failed",
                errors,
                request
            ));
    }
    
    /**
     * Handle constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {
        
        log.warn("Constraint violation on request: {}", request.getDescription(false));
        
        Map<String, String> errors = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                violation -> violation.getMessage()
            ));
        
        return ResponseEntity
            .badRequest()
            .body(createErrorResponse(
                "CONSTRAINT_VIOLATION",
                "Data constraint violation",
                errors,
                request
            ));
    }
    
    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request) {
        
        log.warn("Access denied for request: {}", request.getDescription(false));
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(createErrorResponse(
                "ACCESS_DENIED",
                "Access denied - insufficient permissions",
                null,
                request
            ));
    }
    
    /**
     * Handle notification-specific exceptions
     */
    @ExceptionHandler(NotificationServiceException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationServiceException(
            NotificationServiceException ex,
            WebRequest request) {
        
        log.error("Notification service error on request: {}", request.getDescription(false), ex);
        
        HttpStatus status = switch (ex.getErrorCode()) {
            case "TEMPLATE_NOT_FOUND", "PREFERENCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "TEMPLATE_ALREADY_EXISTS", "INVALID_OPERATION" -> HttpStatus.BAD_REQUEST;
            case "EXTERNAL_SERVICE_ERROR" -> HttpStatus.BAD_GATEWAY;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        return ResponseEntity
            .status(status)
            .body(createErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                null,
                request
            ));
    }
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {
        
        log.warn("Illegal argument on request: {}", request.getDescription(false));
        
        return ResponseEntity
            .badRequest()
            .body(createErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                null,
                request
            ));
    }
    
    /**
     * Handle database-related exceptions
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            org.springframework.dao.DataAccessException ex,
            WebRequest request) {
        
        log.error("Database error on request: {}", request.getDescription(false), ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(
                "DATABASE_ERROR",
                "Database operation failed",
                null,
                request
            ));
    }
    
    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {
        
        log.error("Runtime error on request: {}", request.getDescription(false), ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                null,
                request
            ));
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error on request: {}", request.getDescription(false), ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(
                "UNKNOWN_ERROR",
                "An unknown error occurred",
                null,
                request
            ));
    }
    
    /**
     * Create standardized error response
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    private Map<String, Object> createErrorResponse(
            String errorCode,
            String message,
            Map<String, String> details,
            WebRequest request) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));

        // Rule #3: NO if-else, use Optional for conditional map entry
        java.util.Optional.ofNullable(details)
            .filter(d -> !d.isEmpty())
            .ifPresent(d -> errorResponse.put("details", d));

        return errorResponse;
    }
}