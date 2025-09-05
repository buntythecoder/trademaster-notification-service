package com.trademaster.notification.service;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.constant.NotificationConstants;
import com.trademaster.notification.common.Result;
import com.trademaster.notification.entity.NotificationHistory.NotificationStatus;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * SMS Notification Service
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Pattern Matching - Rule #14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notification.sms.enabled", havingValue = "true", matchIfMissing = true)
public class SmsNotificationService {
    
    private final NotificationHistoryService historyService;
    private final CircuitBreaker smsCircuitBreaker;
    private final Retry smsRetry;
    private final TimeLimiter smsTimeLimiter;
    
    private static final java.util.concurrent.ScheduledExecutorService scheduler = 
        java.util.concurrent.Executors.newScheduledThreadPool(2);
    
    @Value("${twilio.account-sid:}")
    private String accountSid;
    
    @Value("${twilio.auth-token:}")
    private String authToken;
    
    @Value("${twilio.phone-number:+1234567890}")
    private String twilioPhoneNumber;
    
    @PostConstruct
    public void init() {
        if (!accountSid.isEmpty() && !authToken.isEmpty()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS service initialized");
        } else {
            log.warn("Twilio credentials not configured - SMS service will be disabled");
        }
    }
    
    /**
     * Send SMS notification asynchronously with virtual threads and circuit breaker
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Circuit Breaker - Rule #24
     * MANDATORY: CompletableFuture - Rule #11
     */
    public CompletableFuture<NotificationResponse> sendSms(NotificationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        
        return historyService.createNotificationHistory(request, correlationId)
            .thenCompose(historyResult -> historyResult.match(
                history -> sendSmsWithResilience(request, history.getNotificationId())
                    .thenCompose(response -> updateHistoryAfterSend(response, history.getNotificationId())),
                error -> CompletableFuture.completedFuture(NotificationResponse.failure("HISTORY_ERROR", error))
            ));
    }
    
    /**
     * Send SMS with circuit breaker, retry, and timeout protection
     * 
     * MANDATORY: Resilience Patterns - Rule #24
     * MANDATORY: Virtual Threads - Rule #12
     */
    private CompletableFuture<NotificationResponse> sendSmsWithResilience(NotificationRequest request, String notificationId) {
        // Create resilient SMS sending function with enterprise-grade decorator pattern
        try {
            // Use Resilience4j Decorators with proper chaining order
            var decoratedSupplier = Decorators.ofSupplier(() -> performSmsSend(request, notificationId))
                .withCircuitBreaker(smsCircuitBreaker)
                .withRetry(smsRetry)
                .decorate();
            
            // Apply TimeLimiter with CompletionStage
            return smsTimeLimiter.executeCompletionStage(
                scheduler,
                () -> CompletableFuture.supplyAsync(decoratedSupplier, Executors.newVirtualThreadPerTaskExecutor())
            ).toCompletableFuture()
                .exceptionally(throwable -> {
                    log.error("SMS sending failed: notificationId={}, error={}", 
                             notificationId, throwable.getMessage());
                    return NotificationResponse.failure(notificationId, 
                        "SMS service unavailable: " + throwable.getMessage());
                });
                
        } catch (Exception e) {
            log.error("SMS sending setup failed: notificationId={}, error={}", 
                     notificationId, e.getMessage());
            return CompletableFuture.completedFuture(
                NotificationResponse.failure(notificationId, 
                    "SMS service unavailable: " + e.getMessage()));
        }
    }
    
    /**
     * Perform SMS sending with pattern matching
     * 
     * MANDATORY: Pattern Matching - Rule #14
     * MANDATORY: Functional Programming - Rule #3
     */
    private NotificationResponse performSmsSend(NotificationRequest request, String notificationId) {
        log.info("Sending SMS notification to: {}, ID: {}", request.phoneRecipient(), notificationId);
        
        return switch (validateSmsRequest(request)) {
            case VALID -> sendSmsMessage(request, notificationId);
            case INVALID_PHONE -> NotificationResponse.failure(notificationId, 
                "Invalid phone number format");
            case MISSING_CREDENTIALS -> NotificationResponse.failure(notificationId, 
                "SMS service not configured");
            case CONTENT_TOO_LONG -> NotificationResponse.failure(notificationId, 
                "SMS content exceeds maximum length");
        };
    }
    
    private SmsValidationResult validateSmsRequest(NotificationRequest request) {
        if (accountSid.isEmpty() || authToken.isEmpty()) {
            return SmsValidationResult.MISSING_CREDENTIALS;
        }
        if (request.phoneRecipient() == null || request.phoneRecipient().isEmpty()) {
            return SmsValidationResult.INVALID_PHONE;
        }
        if (request.content().length() > 1600) { // SMS limit
            return SmsValidationResult.CONTENT_TOO_LONG;
        }
        return SmsValidationResult.VALID;
    }
    
    private NotificationResponse sendSmsMessage(NotificationRequest request, String notificationId) {
        return Result.tryExecute(() -> {
            Message message = Message.creator(
                new PhoneNumber(request.phoneRecipient()),
                new PhoneNumber(twilioPhoneNumber),
                request.content()
            ).create();
            
            log.info("SMS sent successfully to: {}, SID: {}", 
                    request.phoneRecipient(), message.getSid());
            
            return NotificationResponse.success(notificationId, message.getSid());
        }).match(
            response -> response,
            exception -> {
                log.error("Failed to send SMS to: {}, error: {}", 
                         request.phoneRecipient(), exception.getMessage());
                return NotificationResponse.failure(notificationId, exception.getMessage());
            }
        );
    }
    
    private NotificationResponse handleSmsResult(NotificationResponse result, Throwable throwable) {
        if (throwable != null) {
            log.error("SMS notification error", throwable);
            return NotificationResponse.failure("SMS_ERROR", throwable.getMessage());
        }
        return result;
    }
    
    /**
     * Update notification history after send attempt
     */
    private CompletableFuture<NotificationResponse> updateHistoryAfterSend(
            NotificationResponse response, 
            String notificationId) {
        
        if (response.success()) {
            return historyService.updateNotificationStatus(
                    notificationId, 
                    NotificationStatus.SENT, 
                    response.deliveryId(), 
                    "system")
                .thenApply(historyResult -> response);
        } else {
            return historyService.markNotificationFailed(
                    notificationId, 
                    response.message(), 
                    "system")
                .thenApply(historyResult -> response);
        }
    }
    
    // Factory methods for common SMS types
    public static NotificationRequest createWelcomeSms(String phoneNumber, String firstName) {
        String content = String.format(
            "Welcome to TradeMaster, %s! Your account is ready. Start trading at app.trademaster.com. Reply STOP to opt out.",
            firstName
        );
        
        return NotificationRequest.sms(phoneNumber, "Welcome to TradeMaster", content);
    }
    
    public static NotificationRequest createKycApprovalSms(String phoneNumber, String firstName) {
        String content = String.format(
            "Great news %s! Your KYC verification is approved. You can now start trading on TradeMaster. Visit app.trademaster.com",
            firstName
        );
        
        return NotificationRequest.sms(phoneNumber, "KYC Approved", content);
    }
    
    public static NotificationRequest createOtpSms(String phoneNumber, String otp) {
        String content = String.format(
            "Your TradeMaster verification code is: %s. Valid for 10 minutes. Do not share this code with anyone.",
            otp
        );
        
        return NotificationRequest.sms(phoneNumber, "Verification Code", content);
    }
    
    public static NotificationRequest createTradeExecutionSms(String phoneNumber, 
                                                             String symbol, String action, 
                                                             String quantity, String price) {
        String content = String.format(
            "Trade Alert: %s %s shares of %s at ₹%s. Check your portfolio at app.trademaster.com",
            action, quantity, symbol, price
        );
        
        return NotificationRequest.sms(phoneNumber, "Trade Executed", content);
    }
    
    private enum SmsValidationResult {
        VALID,
        INVALID_PHONE,
        MISSING_CREDENTIALS,
        CONTENT_TOO_LONG
    }
}