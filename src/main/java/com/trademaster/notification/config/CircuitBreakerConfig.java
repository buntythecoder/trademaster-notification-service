package com.trademaster.notification.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration for External Services
 * 
 * MANDATORY: Resilience Patterns - Rule #24
 * MANDATORY: External Service Protection - Rule #6
 */
@Configuration
@Slf4j
public class CircuitBreakerConfig {
    
    private static final String EMAIL_SERVICE = "emailService";
    private static final String SMS_SERVICE = "smsService";
    private static final String PUSH_SERVICE = "pushService";
    
    // Circuit Breaker Configuration Constants - Rule #17
    private static final float DEFAULT_FAILURE_RATE_THRESHOLD = 50.0f;
    private static final int DEFAULT_SLIDING_WINDOW_SIZE = 10;
    private static final int DEFAULT_MINIMUM_CALLS = 5;
    private static final int DEFAULT_WAIT_DURATION_SECONDS = 30;
    private static final int DEFAULT_HALF_OPEN_CALLS = 3;
    
    // Email Service Constants - Rule #17
    private static final float EMAIL_FAILURE_RATE_THRESHOLD = 60.0f;
    private static final int EMAIL_SLIDING_WINDOW_SIZE = 15;
    private static final int EMAIL_MINIMUM_CALLS = 8;
    private static final int EMAIL_WAIT_DURATION_MINUTES = 1;
    private static final int EMAIL_HALF_OPEN_CALLS = 5;
    
    // SMS Service Constants - Rule #17
    private static final float SMS_FAILURE_RATE_THRESHOLD = 40.0f;
    private static final int SMS_SLIDING_WINDOW_SIZE = 8;
    private static final int SMS_MINIMUM_CALLS = 4;
    private static final int SMS_WAIT_DURATION_MINUTES = 2;
    private static final int SMS_HALF_OPEN_CALLS = 2;
    
    // Push Service Constants - Rule #17
    private static final float PUSH_FAILURE_RATE_THRESHOLD = 50.0f;
    private static final int PUSH_SLIDING_WINDOW_SIZE = 12;
    private static final int PUSH_MINIMUM_CALLS = 6;
    private static final int PUSH_WAIT_DURATION_SECONDS = 45;
    private static final int PUSH_HALF_OPEN_CALLS = 3;
    
    // Retry Configuration Constants - Rule #17
    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    private static final int DEFAULT_RETRY_WAIT_MILLIS = 500;
    private static final int EMAIL_MAX_RETRY_ATTEMPTS = 4;
    private static final int EMAIL_RETRY_WAIT_SECONDS = 1;
    private static final int SMS_MAX_RETRY_ATTEMPTS = 2;
    private static final int SMS_RETRY_WAIT_MILLIS = 800;
    private static final int PUSH_MAX_RETRY_ATTEMPTS = 3;
    private static final int PUSH_RETRY_WAIT_MILLIS = 600;
    
    // Timeout Configuration Constants - Rule #17
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int EMAIL_TIMEOUT_SECONDS = 15;
    private static final int SMS_TIMEOUT_SECONDS = 8;
    private static final int PUSH_TIMEOUT_SECONDS = 5;
    
    /**
     * Circuit Breaker Registry with custom configurations
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
            // Default configuration
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(DEFAULT_FAILURE_RATE_THRESHOLD)
                .slidingWindowSize(DEFAULT_SLIDING_WINDOW_SIZE)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(DEFAULT_MINIMUM_CALLS)
                .waitDurationInOpenState(Duration.ofSeconds(DEFAULT_WAIT_DURATION_SECONDS))
                .permittedNumberOfCallsInHalfOpenState(DEFAULT_HALF_OPEN_CALLS)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build()
        );
        
        // Email Service Circuit Breaker - More lenient due to email criticality
        registry.circuitBreaker(EMAIL_SERVICE, 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(EMAIL_FAILURE_RATE_THRESHOLD)
                .slidingWindowSize(EMAIL_SLIDING_WINDOW_SIZE)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(EMAIL_MINIMUM_CALLS)
                .waitDurationInOpenState(Duration.ofMinutes(EMAIL_WAIT_DURATION_MINUTES))
                .permittedNumberOfCallsInHalfOpenState(EMAIL_HALF_OPEN_CALLS)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build()
        );
        
        // SMS Service Circuit Breaker - Stricter due to cost implications
        registry.circuitBreaker(SMS_SERVICE,
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(SMS_FAILURE_RATE_THRESHOLD)
                .slidingWindowSize(SMS_SLIDING_WINDOW_SIZE)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(SMS_MINIMUM_CALLS)
                .waitDurationInOpenState(Duration.ofMinutes(SMS_WAIT_DURATION_MINUTES))
                .permittedNumberOfCallsInHalfOpenState(SMS_HALF_OPEN_CALLS)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build()
        );
        
        // Push Service Circuit Breaker - Balanced configuration
        registry.circuitBreaker(PUSH_SERVICE,
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(PUSH_FAILURE_RATE_THRESHOLD)
                .slidingWindowSize(PUSH_SLIDING_WINDOW_SIZE)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(PUSH_MINIMUM_CALLS)
                .waitDurationInOpenState(Duration.ofSeconds(PUSH_WAIT_DURATION_SECONDS))
                .permittedNumberOfCallsInHalfOpenState(PUSH_HALF_OPEN_CALLS)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build()
        );
        
        // Add event listeners for monitoring
        registry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("Circuit breaker {} state transition: {} -> {}", 
                            event.getCircuitBreakerName(), 
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()))
                .onSuccess(event -> 
                    log.debug("Circuit breaker {} success: duration={}ms", 
                            event.getCircuitBreakerName(), 
                            event.getElapsedDuration().toMillis()))
                .onError(event -> 
                    log.warn("Circuit breaker {} error: duration={}ms, error={}", 
                            event.getCircuitBreakerName(), 
                            event.getElapsedDuration().toMillis(),
                            event.getThrowable().getMessage()));
        });
        
        return registry;
    }
    
    /**
     * Retry Registry with custom configurations
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.of(
            // Default retry configuration
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(DEFAULT_MAX_RETRY_ATTEMPTS)
                .waitDuration(Duration.ofMillis(DEFAULT_RETRY_WAIT_MILLIS))
                .retryExceptions(Exception.class)
                .build()
        );
        
        // Email Service Retry - More attempts due to importance
        registry.retry(EMAIL_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(EMAIL_MAX_RETRY_ATTEMPTS)
                .waitDuration(Duration.ofSeconds(EMAIL_RETRY_WAIT_SECONDS))
                .retryExceptions(Exception.class)
                .build()
        );
        
        // SMS Service Retry - Fewer attempts due to cost
        registry.retry(SMS_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(SMS_MAX_RETRY_ATTEMPTS)
                .waitDuration(Duration.ofMillis(SMS_RETRY_WAIT_MILLIS))
                .retryExceptions(Exception.class)
                .build()
        );
        
        // Push Service Retry - Standard configuration
        registry.retry(PUSH_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(PUSH_MAX_RETRY_ATTEMPTS)
                .waitDuration(Duration.ofMillis(PUSH_RETRY_WAIT_MILLIS))
                .retryExceptions(Exception.class)
                .build()
        );
        
        return registry;
    }
    
    /**
     * Time Limiter Registry for timeout management
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(
            // Default timeout configuration
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .cancelRunningFuture(true)
                .build()
        );
        
        // Email Service Timeout - Longer due to potential SMTP delays
        registry.timeLimiter(EMAIL_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(EMAIL_TIMEOUT_SECONDS))
                .cancelRunningFuture(true)
                .build()
        );
        
        // SMS Service Timeout - Medium timeout
        registry.timeLimiter(SMS_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(SMS_TIMEOUT_SECONDS))
                .cancelRunningFuture(true)
                .build()
        );
        
        // Push Service Timeout - Shorter timeout for better UX
        registry.timeLimiter(PUSH_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(PUSH_TIMEOUT_SECONDS))
                .cancelRunningFuture(true)
                .build()
        );
        
        return registry;
    }
    
    /**
     * Get Email Service Circuit Breaker
     */
    @Bean
    public CircuitBreaker emailCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(EMAIL_SERVICE);
    }
    
    /**
     * Get SMS Service Circuit Breaker
     */
    @Bean
    public CircuitBreaker smsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(SMS_SERVICE);
    }
    
    /**
     * Get Push Service Circuit Breaker
     */
    @Bean
    public CircuitBreaker pushCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(PUSH_SERVICE);
    }
    
    /**
     * Get Email Service Retry
     */
    @Bean
    public Retry emailRetry(RetryRegistry registry) {
        return registry.retry(EMAIL_SERVICE);
    }
    
    /**
     * Get SMS Service Retry
     */
    @Bean
    public Retry smsRetry(RetryRegistry registry) {
        return registry.retry(SMS_SERVICE);
    }
    
    /**
     * Get Push Service Retry
     */
    @Bean
    public Retry pushRetry(RetryRegistry registry) {
        return registry.retry(PUSH_SERVICE);
    }
    
    /**
     * Get Email Service Time Limiter
     */
    @Bean
    public TimeLimiter emailTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(EMAIL_SERVICE);
    }
    
    /**
     * Get SMS Service Time Limiter
     */
    @Bean
    public TimeLimiter smsTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(SMS_SERVICE);
    }
    
    /**
     * Get Push Service Time Limiter
     */
    @Bean
    public TimeLimiter pushTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(PUSH_SERVICE);
    }
}