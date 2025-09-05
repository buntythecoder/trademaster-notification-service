package com.trademaster.notification.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Handshake Interceptor for Authentication
 * 
 * MANDATORY: Security First - Rule #6
 * MANDATORY: Zero Trust Security - Rule #6
 */
@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    // Token validation constants - Rule #17
    private static final int MINIMUM_TOKEN_LENGTH = 10;
    
    /**
     * Pre-handshake validation and attribute setting
     * 
     * MANDATORY: Security First - Rule #6
     */
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, 
            ServerHttpResponse response,
            WebSocketHandler wsHandler, 
            Map<String, Object> attributes) throws Exception {
        
        // Extract user ID from query parameters or headers
        String userId = extractUserId(request);
        String userRole = extractUserRole(request);
        String authToken = extractAuthToken(request);
        
        if (userId == null || authToken == null) {
            log.warn("WebSocket handshake rejected - missing user ID or auth token: {}", 
                    request.getRemoteAddress());
            return false;
        }
        
        // Validate authentication token
        if (!validateAuthToken(authToken, userId)) {
            log.warn("WebSocket handshake rejected - invalid auth token for user: {}", userId);
            return false;
        }
        
        // Store user information in session attributes
        attributes.put("userId", userId);
        attributes.put("userRole", userRole != null ? userRole : "USER");
        attributes.put("authToken", authToken);
        attributes.put("connectTime", System.currentTimeMillis());
        attributes.put("remoteAddress", request.getRemoteAddress());
        
        log.info("WebSocket handshake approved for user: {}, role: {}, from: {}", 
                userId, userRole, request.getRemoteAddress());
        
        return true;
    }
    
    /**
     * Post-handshake cleanup
     */
    @Override
    public void afterHandshake(
            ServerHttpRequest request, 
            ServerHttpResponse response,
            WebSocketHandler wsHandler, 
            Exception exception) {
        
        if (exception != null) {
            log.error("WebSocket handshake failed for request: {}", 
                     request.getRemoteAddress(), exception);
        }
    }
    
    // Private helper methods
    
    private String extractUserId(ServerHttpRequest request) {
        // Try query parameter first
        String userId = request.getURI().getQuery();
        if (userId != null && userId.contains("userId=")) {
            return extractQueryParam(userId, "userId");
        }
        
        // Try header
        return getFirstHeaderValue(request, "X-User-ID");
    }
    
    private String extractUserRole(ServerHttpRequest request) {
        // Try query parameter first
        String query = request.getURI().getQuery();
        if (query != null && query.contains("userRole=")) {
            return extractQueryParam(query, "userRole");
        }
        
        // Try header
        return getFirstHeaderValue(request, "X-User-Role");
    }
    
    private String extractAuthToken(ServerHttpRequest request) {
        // Try Authorization header first
        String authHeader = getFirstHeaderValue(request, "Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Try query parameter
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            return extractQueryParam(query, "token");
        }
        
        return null;
    }
    
    private boolean validateAuthToken(String token, String userId) {
        // In production, this would validate the JWT token
        // and ensure it belongs to the specified user
        
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        // For now, just check basic format
        // In real implementation:
        // 1. Decode JWT token
        // 2. Verify signature
        // 3. Check expiration
        // 4. Validate user ID claim
        // 5. Check token permissions
        
        try {
            // Placeholder validation - replace with actual JWT validation
            return token.length() > MINIMUM_TOKEN_LENGTH && !token.equals("invalid");
        } catch (Exception e) {
            log.error("Token validation failed for user: {}", userId, e);
            return false;
        }
    }
    
    private String extractQueryParam(String query, String paramName) {
        if (query == null) {
            return null;
        }
        
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith(paramName + "=")) {
                return param.substring(paramName.length() + 1);
            }
        }
        
        return null;
    }
    
    private String getFirstHeaderValue(ServerHttpRequest request, String headerName) {
        return request.getHeaders().getFirst(headerName);
    }
}