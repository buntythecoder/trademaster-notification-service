package com.trademaster.notification.service;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.constant.NotificationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Service for Notifications
 * 
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Pattern Matching - Rule #14
 * MANDATORY: Constants Usage - Rule #17
 */
@Service
@Slf4j
public class RateLimitService {
    
    // Sliding window counters using atomic operations
    private final ConcurrentHashMap<String, AtomicInteger> windowCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> windowStarts = new ConcurrentHashMap<>();
    
    /**
     * Check if notification type and count is allowed under rate limits
     * 
     * MANDATORY: Functional Programming - Rule #3
     * MANDATORY: Pattern Matching - Rule #14
     */
    public boolean isAllowed(String notificationType, int count) {
        String rateLimitKey = notificationType + ":global";
        int limit = getRateLimitForType(notificationType);
        
        return checkRateLimit(rateLimitKey, limit, count);
    }
    
    /**
     * Check if individual notification request is allowed
     */
    public boolean isAllowed(NotificationRequest request) {
        String rateLimitKey = createRateLimitKey(request);
        int limit = getRateLimitForNotificationType(request.type());
        
        return checkRateLimit(rateLimitKey, limit, 1);
    }
    
    /**
     * Record notification attempt
     */
    public void recordAttempt(NotificationRequest request) {
        String rateLimitKey = createRateLimitKey(request);
        incrementCounter(rateLimitKey);
    }
    
    /**
     * Get rate limit using pattern matching
     * 
     * MANDATORY: Pattern Matching - Rule #14
     * MANDATORY: Constants Usage - Rule #17
     */
    private int getRateLimitForType(String type) {
        return switch (type.toLowerCase()) {
            case "email" -> NotificationConstants.EMAIL_RATE_LIMIT_PER_HOUR;
            case "sms" -> NotificationConstants.SMS_RATE_LIMIT_PER_HOUR;
            case "push" -> NotificationConstants.PUSH_RATE_LIMIT_PER_HOUR;
            default -> 100; // Conservative default
        };
    }
    
    /**
     * Get rate limit for notification type enum
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    private int getRateLimitForNotificationType(NotificationRequest.NotificationType type) {
        return switch (type) {
            case EMAIL -> NotificationConstants.EMAIL_RATE_LIMIT_PER_HOUR;
            case SMS -> NotificationConstants.SMS_RATE_LIMIT_PER_HOUR;
            case PUSH -> NotificationConstants.PUSH_RATE_LIMIT_PER_HOUR;
            case IN_APP -> 1000; // High limit for in-app notifications
        };
    }
    
    private String createRateLimitKey(NotificationRequest request) {
        // Rate limit by type and recipient to prevent spam
        return request.type().name() + ":" + request.recipient();
    }
    
    private boolean checkRateLimit(String key, int limit, int requestedCount) {
        LocalDateTime now = LocalDateTime.now();
        
        return Optional.ofNullable(windowStarts.get(key))
            .filter(windowStart -> windowStart.isAfter(now.minusHours(1)))
            .map(windowStart -> checkExistingWindow(key, limit, requestedCount))
            .orElseGet(() -> initializeNewWindow(key, limit, requestedCount, now));
    }
    
    private boolean checkExistingWindow(String key, int limit, int requestedCount) {
        return Optional.ofNullable(windowCounts.get(key))
            .map(AtomicInteger::get)
            .map(currentCount -> 
                Optional.of(currentCount + requestedCount)
                    .filter(totalCount -> totalCount <= limit)
                    .map(totalCount -> {
                        incrementCounter(key, requestedCount);
                        return true;
                    })
                    .orElseGet(() -> {
                        log.warn("Rate limit exceeded for key: {} (current: {}, requested: {}, limit: {})",
                                key, currentCount, requestedCount, limit);
                        return false;
                    })
            )
            .orElse(false);
    }
    
    private boolean initializeNewWindow(String key, int limit, int requestedCount, LocalDateTime now) {
        windowStarts.put(key, now);
        windowCounts.put(key, new AtomicInteger(0));
        
        return Optional.of(requestedCount)
            .filter(count -> count <= limit)
            .map(count -> {
                incrementCounter(key, requestedCount);
                return true;
            })
            .orElse(false);
    }
    
    private void incrementCounter(String key) {
        incrementCounter(key, 1);
    }
    
    private void incrementCounter(String key, int count) {
        windowCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).addAndGet(count);
    }
    
    /**
     * Clean up old rate limit entries
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Stream API)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * MANDATORY: Rule #22 - Memory Management
     * Complexity: 2
     */
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);

        // Remove old window start entries
        windowStarts.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

        // Remove counters for windows that no longer exist (Rule #3 - NO if-else, functional removeIf)
        windowCounts.keySet().removeIf(key -> !windowStarts.containsKey(key));

        log.debug("Cleaned up rate limit entries older than 2 hours");
    }
}