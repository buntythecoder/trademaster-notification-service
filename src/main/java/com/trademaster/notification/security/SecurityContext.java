package com.trademaster.notification.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Security Context for Zero Trust Architecture
 * 
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Immutability - Rule #9
 * MANDATORY: Records Usage - Rule #9
 */
public record SecurityContext(
    String correlationId,
    String userId,
    String sessionId,
    List<String> roles,
    Map<String, String> attributes,
    String ipAddress,
    String userAgent,
    LocalDateTime timestamp,
    SecurityRisk riskLevel
) {
    
    /**
     * Security risk levels
     */
    public enum SecurityRisk {
        LOW(0.0, 0.3),
        MEDIUM(0.3, 0.6),
        HIGH(0.6, 0.8),
        CRITICAL(0.8, 1.0);
        
        private final double minScore;
        private final double maxScore;
        
        SecurityRisk(double minScore, double maxScore) {
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
        
        public static SecurityRisk fromScore(double score) {
            return switch (score) {
                case double s when s < 0.3 -> LOW;
                case double s when s < 0.6 -> MEDIUM;
                case double s when s < 0.8 -> HIGH;
                default -> CRITICAL;
            };
        }
        
        public boolean isAcceptable() {
            return this == LOW || this == MEDIUM;
        }
        
        public double getMinScore() {
            return minScore;
        }
        
        public double getMaxScore() {
            return maxScore;
        }
    }
    
    /**
     * Builder for SecurityContext
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Check if user has required role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Check if user has any of the required roles
     */
    public boolean hasAnyRole(String... requiredRoles) {
        return List.of(requiredRoles).stream()
            .anyMatch(this::hasRole);
    }
    
    /**
     * Get attribute value
     */
    public Optional<String> getAttribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }
    
    /**
     * Check if risk level is acceptable
     */
    public boolean isRiskAcceptable() {
        return riskLevel.isAcceptable();
    }
    
    /**
     * Create anonymous context for system operations
     */
    public static SecurityContext system(String correlationId) {
        return builder()
            .correlationId(correlationId)
            .userId("system")
            .sessionId("system-session")
            .roles(List.of("SYSTEM"))
            .attributes(Map.of("type", "system"))
            .ipAddress("127.0.0.1")
            .userAgent("TradeMaster-System")
            .timestamp(LocalDateTime.now())
            .riskLevel(SecurityRisk.LOW)
            .build();
    }
    
    /**
     * Create user context from request
     */
    public static SecurityContext user(
        String correlationId,
        String userId,
        String sessionId,
        List<String> roles,
        String ipAddress,
        String userAgent
    ) {
        return builder()
            .correlationId(correlationId)
            .userId(userId)
            .sessionId(sessionId)
            .roles(roles)
            .attributes(Map.of())
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .timestamp(LocalDateTime.now())
            .riskLevel(SecurityRisk.MEDIUM) // Default to medium, will be assessed
            .build();
    }
    
    /**
     * Builder pattern for SecurityContext
     */
    public static class Builder {
        private String correlationId;
        private String userId;
        private String sessionId;
        private List<String> roles = List.of();
        private Map<String, String> attributes = Map.of();
        private String ipAddress;
        private String userAgent;
        private LocalDateTime timestamp;
        private SecurityRisk riskLevel = SecurityRisk.MEDIUM;
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder roles(List<String> roles) {
            this.roles = List.copyOf(roles);
            return this;
        }
        
        public Builder attributes(Map<String, String> attributes) {
            this.attributes = Map.copyOf(attributes);
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder riskLevel(SecurityRisk riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }
        
        public SecurityContext build() {
            return new SecurityContext(
                correlationId,
                userId,
                sessionId,
                roles,
                attributes,
                ipAddress,
                userAgent,
                timestamp != null ? timestamp : LocalDateTime.now(),
                riskLevel
            );
        }
    }
}