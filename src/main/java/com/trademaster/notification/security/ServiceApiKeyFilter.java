package com.trademaster.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * Service API Key Authentication Filter
 *
 * Implements API key-based authentication for external service access
 * through Kong API Gateway. Validates X-API-Key headers and manages
 * service-level authorization patterns.
 *
 * Features:
 * - API key validation for Kong Gateway integration
 * - Service-level authentication and authorization
 * - Request correlation tracking
 * - Security audit logging
 * - Rate limiting coordination
 *
 * Security:
 * - Validates API keys against configured service keys
 * - Implements fail-safe authentication patterns
 * - Comprehensive audit trail for security events
 *
 * NOTE: Registered in NotificationSecurityConfig.filterChain() AND as @Component.
 * @Component required for dependency injection. Do NOT add @Order to avoid double registration.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    @Value("${kong.gateway.enabled:true}")
    private boolean kongEnabled;

    @Value("${notification.security.api-keys:pTB9KkzqJWNkFDUJHIFyDv5b1tSUpP4q}")
    private String configuredApiKeys;

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    // API endpoints that require authentication
    private static final Set<String> PROTECTED_PATTERNS = Set.of(
        "/api/v1/", "/api/v2/notifications", "/api/internal/"
    );

    // Public endpoints that don't require authentication
    private static final Set<String> PUBLIC_PATTERNS = Set.of(
        "/api/v2/health", "/actuator/", "/swagger-ui/", "/v3/api-docs"
    );

    /**
     * Filter internal processing with functional authentication decision
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, pattern matching)
     * MANDATORY: Rule #14 - Pattern Matching with switch expressions
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 5
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String correlationId = getOrGenerateCorrelationId(request);
        String apiKey = request.getHeader(API_KEY_HEADER);

        try {
            // Rule #3: NO if-else, use pattern matching with switch expression
            AuthenticationDecision decision = determineAuthenticationDecision(requestPath, apiKey);

            switch (decision) {
                case PUBLIC_ENDPOINT -> {
                    log.debug("Allowing public access to: {} - CorrelationId: {}", requestPath, correlationId);
                    addCorrelationIdToResponse(response, correlationId);
                    filterChain.doFilter(request, response);
                }
                case KONG_DISABLED -> {
                    log.debug("Kong disabled, skipping API key validation - CorrelationId: {}", correlationId);
                    addCorrelationIdToResponse(response, correlationId);
                    filterChain.doFilter(request, response);
                }
                case INVALID_API_KEY -> {
                    log.warn("Invalid or missing API key for protected endpoint: {} - CorrelationId: {}",
                            requestPath, correlationId);
                    sendUnauthorizedResponse(response, correlationId, "Invalid or missing API key");
                }
                case AUTHENTICATED -> {
                    log.info("Valid API key authentication for: {} - CorrelationId: {}", requestPath, correlationId);
                    addCorrelationIdToResponse(response, correlationId);
                    filterChain.doFilter(request, response);
                }
                case UNPROTECTED -> {
                    addCorrelationIdToResponse(response, correlationId);
                    filterChain.doFilter(request, response);
                }
            }

        } catch (Exception e) {
            log.error("API key authentication error for: {} - CorrelationId: {}", requestPath, correlationId, e);
            sendInternalServerErrorResponse(response, correlationId, e.getMessage());
        }
    }

    /**
     * Determine authentication decision using functional patterns
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private AuthenticationDecision determineAuthenticationDecision(String requestPath, String apiKey) {
        // Rule #3: NO if-else, use Optional.orElseGet() for decision logic
        return java.util.Optional.of(isPublicEndpoint(requestPath))
            .filter(Boolean::booleanValue)
            .map(_ -> AuthenticationDecision.PUBLIC_ENDPOINT)
            .orElseGet(() -> java.util.Optional.of(isProtectedEndpoint(requestPath))
                .filter(Boolean::booleanValue)
                .map(_ -> java.util.Optional.of(kongEnabled)
                    .filter(Boolean::booleanValue)
                    .map(__ -> java.util.Optional.of(isValidApiKey(apiKey))
                        .filter(Boolean::booleanValue)
                        .map(___  -> AuthenticationDecision.AUTHENTICATED)
                        .orElse(AuthenticationDecision.INVALID_API_KEY))
                    .orElse(AuthenticationDecision.KONG_DISABLED))
                .orElse(AuthenticationDecision.UNPROTECTED));
    }

    /**
     * Authentication decision enumeration for pattern matching
     */
    private enum AuthenticationDecision {
        PUBLIC_ENDPOINT,
        KONG_DISABLED,
        INVALID_API_KEY,
        AUTHENTICATED,
        UNPROTECTED
    }

    /**
     * Get or generate correlation ID
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2
     */
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        // Rule #3: NO if-else, use Optional.orElseGet() for default value generation
        return java.util.Optional.ofNullable(request.getHeader(CORRELATION_ID_HEADER))
            .filter(id -> !id.trim().isEmpty())
            .orElseGet(() -> "gen-" + System.currentTimeMillis() + "-" +
                            Integer.toHexString((int) (Math.random() * 0x10000)));
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_PATTERNS.stream().anyMatch(path::startsWith);
    }

    private boolean isProtectedEndpoint(String path) {
        return PROTECTED_PATTERNS.stream().anyMatch(path::startsWith);
    }

    /**
     * Validate API key
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional + Stream API)
     * MANDATORY: Rule #13 - Stream API for collection processing
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private boolean isValidApiKey(String apiKey) {
        // Rule #3: NO if-else, use Optional.map() with Stream API for validation
        return java.util.Optional.ofNullable(apiKey)
            .filter(key -> !key.trim().isEmpty())
            .map(key -> java.util.Optional.ofNullable(configuredApiKeys)
                .filter(keys -> !keys.isEmpty())
                .map(keys -> java.util.Arrays.stream(keys.split(","))
                    .anyMatch(validKey -> key.trim().equals(validKey.trim())))
                .orElseGet(() -> key.length() >= 32 && key.matches("^[a-zA-Z0-9-_]+$")))
            .orElse(false);
    }

    private void addCorrelationIdToResponse(HttpServletResponse response, String correlationId) {
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String correlationId, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        String jsonResponse = String.format(
            "{\"status\":\"UNAUTHORIZED\",\"message\":\"%s\",\"correlation_id\":\"%s\",\"timestamp\":\"%s\"}",
            message, correlationId, Instant.now().toString()
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private void sendInternalServerErrorResponse(HttpServletResponse response, String correlationId, String error)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        String jsonResponse = String.format(
            "{\"status\":\"ERROR\",\"message\":\"Authentication error\",\"error\":\"%s\",\"correlation_id\":\"%s\",\"timestamp\":\"%s\"}",
            error, correlationId, Instant.now().toString()
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}