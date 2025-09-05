package com.trademaster.notification.security;

import com.trademaster.notification.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Security Audit Service - Audit logging for security events
 * 
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Structured Logging - Rule #15
 * MANDATORY: Virtual Threads - Rule #12
 */
@Service
@Slf4j
public class SecurityAuditService {
    
    /**
     * Log security operation with structured logging
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Result Types - Rule #11
     */
    public CompletableFuture<Result<String, SecurityError>> logOperation(
            SecurityContext context,
            String operation,
            String phase) {
        
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    String auditEntry = createAuditEntry(context, operation, phase);
                    
                    log.info("SECURITY_AUDIT | correlationId={} | userId={} | operation={} | phase={} | " +
                            "timestamp={} | ipAddress={} | riskLevel={} | sessionId={}",
                        context.correlationId(),
                        context.userId(),
                        operation,
                        phase,
                        context.timestamp(),
                        context.ipAddress(),
                        context.riskLevel(),
                        context.sessionId()
                    );
                    
                    return Result.<String, SecurityError>success(auditEntry);
                    
                } catch (Exception e) {
                    log.error("Failed to write security audit log: {}", e.getMessage());
                    return Result.<String, SecurityError>failure(SecurityError.AuditError.auditLogFailed());
                }
            }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Log operation result
     */
    public <T> CompletableFuture<Result<String, SecurityError>> logOperationResult(
            SecurityContext context,
            String operation,
            Result<T, SecurityError> operationResult) {
        
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    String status = operationResult.isSuccess() ? "SUCCESS" : "FAILURE";
                    String errorCode = operationResult.isFailure() ? 
                        operationResult.getError().getCode() : "NONE";
                    
                    log.info("SECURITY_AUDIT_RESULT | correlationId={} | userId={} | operation={} | " +
                            "status={} | errorCode={} | timestamp={} | duration={}ms",
                        context.correlationId(),
                        context.userId(),
                        operation,
                        status,
                        errorCode,
                        LocalDateTime.now(),
                        calculateDuration(context.timestamp())
                    );
                    
                    return Result.<String, SecurityError>success("AUDIT_LOGGED");
                    
                } catch (Exception e) {
                    log.error("Failed to write security audit result: {}", e.getMessage());
                    return Result.<String, SecurityError>failure(SecurityError.AuditError.auditLogFailed());
                }
            }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Log security violation
     */
    public CompletableFuture<Result<String, SecurityError>> logSecurityViolation(
            SecurityContext context,
            SecurityError securityError,
            String details) {
        
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    log.warn("SECURITY_VIOLATION | correlationId={} | userId={} | errorCode={} | " +
                            "severity={} | message={} | details={} | timestamp={} | ipAddress={}",
                        context.correlationId(),
                        context.userId(),
                        securityError.getCode(),
                        securityError.getSeverity(),
                        securityError.getMessage(),
                        details,
                        LocalDateTime.now(),
                        context.ipAddress()
                    );
                    
                    return Result.<String, SecurityError>success("VIOLATION_LOGGED");
                    
                } catch (Exception e) {
                    log.error("Failed to log security violation: {}", e.getMessage());
                    return Result.<String, SecurityError>failure(SecurityError.AuditError.auditLogFailed());
                }
            }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Create structured audit entry
     */
    private String createAuditEntry(SecurityContext context, String operation, String phase) {
        return String.format(
            "AuditEntry{correlationId='%s', userId='%s', operation='%s', phase='%s', timestamp='%s'}",
            context.correlationId(),
            context.userId(),
            operation,
            phase,
            context.timestamp()
        );
    }
    
    /**
     * Calculate operation duration
     */
    private long calculateDuration(LocalDateTime startTime) {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }
}