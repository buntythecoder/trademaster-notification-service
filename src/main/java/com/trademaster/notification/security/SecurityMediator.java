package com.trademaster.notification.security;

import com.trademaster.notification.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Security Mediator - Coordinates all security operations
 * 
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Mediator Pattern - Rule #4
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityMediator {
    
    private final SecurityAuditService auditService;
    private final RiskAssessmentService riskService;
    
    /**
     * Mediate secure access with full security pipeline
     * 
     * MANDATORY: Railway Programming - Rule #11
     * MANDATORY: Pattern Matching - Rule #14
     */
    public <T> CompletableFuture<Result<T, SecurityError>> mediateSecureAccess(
            SecurityContext context,
            String operation,
            Supplier<CompletableFuture<Result<T, Exception>>> businessOperation) {
        
        return CompletableFuture
            .supplyAsync(() -> {
                log.debug("Starting security mediation for operation: {} by user: {}", 
                         operation, context.userId());
                
                return authenticateContext(context)
                    .flatMap(ctx -> authorizeOperation(ctx, operation))
                    .flatMap(ctx -> assessRisk(ctx, operation))
                    .flatMap(ctx -> validateInput(ctx, operation));
                    
            }, Executors.newVirtualThreadPerTaskExecutor())
            .thenCompose(securityResult -> 
                securityResult.match(
                    validContext -> executeSecureOperation(validContext, operation, businessOperation),
                    securityError -> CompletableFuture.completedFuture(Result.failure(securityError))
                )
            );
    }
    
    /**
     * Simple internal service access (within security boundary)
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    public <T> CompletableFuture<Result<T, SecurityError>> mediateInternalAccess(
            String correlationId,
            String operation,
            Supplier<CompletableFuture<Result<T, Exception>>> businessOperation) {
        
        SecurityContext systemContext = SecurityContext.system(correlationId);
        
        return auditService.logOperation(systemContext, operation, "INTERNAL_ACCESS")
            .thenCompose(auditResult -> 
                auditResult.match(
                    success -> businessOperation.get()
                        .thenApply(result -> result.mapError(this::convertToSecurityError)),
                    auditError -> CompletableFuture.completedFuture(Result.failure(auditError))
                )
            );
    }
    
    /**
     * Authentication step
     */
    private Result<SecurityContext, SecurityError> authenticateContext(SecurityContext context) {
        return switch (validateAuthenticationToken(context)) {
            case VALID -> {
                log.debug("Authentication successful for user: {}", context.userId());
                yield Result.success(context);
            }
            case INVALID_TOKEN -> {
                log.warn("Invalid authentication token for user: {}", context.userId());
                yield Result.failure(SecurityError.AuthenticationError.invalidToken());
            }
            case MISSING_TOKEN -> {
                log.warn("Missing authentication token");
                yield Result.failure(SecurityError.AuthenticationError.missingToken());
            }
            case EXPIRED -> {
                log.warn("Expired authentication token for user: {}", context.userId());
                yield Result.failure(SecurityError.AuthenticationError.invalidToken());
            }
        };
    }
    
    /**
     * Authorization step
     */
    private Result<SecurityContext, SecurityError> authorizeOperation(
            SecurityContext context, String operation) {
        
        return switch (checkOperationPermission(context, operation)) {
            case ALLOWED -> {
                log.debug("Authorization successful for operation: {} by user: {}", 
                         operation, context.userId());
                yield Result.success(context);
            }
            case INSUFFICIENT_PERMISSIONS -> {
                log.warn("Insufficient permissions for operation: {} by user: {}", 
                        operation, context.userId());
                yield Result.failure(SecurityError.AuthorizationError.insufficientPermissions());
            }
            case RESOURCE_DENIED -> {
                log.warn("Resource access denied for operation: {} by user: {}", 
                        operation, context.userId());
                yield Result.failure(SecurityError.AuthorizationError.resourceAccessDenied());
            }
        };
    }
    
    /**
     * Risk assessment step
     */
    private Result<SecurityContext, SecurityError> assessRisk(
            SecurityContext context, String operation) {
        
        double riskScore = riskService.calculateRiskScore(context, operation);
        SecurityContext.SecurityRisk riskLevel = SecurityContext.SecurityRisk.fromScore(riskScore);
        
        SecurityContext updatedContext = SecurityContext.builder()
            .correlationId(context.correlationId())
            .userId(context.userId())
            .sessionId(context.sessionId())
            .roles(context.roles())
            .attributes(context.attributes())
            .ipAddress(context.ipAddress())
            .userAgent(context.userAgent())
            .timestamp(context.timestamp())
            .riskLevel(riskLevel)
            .build();
        
        return switch (riskLevel) {
            case LOW, MEDIUM -> {
                log.debug("Risk assessment passed for operation: {} with level: {}", 
                         operation, riskLevel);
                yield Result.success(updatedContext);
            }
            case HIGH -> {
                log.warn("High risk detected for operation: {} by user: {}", 
                        operation, context.userId());
                yield Result.failure(SecurityError.RiskError.highRiskOperation());
            }
            case CRITICAL -> {
                log.error("Critical risk detected for operation: {} by user: {}", 
                         operation, context.userId());
                yield Result.failure(SecurityError.RiskError.suspiciousActivity());
            }
        };
    }
    
    /**
     * Input validation step
     */
    private Result<SecurityContext, SecurityError> validateInput(
            SecurityContext context, String operation) {
        
        // Basic input validation - can be extended
        return Optional.of(context)
            .filter(ctx -> ctx.correlationId() != null && !ctx.correlationId().isEmpty())
            .filter(ctx -> ctx.userId() != null && !ctx.userId().isEmpty())
            .map(Result::<SecurityContext, SecurityError>success)
            .orElse(Result.failure(SecurityError.ValidationError.invalidInput("context")));
    }
    
    /**
     * Execute the business operation with security context
     */
    private <T> CompletableFuture<Result<T, SecurityError>> executeSecureOperation(
            SecurityContext context,
            String operation,
            Supplier<CompletableFuture<Result<T, Exception>>> businessOperation) {
        
        return auditService.logOperation(context, operation, "EXECUTE_START")
            .thenCompose(auditResult -> 
                auditResult.match(
                    success -> businessOperation.get()
                        .thenApply(result -> result.mapError(this::convertToSecurityError))
                        .thenCompose(businessResult -> 
                            auditService.logOperationResult(context, operation, businessResult)
                                .thenApply(finalAudit -> businessResult)
                        ),
                    auditError -> CompletableFuture.completedFuture(Result.failure(auditError))
                )
            );
    }
    
    /**
     * Convert business exceptions to security errors
     */
    private SecurityError convertToSecurityError(Exception exception) {
        return SecurityError.ValidationError.invalidInput("operation: " + exception.getMessage());
    }
    
    /**
     * Validate authentication token
     */
    private AuthenticationStatus validateAuthenticationToken(SecurityContext context) {
        // System context is always valid
        if ("system".equals(context.userId())) {
            return AuthenticationStatus.VALID;
        }
        
        // For now, basic validation - would integrate with JWT in production
        if (context.sessionId() == null || context.sessionId().isEmpty()) {
            return AuthenticationStatus.MISSING_TOKEN;
        }
        
        // Simulate token validation
        return AuthenticationStatus.VALID;
    }
    
    /**
     * Check operation permissions
     */
    private AuthorizationStatus checkOperationPermission(SecurityContext context, String operation) {
        // System context has all permissions
        if (context.hasRole("SYSTEM")) {
            return AuthorizationStatus.ALLOWED;
        }
        
        // Basic role-based authorization
        return switch (operation) {
            case "SEND_NOTIFICATION" -> context.hasAnyRole("USER", "ADMIN") ? 
                AuthorizationStatus.ALLOWED : AuthorizationStatus.INSUFFICIENT_PERMISSIONS;
            case "BULK_NOTIFICATION" -> context.hasRole("ADMIN") ?
                AuthorizationStatus.ALLOWED : AuthorizationStatus.INSUFFICIENT_PERMISSIONS;
            case "ADMIN_OPERATION" -> context.hasRole("ADMIN") ?
                AuthorizationStatus.ALLOWED : AuthorizationStatus.INSUFFICIENT_PERMISSIONS;
            default -> AuthorizationStatus.ALLOWED;
        };
    }
    
    /**
     * Authentication status enumeration
     */
    private enum AuthenticationStatus {
        VALID, INVALID_TOKEN, MISSING_TOKEN, EXPIRED
    }
    
    /**
     * Authorization status enumeration
     */
    private enum AuthorizationStatus {
        ALLOWED, INSUFFICIENT_PERMISSIONS, RESOURCE_DENIED
    }
}