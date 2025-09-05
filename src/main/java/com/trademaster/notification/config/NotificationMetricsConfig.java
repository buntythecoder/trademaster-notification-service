package com.trademaster.notification.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuator.health.Health;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive Metrics Configuration for Notification Service
 * 
 * MANDATORY: Production Monitoring - TradeMaster Standards
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * 
 * Provides comprehensive metrics collection for notification operations
 */
@Configuration
public class NotificationMetricsConfig {

    // Notification counters
    private final AtomicLong emailNotificationsSent = new AtomicLong(0);
    private final AtomicLong smsNotificationsSent = new AtomicLong(0);
    private final AtomicLong pushNotificationsSent = new AtomicLong(0);
    private final AtomicLong inAppNotificationsSent = new AtomicLong(0);
    
    private final AtomicLong emailNotificationsFailed = new AtomicLong(0);
    private final AtomicLong smsNotificationsFailed = new AtomicLong(0);
    private final AtomicLong pushNotificationsFailed = new AtomicLong(0);
    private final AtomicLong inAppNotificationsFailed = new AtomicLong(0);
    
    private final AtomicInteger activeNotificationJobs = new AtomicInteger(0);
    private final AtomicInteger rateLimitViolations = new AtomicInteger(0);
    private final AtomicInteger circuitBreakerTrips = new AtomicInteger(0);

    /**
     * Email notification success counter
     */
    @Bean
    public Counter emailNotificationSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_email_success_total")
            .description("Total number of successful email notifications sent")
            .tag("service", "notification-service")
            .tag("type", "email")
            .register(meterRegistry);
    }

    /**
     * SMS notification success counter
     */
    @Bean
    public Counter smsNotificationSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_sms_success_total")
            .description("Total number of successful SMS notifications sent")
            .tag("service", "notification-service")
            .tag("type", "sms")
            .register(meterRegistry);
    }

    /**
     * Push notification success counter
     */
    @Bean
    public Counter pushNotificationSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_push_success_total")
            .description("Total number of successful push notifications sent")
            .tag("service", "notification-service")
            .tag("type", "push")
            .register(meterRegistry);
    }

    /**
     * In-app notification success counter
     */
    @Bean
    public Counter inAppNotificationSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_inapp_success_total")
            .description("Total number of successful in-app notifications sent")
            .tag("service", "notification-service")
            .tag("type", "inapp")
            .register(meterRegistry);
    }

    /**
     * Email notification failure counter
     */
    @Bean
    public Counter emailNotificationFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_email_failure_total")
            .description("Total number of failed email notifications")
            .tag("service", "notification-service")
            .tag("type", "email")
            .register(meterRegistry);
    }

    /**
     * SMS notification failure counter
     */
    @Bean
    public Counter smsNotificationFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_sms_failure_total")
            .description("Total number of failed SMS notifications")
            .tag("service", "notification-service")
            .tag("type", "sms")
            .register(meterRegistry);
    }

    /**
     * Push notification failure counter
     */
    @Bean
    public Counter pushNotificationFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_push_failure_total")
            .description("Total number of failed push notifications")
            .tag("service", "notification-service")
            .tag("type", "push")
            .register(meterRegistry);
    }

    /**
     * In-app notification failure counter
     */
    @Bean
    public Counter inAppNotificationFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_inapp_failure_total")
            .description("Total number of failed in-app notifications")
            .tag("service", "notification-service")
            .tag("type", "inapp")
            .register(meterRegistry);
    }

    /**
     * Notification processing duration timer
     */
    @Bean
    public Timer notificationProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("notification_processing_duration_seconds")
            .description("Time taken to process notifications")
            .tag("service", "notification-service")
            .register(meterRegistry);
    }

    /**
     * Email delivery duration timer
     */
    @Bean
    public Timer emailDeliveryTimer(MeterRegistry meterRegistry) {
        return Timer.builder("notification_email_delivery_duration_seconds")
            .description("Time taken to deliver email notifications")
            .tag("service", "notification-service")
            .tag("type", "email")
            .register(meterRegistry);
    }

    /**
     * SMS delivery duration timer
     */
    @Bean
    public Timer smsDeliveryTimer(MeterRegistry meterRegistry) {
        return Timer.builder("notification_sms_delivery_duration_seconds")
            .description("Time taken to deliver SMS notifications")
            .tag("service", "notification-service")
            .tag("type", "sms")
            .register(meterRegistry);
    }

    /**
     * Active notification jobs gauge
     */
    @Bean
    public Gauge activeNotificationJobsGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("notification_active_jobs")
            .description("Number of currently active notification processing jobs")
            .tag("service", "notification-service")
            .register(meterRegistry, activeNotificationJobs, AtomicInteger::get);
    }

    /**
     * Rate limit violations gauge
     */
    @Bean
    public Gauge rateLimitViolationsGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("notification_rate_limit_violations_total")
            .description("Total number of rate limit violations")
            .tag("service", "notification-service")
            .register(meterRegistry, rateLimitViolations, AtomicInteger::get);
    }

    /**
     * Circuit breaker trips gauge
     */
    @Bean
    public Gauge circuitBreakerTripsGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("notification_circuit_breaker_trips_total")
            .description("Total number of circuit breaker trips")
            .tag("service", "notification-service")
            .register(meterRegistry, circuitBreakerTrips, AtomicInteger::get);
    }

    /**
     * Queue size gauge for monitoring notification queue depth
     */
    @Bean
    public Counter queueProcessedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_queue_processed_total")
            .description("Total number of notifications processed from queue")
            .tag("service", "notification-service")
            .tag("queue", "kafka")
            .register(meterRegistry);
    }

    /**
     * Template usage metrics
     */
    @Bean
    public Counter templateUsageCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_template_usage_total")
            .description("Total number of template usages by type")
            .tag("service", "notification-service")
            .register(meterRegistry);
    }

    /**
     * Business metrics - critical for trading platform
     */
    @Bean
    public Counter tradeNotificationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_trade_alerts_total")
            .description("Total number of trade-related notifications sent")
            .tag("service", "notification-service")
            .tag("category", "trading")
            .register(meterRegistry);
    }

    @Bean
    public Counter securityAlertCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_security_alerts_total")
            .description("Total number of security alerts sent")
            .tag("service", "notification-service")
            .tag("category", "security")
            .register(meterRegistry);
    }

    @Bean
    public Counter paymentNotificationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("notification_payment_alerts_total")
            .description("Total number of payment-related notifications sent")
            .tag("service", "notification-service")
            .tag("category", "payment")
            .register(meterRegistry);
    }

    // Getter methods for updating counters (Lombok can't handle AtomicInteger/AtomicLong)
    public AtomicLong getEmailNotificationsSent() {
        return emailNotificationsSent;
    }

    public AtomicLong getSmsNotificationsSent() {
        return smsNotificationsSent;
    }

    public AtomicLong getPushNotificationsSent() {
        return pushNotificationsSent;
    }

    public AtomicLong getInAppNotificationsSent() {
        return inAppNotificationsSent;
    }

    public AtomicLong getEmailNotificationsFailed() {
        return emailNotificationsFailed;
    }

    public AtomicLong getSmsNotificationsFailed() {
        return smsNotificationsFailed;
    }

    public AtomicLong getPushNotificationsFailed() {
        return pushNotificationsFailed;
    }

    public AtomicLong getInAppNotificationsFailed() {
        return inAppNotificationsFailed;
    }

    public AtomicInteger getActiveNotificationJobs() {
        return activeNotificationJobs;
    }

    public AtomicInteger getRateLimitViolations() {
        return rateLimitViolations;
    }

    public AtomicInteger getCircuitBreakerTrips() {
        return circuitBreakerTrips;
    }
}