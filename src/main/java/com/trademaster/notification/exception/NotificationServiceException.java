package com.trademaster.notification.exception;

import lombok.Getter;

/**
 * Custom Exception for Notification Service Operations
 * 
 * MANDATORY: Error Handling - Rule #11
 * MANDATORY: Functional Programming - Rule #3
 */
@Getter
public class NotificationServiceException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] parameters;
    
    public NotificationServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = new Object[0];
    }
    
    public NotificationServiceException(String errorCode, String message, Object... parameters) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = parameters != null ? parameters : new Object[0];
    }
    
    public NotificationServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.parameters = new Object[0];
    }
    
    public NotificationServiceException(String errorCode, String message, Throwable cause, Object... parameters) {
        super(message, cause);
        this.errorCode = errorCode;
        this.parameters = parameters != null ? parameters : new Object[0];
    }
    
    // Common error codes as constants
    public static final class ErrorCodes {
        public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
        public static final String TEMPLATE_ALREADY_EXISTS = "TEMPLATE_ALREADY_EXISTS";
        public static final String TEMPLATE_VALIDATION_FAILED = "TEMPLATE_VALIDATION_FAILED";
        public static final String PREFERENCE_NOT_FOUND = "PREFERENCE_NOT_FOUND";
        public static final String INVALID_NOTIFICATION_TYPE = "INVALID_NOTIFICATION_TYPE";
        public static final String INVALID_TEMPLATE_CATEGORY = "INVALID_TEMPLATE_CATEGORY";
        public static final String EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR";
        public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
        public static final String INVALID_OPERATION = "INVALID_OPERATION";
        public static final String DATABASE_ERROR = "DATABASE_ERROR";
        
        private ErrorCodes() {}
    }
    
    // Factory methods for common exceptions
    public static NotificationServiceException templateNotFound(String templateName) {
        return new NotificationServiceException(
            ErrorCodes.TEMPLATE_NOT_FOUND,
            "Template not found: " + templateName,
            templateName
        );
    }
    
    public static NotificationServiceException templateAlreadyExists(String templateName) {
        return new NotificationServiceException(
            ErrorCodes.TEMPLATE_ALREADY_EXISTS,
            "Template already exists: " + templateName,
            templateName
        );
    }
    
    public static NotificationServiceException preferenceNotFound(String userId) {
        return new NotificationServiceException(
            ErrorCodes.PREFERENCE_NOT_FOUND,
            "User preferences not found: " + userId,
            userId
        );
    }
    
    public static NotificationServiceException rateLimitExceeded(String operation, int limit) {
        return new NotificationServiceException(
            ErrorCodes.RATE_LIMIT_EXCEEDED,
            String.format("Rate limit exceeded for operation '%s': %d requests", operation, limit),
            operation, limit
        );
    }
    
    public static NotificationServiceException externalServiceError(String service, String error) {
        return new NotificationServiceException(
            ErrorCodes.EXTERNAL_SERVICE_ERROR,
            String.format("External service '%s' error: %s", service, error),
            service, error
        );
    }
    
    public static NotificationServiceException invalidOperation(String operation, String reason) {
        return new NotificationServiceException(
            ErrorCodes.INVALID_OPERATION,
            String.format("Invalid operation '%s': %s", operation, reason),
            operation, reason
        );
    }
}