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
     * MANDATORY: Pattern Matching - Rule #14
     */
    private double calculateBehaviorRisk(SecurityContext context) {
        String userId = context.userId();
        
        // System users have low behavior risk
        if ("system".equals(userId)) {
            return 0.0;
        }
        
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
     */
    private double calculateTimeBasedRisk(SecurityContext context) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Higher risk during off-hours (10 PM - 6 AM)
        if (hour >= 22 || hour <= 6) {
            return 0.3;
        }
        
        // Check for rapid successive requests
        LocalDateTime lastActivity = userLastActivity.get(context.userId());
        if (lastActivity != null) {
            long secondsSinceLastActivity = ChronoUnit.SECONDS.between(lastActivity, now);
            if (secondsSinceLastActivity < 1) {
                return 0.5; // Very rapid requests
            } else if (secondsSinceLastActivity < 5) {
                return 0.3; // Rapid requests
            }
        }
        
        return 0.1;
    }
    
    /**
     * Calculate IP-based risk
     */
    private double calculateIpRisk(SecurityContext context) {
        String ipAddress = context.ipAddress();
        
        // Local/trusted IPs have lower risk
        if (ipAddress.startsWith("127.") || ipAddress.startsWith("192.168.") || 
            ipAddress.startsWith("10.") || ipAddress.equals("localhost")) {
            return 0.0;
        }
        
        int ipRequestCount = ipRequestCounts
            .computeIfAbsent(ipAddress, k -> new AtomicInteger(0))
            .incrementAndGet();
        
        return switch (ipRequestCount) {
            case int count when count < 20 -> 0.1;
            case int count when count < 100 -> 0.2;
            case int count when count < 500 -> 0.4;
            default -> 0.7;
        };
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