package com.trademaster.notification.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Logging Interceptor for Request Correlation
 * 
 * MANDATORY: Structured Logging - Rule #15
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Zero Trust Security - Rule #6
 * 
 * Automatically adds correlation IDs and request context to all HTTP requests
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    private final LogAggregationService logAggregationService;
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String REQUEST_START_TIME = "request.start.time";

    /**
     * Pre-handle request to initialize logging context
     * 
     * MANDATORY: Structured Logging - Rule #15
     * MANDATORY: Zero Trust Security - Rule #6
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(REQUEST_START_TIME, startTime);
        
        // Extract or generate correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = logAggregationService.generateCorrelationId();
        }
        
        // Extract user ID if available
        String userId = request.getHeader(USER_ID_HEADER);
        
        // Initialize request context
        logAggregationService.initializeRequestContext(correlationId, userId);
        
        // Add correlation ID to response headers
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        // Log request start
        log.info("Request started: {} {} from {} - User-Agent: {}", 
            request.getMethod(), 
            request.getRequestURI(),
            getClientIP(request),
            request.getHeader("User-Agent"));
        
        return true;
    }

    /**
     * Post-handle request to log completion
     * 
     * MANDATORY: Structured Logging - Rule #15
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) {
        
        try {
            Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
            
            // Log request completion
            if (ex != null) {
                log.error("Request failed: {} {} - Status: {} - Duration: {}ms - Error: {}", 
                    request.getMethod(), 
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    ex.getMessage(),
                    ex);
            } else {
                log.info("Request completed: {} {} - Status: {} - Duration: {}ms", 
                    request.getMethod(), 
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
            }
            
            // Log performance metric
            logAggregationService.logPerformanceMetric(
                "http_request_duration_ms",
                duration,
                "milliseconds",
                java.util.Map.of(
                    "method", request.getMethod(),
                    "uri", request.getRequestURI(),
                    "status", String.valueOf(response.getStatus())
                )
            );
            
        } finally {
            // Clear request context
            logAggregationService.clearRequestContext();
        }
    }

    /**
     * Get client IP address, handling proxies and load balancers
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    private String getClientIP(HttpServletRequest request) {
        // Check common proxy headers
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, get the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
}