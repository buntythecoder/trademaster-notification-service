package com.trademaster.notification.security;

/**
 * Security Error Types for Zero Trust Architecture
 * 
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Sealed Classes - Rule #9
 * MANDATORY: Pattern Matching - Rule #14
 */
public sealed interface SecurityError permits 
    SecurityError.AuthenticationError,
    SecurityError.AuthorizationError,
    SecurityError.ValidationError,
    SecurityError.AuditError,
    SecurityError.RiskError {
    
    String getMessage();
    String getCode();
    SecurityLevel getSeverity();
    
    enum SecurityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Authentication failures
     */
    record AuthenticationError(
        String message,
        String code,
        SecurityLevel severity
    ) implements SecurityError {
        
        public static AuthenticationError invalidToken() {
            return new AuthenticationError(
                "Invalid or expired authentication token",
                "AUTH_001",
                SecurityLevel.HIGH
            );
        }
        
        public static AuthenticationError missingToken() {
            return new AuthenticationError(
                "Authentication token required",
                "AUTH_002",
                SecurityLevel.HIGH
            );
        }
        
        public static AuthenticationError invalidCredentials() {
            return new AuthenticationError(
                "Invalid credentials provided",
                "AUTH_003",
                SecurityLevel.HIGH
            );
        }
        
        @Override
        public String getMessage() {
            return message;
        }
        
        @Override
        public String getCode() {
            return code;
        }
        
        @Override
        public SecurityLevel getSeverity() {
            return severity;
        }
    }
    
    /**
     * Authorization failures
     */
    record AuthorizationError(
        String message,
        String code,
        SecurityLevel severity
    ) implements SecurityError {
        
        public static AuthorizationError insufficientPermissions() {
            return new AuthorizationError(
                "Insufficient permissions for this operation",
                "AUTHZ_001",
                SecurityLevel.HIGH
            );
        }
        
        public static AuthorizationError resourceAccessDenied() {
            return new AuthorizationError(
                "Access denied to requested resource",
                "AUTHZ_002",
                SecurityLevel.HIGH
            );
        }
        
        public static AuthorizationError roleRequired(String role) {
            return new AuthorizationError(
                String.format("Role '%s' required for this operation", role),
                "AUTHZ_003",
                SecurityLevel.HIGH
            );
        }
        
        @Override
        public String getMessage() {
            return message;
        }
        
        @Override
        public String getCode() {
            return code;
        }
        
        @Override
        public SecurityLevel getSeverity() {
            return severity;
        }
    }
    
    /**
     * Input validation failures
     */
    record ValidationError(
        String message,
        String code,
        SecurityLevel severity
    ) implements SecurityError {
        
        public static ValidationError invalidInput(String field) {
            return new ValidationError(
                String.format("Invalid input for field: %s", field),
                "VAL_001",
                SecurityLevel.MEDIUM
            );
        }
        
        public static ValidationError suspiciousInput() {
            return new ValidationError(
                "Input contains suspicious patterns",
                "VAL_002",
                SecurityLevel.HIGH
            );
        }
        
        public static ValidationError rateLimitExceeded() {
            return new ValidationError(
                "Rate limit exceeded for this operation",
                "VAL_003",
                SecurityLevel.MEDIUM
            );
        }
        
        @Override
        public String getMessage() {
            return message;
        }
        
        @Override
        public String getCode() {
            return code;
        }
        
        @Override
        public SecurityLevel getSeverity() {
            return severity;
        }
    }
    
    /**
     * Audit logging failures
     */
    record AuditError(
        String message,
        String code,
        SecurityLevel severity
    ) implements SecurityError {
        
        public static AuditError auditLogFailed() {
            return new AuditError(
                "Failed to write security audit log",
                "AUDIT_001",
                SecurityLevel.CRITICAL
            );
        }
        
        @Override
        public String getMessage() {
            return message;
        }
        
        @Override
        public String getCode() {
            return code;
        }
        
        @Override
        public SecurityLevel getSeverity() {
            return severity;
        }
    }
    
    /**
     * Risk assessment failures
     */
    record RiskError(
        String message,
        String code,
        SecurityLevel severity
    ) implements SecurityError {
        
        public static RiskError highRiskOperation() {
            return new RiskError(
                "Operation blocked due to high risk score",
                "RISK_001",
                SecurityLevel.HIGH
            );
        }
        
        public static RiskError suspiciousActivity() {
            return new RiskError(
                "Suspicious activity detected",
                "RISK_002",
                SecurityLevel.CRITICAL
            );
        }
        
        @Override
        public String getMessage() {
            return message;
        }
        
        @Override
        public String getCode() {
            return code;
        }
        
        @Override
        public SecurityLevel getSeverity() {
            return severity;
        }
    }
}