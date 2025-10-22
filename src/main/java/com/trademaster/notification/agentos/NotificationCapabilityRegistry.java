package com.trademaster.notification.agentos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.trademaster.notification.agentos.AgentConstants.*;

/**
 * Notification Capability Registry
 *
 * Tracks performance, health, and usage statistics for all notification capabilities
 * in the TradeMaster Agent ecosystem. Provides real-time metrics for the
 * Agent Orchestration Service to make intelligent routing decisions.
 *
 * Capabilities Managed:
 * - EMAIL_NOTIFICATION: Email delivery with template support
 * - SMS_NOTIFICATION: SMS delivery with carrier routing
 * - PUSH_NOTIFICATION: Mobile push notifications
 * - IN_APP_NOTIFICATION: In-app notification delivery
 * - TEMPLATE_MANAGEMENT: Notification template CRUD
 * - PREFERENCE_CHECK: User notification preferences validation
 * - NOTIFICATION_HISTORY: Historical notification retrieval
 * - BATCH_NOTIFICATIONS: Batch notification processing
 *
 * MANDATORY COMPLIANCE:
 * - Rule #1: Java 24 Virtual Threads for async operations
 * - Rule #2: Single Responsibility - Capability metrics only
 * - Rule #3: Functional Programming - Stream API for collection processing
 * - Rule #9: Immutability - Immutable collections returned
 * - Rule #10: Lombok - @Slf4j, @RequiredArgsConstructor
 * - Rule #12: Virtual Threads - Lock-free concurrency with atomic types
 * - Rule #15: Structured Logging - Correlation IDs and metrics
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCapabilityRegistry {

    private final Map<String, CapabilityMetrics> capabilityMetrics = new ConcurrentHashMap<>();

    /**
     * Initialize capability metrics for notification agent
     *
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 1 (well below limit)
     */
    public void initializeCapabilities() {
        initializeCapability(CAPABILITY_EMAIL_NOTIFICATION);
        initializeCapability(CAPABILITY_SMS_NOTIFICATION);
        initializeCapability(CAPABILITY_PUSH_NOTIFICATION);
        initializeCapability(CAPABILITY_IN_APP_NOTIFICATION);
        initializeCapability(CAPABILITY_TEMPLATE_MANAGEMENT);
        initializeCapability(CAPABILITY_PREFERENCE_CHECK);
        initializeCapability(CAPABILITY_NOTIFICATION_HISTORY);
        initializeCapability(CAPABILITY_BATCH_NOTIFICATIONS);

        log.info("Notification capability registry initialized with {} capabilities",
                capabilityMetrics.size());
    }

    /**
     * Records successful execution of a capability
     *
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2 (well below limit)
     */
    public void recordSuccessfulExecution(String capability) {
        getMetricsForCapability(capability)
            .ifPresent(metrics -> {
                metrics.recordSuccess();
                log.debug("Recorded successful execution for capability: {}", capability);
            });
    }

    /**
     * Records failed execution of a capability
     *
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2 (well below limit)
     */
    public void recordFailedExecution(String capability, Exception error) {
        getMetricsForCapability(capability)
            .ifPresent(metrics -> {
                metrics.recordFailure(error);
                log.warn("Recorded failed execution for capability: {} - Error: {}",
                        capability, error.getMessage());
            });
    }

    /**
     * Records execution time for performance tracking
     *
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2 (well below limit)
     */
    public void recordExecutionTime(String capability, Duration executionTime) {
        getMetricsForCapability(capability)
            .ifPresent(metrics -> {
                metrics.recordExecutionTime(executionTime);
                log.debug("Recorded execution time for capability: {} - Duration: {}ms",
                        capability, executionTime.toMillis());
            });
    }

    /**
     * Gets current health score for a specific capability
     *
     * MANDATORY: Rule #11 - Result Types (Optional for null safety)
     * Complexity: 1
     */
    public Double getCapabilityHealthScore(String capability) {
        return getMetricsForCapability(capability)
            .map(CapabilityMetrics::getHealthScore)
            .orElse(0.0);
    }

    /**
     * Gets success rate for a specific capability
     *
     * Complexity: 1
     */
    public Double getCapabilitySuccessRate(String capability) {
        return getMetricsForCapability(capability)
            .map(CapabilityMetrics::getSuccessRate)
            .orElse(0.0);
    }

    /**
     * Gets average execution time for a specific capability
     *
     * Complexity: 1
     */
    public Double getCapabilityAverageExecutionTime(String capability) {
        return getMetricsForCapability(capability)
            .map(CapabilityMetrics::getAverageExecutionTime)
            .orElse(0.0);
    }

    /**
     * Calculates overall agent health score across all capabilities
     *
     * MANDATORY: Rule #13 - Stream API for collection processing
     * Complexity: 3
     */
    public Double calculateOverallHealthScore() {
        return java.util.Optional.of(capabilityMetrics)
            .filter(metrics -> !metrics.isEmpty())
            .map(metrics -> metrics.values().stream()
                    .mapToDouble(CapabilityMetrics::getHealthScore)
                    .average()
                    .orElse(0.0))
            .orElse(0.0);
    }

    /**
     * Gets performance summary for all capabilities
     *
     * MANDATORY: Rule #9 - Immutability (returns immutable Map)
     * MANDATORY: Rule #13 - Stream API
     * Complexity: 2
     */
    public Map<String, String> getPerformanceSummary() {
        return capabilityMetrics.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> formatPerformanceMetrics(entry.getValue())
            ));
    }

    /**
     * Gets all capabilities with their current metrics
     *
     * MANDATORY: Rule #9 - Immutability
     * Complexity: 1
     */
    public Map<String, Object> getAllCapabilities() {
        return Map.copyOf(capabilityMetrics);
    }

    /**
     * Resets metrics for a specific capability
     *
     * Complexity: 2
     */
    public void resetCapabilityMetrics(String capability) {
        getMetricsForCapability(capability)
            .ifPresent(metrics -> {
                metrics.reset();
                log.info("Reset metrics for capability: {}", capability);
            });
    }

    /**
     * Initializes a new capability with default metrics
     *
     * MANDATORY: Rule #3 - Functional Programming (no if-else)
     * Complexity: 1
     */
    private void initializeCapability(String capability) {
        capabilityMetrics.put(capability, new CapabilityMetrics(capability));
        log.debug("Initialized capability: {}", capability);
    }

    /**
     * Gets Optional<CapabilityMetrics> for capability
     *
     * MANDATORY: Rule #11 - Result Types (Optional instead of null)
     * Complexity: 1
     */
    private java.util.Optional<CapabilityMetrics> getMetricsForCapability(String capability) {
        return java.util.Optional.ofNullable(capabilityMetrics.get(capability));
    }

    /**
     * Formats performance metrics as human-readable string
     *
     * MANDATORY: Rule #3 - Functional Programming
     * Complexity: 1
     */
    private String formatPerformanceMetrics(CapabilityMetrics metrics) {
        return String.format(
            "Success Rate: %.2f%%, Avg Time: %.2fms, Health: %.2f",
            metrics.getSuccessRate() * 100,
            metrics.getAverageExecutionTime(),
            metrics.getHealthScore()
        );
    }

    /**
     * Internal class to track metrics for each capability
     *
     * MANDATORY COMPLIANCE:
     * - Rule #9: Immutability - Only atomic types for thread-safe mutation
     * - Rule #12: Virtual Threads - Lock-free concurrency patterns
     * - Rule #19: Access Control - Private static inner class
     */
    private static class CapabilityMetrics {
        private final String capabilityName;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private volatile LocalDateTime lastExecution = LocalDateTime.now();
        private volatile String lastError = null;

        public CapabilityMetrics(String capabilityName) {
            this.capabilityName = capabilityName;
        }

        public void recordSuccess() {
            successCount.incrementAndGet();
            lastExecution = LocalDateTime.now();
        }

        public void recordFailure(Exception error) {
            failureCount.incrementAndGet();
            lastExecution = LocalDateTime.now();
            lastError = error.getMessage();
        }

        public void recordExecutionTime(Duration executionTime) {
            totalExecutionTime.addAndGet(executionTime.toMillis());
            executionCount.incrementAndGet();
        }

        public Double getSuccessRate() {
            long total = successCount.get() + failureCount.get();
            return total > 0 ? (double) successCount.get() / total : 1.0;
        }

        public Double getAverageExecutionTime() {
            int count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
        }

        /**
         * Calculates health score based on success rate, performance, and recency
         *
         * MANDATORY: Rule #5 - Cognitive Complexity ≤7
         * Complexity: 3
         */
        public Double getHealthScore() {
            double successRate = getSuccessRate();
            double avgTime = getAverageExecutionTime();
            double recency = getRecencyScore();

            // Health score: success rate (60%), performance (25%), recency (15%)
            return (successRate * 0.60) +
                   (getPerformanceScore(avgTime) * 0.25) +
                   (recency * 0.15);
        }

        /**
         * Calculates recency score based on last execution time
         *
         * MANDATORY: Rule #14 - Pattern Matching
         * Complexity: 5 (switch expression)
         */
        private Double getRecencyScore() {
            Duration timeSinceLastExecution = Duration.between(lastExecution, LocalDateTime.now());
            long minutesSinceExecution = timeSinceLastExecution.toMinutes();

            return switch (Long.valueOf(minutesSinceExecution)) {
                case Long m when m <= 5 -> 1.0;
                case Long m when m <= 30 -> 0.8;
                case Long m when m <= 120 -> 0.6;
                case Long m when m <= 360 -> 0.4;
                default -> 0.2;
            };
        }

        /**
         * Calculates performance score based on average execution time
         *
         * MANDATORY: Rule #14 - Pattern Matching
         * Complexity: 5 (switch expression)
         */
        private Double getPerformanceScore(double avgTimeMs) {
            // Notification-specific performance thresholds
            return switch (Double.valueOf(avgTimeMs)) {
                case Double t when t <= 10 -> 1.0;    // Excellent for in-app
                case Double t when t <= 50 -> 0.9;    // Good for push notifications
                case Double t when t <= 200 -> 0.7;   // Average for email
                case Double t when t <= 1000 -> 0.5;  // Poor for batch operations
                default -> 0.2;  // Very poor
            };
        }

        public void reset() {
            successCount.set(0);
            failureCount.set(0);
            totalExecutionTime.set(0);
            executionCount.set(0);
            lastExecution = LocalDateTime.now();
            lastError = null;
        }
    }
}
