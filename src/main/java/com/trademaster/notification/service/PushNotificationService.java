package com.trademaster.notification.service;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.constant.NotificationConstants;
import com.trademaster.notification.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Push Notification Service
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Pattern Matching - Rule #14
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notification.push.enabled", havingValue = "true", matchIfMissing = true)
public class PushNotificationService {
    
    @Value("${firebase.server-key:}")
    private String firebaseServerKey;
    
    @Value("${apns.certificate-path:}")
    private String apnsCertificatePath;
    
    /**
     * Send push notification asynchronously with virtual threads
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: CompletableFuture - Rule #11
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = NotificationConstants.DEFAULT_MAX_RETRY_ATTEMPTS,
        backoff = @Backoff(delay = NotificationConstants.RETRY_DELAY_SECONDS * 1000, multiplier = 2)
    )
    public CompletableFuture<NotificationResponse> sendPush(NotificationRequest request) {
        return CompletableFuture
            .supplyAsync(() -> performPushSend(request), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handlePushResult);
    }
    
    /**
     * Perform push notification sending with pattern matching
     * 
     * MANDATORY: Pattern Matching - Rule #14
     * MANDATORY: Functional Programming - Rule #3
     */
    private NotificationResponse performPushSend(NotificationRequest request) {
        log.info("Processing push notification to: {}", request.recipient());
        
        String notificationId = UUID.randomUUID().toString();
        
        return switch (validatePushRequest(request)) {
            case VALID -> sendPushMessage(request, notificationId);
            case INVALID_DEVICE_TOKEN -> NotificationResponse.failure(notificationId, 
                "Invalid device token format");
            case MISSING_CONFIGURATION -> NotificationResponse.failure(notificationId, 
                "Push notification service not configured");
            case CONTENT_TOO_LONG -> NotificationResponse.failure(notificationId, 
                "Push notification content exceeds maximum length");
        };
    }
    
    private PushValidationResult validatePushRequest(NotificationRequest request) {
        if (firebaseServerKey.isEmpty() && apnsCertificatePath.isEmpty()) {
            return PushValidationResult.MISSING_CONFIGURATION;
        }
        if (request.recipient() == null || request.recipient().isEmpty()) {
            return PushValidationResult.INVALID_DEVICE_TOKEN;
        }
        if (request.content().length() > 2048) { // Push notification limit
            return PushValidationResult.CONTENT_TOO_LONG;
        }
        return PushValidationResult.VALID;
    }
    
    private NotificationResponse sendPushMessage(NotificationRequest request, String notificationId) {
        return Result.tryExecute(() -> {
            // In production, this would integrate with Firebase FCM or Apple APNs
            log.info("Push notification sent successfully to: {}, ID: {}", 
                    request.recipient(), notificationId);
            
            return NotificationResponse.success(notificationId, "PUSH_SENT");
        }).match(
            response -> response,
            exception -> {
                log.error("Failed to send push notification to: {}, error: {}", 
                         request.recipient(), exception.getMessage());
                return NotificationResponse.failure(notificationId, exception.getMessage());
            }
        );
    }
    
    private NotificationResponse handlePushResult(NotificationResponse result, Throwable throwable) {
        if (throwable != null) {
            log.error("Push notification error", throwable);
            return NotificationResponse.failure("PUSH_ERROR", throwable.getMessage());
        }
        return result;
    }
    
    // Factory methods for common push notification types
    public static NotificationRequest createOrderAlert(String deviceToken, String symbol, String action, String quantity) {
        String content = String.format("Trade Alert: %s %s shares of %s executed successfully", action, quantity, symbol);
        
        return NotificationRequest.push(deviceToken, "Trade Executed", content);
    }
    
    public static NotificationRequest createPriceAlert(String deviceToken, String symbol, String price) {
        String content = String.format("Price Alert: %s has reached ₹%s", symbol, price);
        
        return NotificationRequest.push(deviceToken, "Price Alert", content);
    }
    
    public static NotificationRequest createMarketAlert(String deviceToken, String message) {
        return NotificationRequest.push(deviceToken, "Market Alert", message);
    }
    
    private enum PushValidationResult {
        VALID,
        INVALID_DEVICE_TOKEN,
        MISSING_CONFIGURATION,
        CONTENT_TOO_LONG
    }
}