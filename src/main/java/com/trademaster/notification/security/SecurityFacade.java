package com.trademaster.notification.security;

import com.trademaster.notification.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Security Facade - Single entry point for all external security operations
 * 
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Facade Pattern - Rule #4
 * MANDATORY: Single Responsibility - Rule #2
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityFacade {
    
    private final SecurityMediator securityMediator;
    
    /**
     * Secure external API access with full security pipeline
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     * MANDATORY: Virtual Threads - Rule #12
     */
    public <T> CompletableFuture<Result<T, SecurityError>> secureExternalAccess(
            HttpServletRequest request,
            String operation,
            Supplier<CompletableFuture<Result<T, Exception>>> businessOperation) {
        
        String correlationId = generateCorrelationId();
        SecurityContext context = buildSecurityContext(request, correlationId);
        
        log.info("External access request - correlationId: {} operation: {} user: {} ip: {}",
                correlationId, operation, context.userId(), context.ipAddress());
        
        return securityMediator.mediateSecureAccess(context, operation, businessOperation);
    }
    
    /**
     * Secure internal service-to-service access (lightweight)
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     */
    public <T> CompletableFuture<Result<T, SecurityError>> secureInternalAccess(
            String operation,
            Supplier<CompletableFuture<Result<T, Exception>>> businessOperation) {
        
        String correlationId = generateCorrelationId();
        
        log.debug("Internal access request - correlationId: {} operation: {}",
                 correlationId, operation);
        
        return securityMediator.mediateInternalAccess(correlationId, operation, businessOperation);
    }
    
    /**
     * Create authenticated user context from HTTP request
     * 
     * MANDATORY: Builder Pattern - Rule #4
     */
    public SecurityContext buildSecurityContext(HttpServletRequest request, String correlationId) {
        String userId = extractUserId(request);
        String sessionId = extractSessionId(request);
        List<String> roles = extractUserRoles(request);
        String ipAddress = extractClientIpAddress(request);
        String userAgent = extractUserAgent(request);
        
        return SecurityContext.user(
            correlationId,
            userId,
            sessionId,
            roles,
            ipAddress,
            userAgent
        );
    }
    
    /**
     * Create system context for internal operations
     */
    public SecurityContext buildSystemContext() {
        String correlationId = generateCorrelationId();
        return SecurityContext.system(correlationId);
    }
    
    /**
     * Validate request has minimum security requirements
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    public Result<SecurityContext, SecurityError> validateMinimumSecurity(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        return switch (validateAuthorizationHeader(authHeader)) {
            case VALID -> {
                SecurityContext context = buildSecurityContext(request, generateCorrelationId());
                yield Result.success(context);
            }
            case MISSING -> Result.failure(SecurityError.AuthenticationError.missingToken());
            case INVALID -> Result.failure(SecurityError.AuthenticationError.invalidToken());
        };
    }
    
    /**
     * Generate correlation ID for request tracking
     */
    private String generateCorrelationId() {
        return "notif-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Extract user ID from request headers/tokens
     */
    private String extractUserId(HttpServletRequest request) {
        // Check for user header (would normally decode JWT)
        String userIdHeader = request.getHeader("X-User-ID");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            return userIdHeader;
        }
        
        // Fallback to session-based ID
        String sessionId = request.getSession().getId();
        return "user-" + sessionId.substring(0, 8);
    }
    
    /**
     * Extract session ID from request
     */
    private String extractSessionId(HttpServletRequest request) {
        return request.getSession().getId();
    }
    
    /**
     * Extract user roles from request/token
     */
    private List<String> extractUserRoles(HttpServletRequest request) {
        String rolesHeader = request.getHeader("X-User-Roles");
        if (rolesHeader != null && !rolesHeader.isEmpty()) {
            return List.of(rolesHeader.split(","));
        }
        
        // Default role for authenticated users
        return List.of("USER");
    }
    
    /**
     * Extract client IP address
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Extract user agent
     */
    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }
    
    /**
     * Validate authorization header
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    private AuthHeaderStatus validateAuthorizationHeader(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return AuthHeaderStatus.MISSING;
        }
        
        return switch (authHeader) {
            case String header when header.startsWith("Bearer ") && header.length() > 7 -> AuthHeaderStatus.VALID;
            case String header when header.startsWith("Basic ") && header.length() > 6 -> AuthHeaderStatus.VALID;
            default -> AuthHeaderStatus.INVALID;
        };
    }
    
    /**
     * Authorization header validation status
     */
    private enum AuthHeaderStatus {
        VALID, MISSING, INVALID
    }
}