package com.trademaster.notification.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Risk Assessment Service for Zero Trust Security
 * 
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Pattern Matching - Rule #14
 * MANDATORY: Functional Programming - Rule #3
 */
@Service
@Slf4j
public class RiskAssessmentService {
    
    private final Map<String, AtomicInteger> userRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> userLastActivity = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> ipRequestCounts = new ConcurrentHashMap<>();
    
    /**
     * Calculate risk score based on multiple factors
     * 
     * MANDATORY: Pattern Matching - Rule #14
     * MANDATORY: Stream API - Rule #13
     */
    public double calculateRiskScore(SecurityContext context, String operation) {
        log.debug("Calculating risk score for user: {} operation: {}", 
                 context.userId(), operation);
        
        double baseRisk = calculateBaseRisk(context);
        double behaviorRisk = calculateBehaviorRisk(context);
        double operationRisk = calculateOperationRisk(operation);
        double timeRisk = calculateTimeBasedRisk(context);
        double ipRisk = calculateIpRisk(context);
        
        double totalRisk = (baseRisk * 0.2) + 
                          (behaviorRisk * 0.3) + 
                          (operationRisk * 0.2) + 
                          (timeRisk * 0.1) + 
                          (ipRisk * 0.2);
        
        // Update tracking data
        updateUserActivity(context);
        
        log.debug("Risk assessment completed - user: {} operation: {} score: {}", 
                 context.userId(), operation, totalRisk);
        
        return Math.min(1.0, Math.max(0.0, totalRisk));
    }
    
    /**
     * Calculate base risk from user context
     */
    private double calculateBaseRisk(SecurityContext context) {
        return switch (context.riskLevel()) {
            case LOW -> 0.1;
            case MEDIUM -> 0.3;
            case HIGH -> 0.6;
            case CRITICAL -> 0.9;
        };
    }
    
    /**
     * Calculate behavior-based risk
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #14 - Pattern Matching with switch
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 3
     */
    private double calculateBehaviorRisk(SecurityContext context) {
        String userId = context.userId();

        // Rule #3: NO if-else, use Optional.orElseGet() for conditional logic
        return java.util.Optional.of(userId)
            .filter("system"::equals)
            .map(_ -> 0.0)
            .orElseGet(() -> {
                int requestCount = userRequestCounts
                    .computeIfAbsent(userId, k -> new AtomicInteger(0))
                    .get();

                return switch (requestCount) {
                    case int count when count < 10 -> 0.1;
                    case int count when count < 50 -> 0.2;
                    case int count when count < 100 -> 0.4;
                    case int count when count < 200 -> 0.6;
                    default -> 0.8;
                };
            });
    }
    
    /**
     * Calculate operation-specific risk
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    private double calculateOperationRisk(String operation) {
        return switch (operation) {
            case "SEND_NOTIFICATION" -> 0.1;
            case "BULK_NOTIFICATION" -> 0.3;
            case "ADMIN_OPERATION" -> 0.5;
            case "SYSTEM_OPERATION" -> 0.0;
            default -> 0.2;
        };
    }
    
    /**
     * Calculate time-based risk factors
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #14 - Pattern Matching with switch
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private double calculateTimeBasedRisk(SecurityContext context) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        // Rule #3: NO if-else, use Optional.orElseGet() for nested conditional logic
        return java.util.Optional.of(hour)
            .filter(h -> h >= 22 || h <= 6)
            .map(_ -> 0.3)
            .orElseGet(() -> java.util.Optional.ofNullable(userLastActivity.get(context.userId()))
                .map(lastActivity -> {
                    long secondsSinceLastActivity = ChronoUnit.SECONDS.between(lastActivity, now);
                    return switch ((int) secondsSinceLastActivity) {
                        case int seconds when seconds < 1 -> 0.5;  // Very rapid requests
                        case int seconds when seconds < 5 -> 0.3;  // Rapid requests
                        default -> 0.1;
                    };
                })
                .orElse(0.1));
    }
    
    /**
     * Calculate IP-based risk
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #14 - Pattern Matching with switch
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 3
     */
    private double calculateIpRisk(SecurityContext context) {
        String ipAddress = context.ipAddress();

        // Rule #3: NO if-else, use Optional.orElseGet() for conditional logic
        return java.util.Optional.of(ipAddress)
            .filter(ip -> ip.startsWith("127.") || ip.startsWith("192.168.") ||
                         ip.startsWith("10.") || ip.equals("localhost"))
            .map(_ -> 0.0)
            .orElseGet(() -> {
                int ipRequestCount = ipRequestCounts
                    .computeIfAbsent(ipAddress, k -> new AtomicInteger(0))
                    .incrementAndGet();

                return switch (ipRequestCount) {
                    case int count when count < 20 -> 0.1;
                    case int count when count < 100 -> 0.2;
                    case int count when count < 500 -> 0.4;
                    default -> 0.7;
                };
            });
    }
    
    /**
     * Update user activity tracking
     */
    private void updateUserActivity(SecurityContext context) {
        String userId = context.userId();
        LocalDateTime now = LocalDateTime.now();
        
        userRequestCounts
            .computeIfAbsent(userId, k -> new AtomicInteger(0))
            .incrementAndGet();
        
        userLastActivity.put(userId, now);
    }
    
    /**
     * Reset risk tracking data (for maintenance)
     */
    public void resetRiskData() {
        log.info("Resetting risk assessment tracking data");
        userRequestCounts.clear();
        userLastActivity.clear();
        ipRequestCounts.clear();
    }
    
    /**
     * Get current risk statistics
     */
    public Map<String, Object> getRiskStatistics() {
        return Map.of(
            "trackedUsers", userRequestCounts.size(),
            "trackedIPs", ipRequestCounts.size(),
            "totalRequests", userRequestCounts.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum()
        );
    }
}