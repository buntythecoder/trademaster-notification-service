package com.trademaster.notification.service;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.constant.NotificationConstants;
import com.trademaster.notification.common.Result;
import com.trademaster.notification.entity.NotificationHistory.NotificationStatus;
import com.trademaster.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

/**
 * In-App Notification Service
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Pattern Matching - Rule #14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notification.inapp.enabled", havingValue = "true", matchIfMissing = true)
public class InAppNotificationService {
    
    // In-app notification limits - Rule #17
    private static final int MAX_IN_APP_CONTENT_LENGTH = 5000;
    
    private final NotificationHistoryService historyService;
    private final NotificationWebSocketHandler webSocketHandler;
    
    // In-memory store for real-time notifications (would use Redis in production)
    private final ConcurrentMap<String, ConcurrentMap<String, NotificationResponse>> userNotifications = new ConcurrentHashMap<>();
    
    /**
     * Send in-app notification asynchronously with virtual threads
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: CompletableFuture - Rule #11
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = NotificationConstants.DEFAULT_MAX_RETRY_ATTEMPTS,
        backoff = @Backoff(delay = NotificationConstants.RETRY_DELAY_SECONDS * 1000, multiplier = 2)
    )
    public CompletableFuture<NotificationResponse> sendInApp(NotificationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        
        return historyService.createNotificationHistory(request, correlationId)
            .thenCompose(historyResult -> historyResult.match(
                history -> CompletableFuture
                    .supplyAsync(() -> performInAppSend(request, history.getNotificationId()), 
                                Executors.newVirtualThreadPerTaskExecutor())
                    .thenCompose(response -> updateHistoryAfterSend(response, history.getNotificationId())),
                error -> CompletableFuture.completedFuture(NotificationResponse.failure("HISTORY_ERROR", error))
            ));
    }
    
    /**
     * Perform in-app notification sending with pattern matching
     * 
     * MANDATORY: Pattern Matching - Rule #14
     * MANDATORY: Functional Programming - Rule #3
     */
    private NotificationResponse performInAppSend(NotificationRequest request, String notificationId) {
        log.info("Processing in-app notification for user: {}, ID: {}", request.recipient(), notificationId);
        
        return switch (validateInAppRequest(request)) {
            case VALID -> sendInAppMessage(request, notificationId);
            case INVALID_USER -> NotificationResponse.failure(notificationId, 
                "Invalid user ID format");
            case CONTENT_TOO_LONG -> NotificationResponse.failure(notificationId, 
                "In-app notification content exceeds maximum length");
        };
    }
    
    private InAppValidationResult validateInAppRequest(NotificationRequest request) {
        return (request.recipient() == null || request.recipient().isEmpty()) ? InAppValidationResult.INVALID_USER :
               (request.content().length() > MAX_IN_APP_CONTENT_LENGTH) ? InAppValidationResult.CONTENT_TOO_LONG :
               InAppValidationResult.VALID;
    }
    
    private NotificationResponse sendInAppMessage(NotificationRequest request, String notificationId) {
        return Result.tryExecute(() -> {
            NotificationResponse notification = NotificationResponse.success(notificationId, "IN_APP_DELIVERED");
            
            // Store notification for user
            userNotifications
                .computeIfAbsent(request.recipient(), k -> new ConcurrentHashMap<>())
                .put(notificationId, notification);
            
            log.info("In-app notification stored for user: {}, ID: {}", 
                    request.recipient(), notificationId);
            
            // Send real-time notification via WebSocket
            webSocketHandler.sendNotificationToUser(request.recipient(), notification)
                .thenAccept(sent -> {
                    if (sent) {
                        log.debug("Real-time notification sent via WebSocket to user: {}", request.recipient());
                    } else {
                        log.debug("WebSocket not available for user: {}, notification stored for later", request.recipient());
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Failed to send WebSocket notification to user: {}", request.recipient(), throwable);
                    return null;
                });
            
            return notification;
        }).match(
            response -> response,
            exception -> {
                log.error("Failed to send in-app notification to user: {}, error: {}", 
                         request.recipient(), exception.getMessage());
                return NotificationResponse.failure(notificationId, exception.getMessage());
            }
        );
    }
    
    /**
     * Check if user has active WebSocket connection
     */
    public boolean isUserConnectedRealTime(String userId) {
        return webSocketHandler.isUserConnected(userId);
    }
    
    /**
     * Get WebSocket connection statistics
     */
    public Map<String, Integer> getConnectionStatistics() {
        return Map.of(
            "activeUserSessions", webSocketHandler.getActiveUserSessionCount(),
            "activeAdminSessions", webSocketHandler.getActiveAdminSessionCount()
        );
    }
    
    /**
     * Get unread notifications for user
     */
    public ConcurrentMap<String, NotificationResponse> getUnreadNotifications(String userId) {
        return userNotifications.getOrDefault(userId, new ConcurrentHashMap<>());
    }
    
    /**
     * Mark notification as read
     */
    public boolean markAsRead(String userId, String notificationId) {
        return userNotifications
            .getOrDefault(userId, new ConcurrentHashMap<>())
            .remove(notificationId) != null;
    }
    
    /**
     * Get notification count for user
     */
    public int getUnreadCount(String userId) {
        return userNotifications
            .getOrDefault(userId, new ConcurrentHashMap<>())
            .size();
    }
    
    private NotificationResponse handleInAppResult(NotificationResponse result, Throwable throwable) {
        if (throwable != null) {
            log.error("In-app notification error", throwable);
            return NotificationResponse.failure("IN_APP_ERROR", throwable.getMessage());
        }
        return result;
    }
    
    // Factory methods for common in-app notification types
    public static NotificationRequest createTradeNotification(String userId, String symbol, String action, String quantity, String price) {
        String content = String.format("Your %s order for %s shares of %s has been executed at ₹%s", 
                                      action, quantity, symbol, price);
        
        return NotificationRequest.inApp(userId, "Trade Executed", content);
    }
    
    public static NotificationRequest createMarketUpdate(String userId, String market, String change) {
        String content = String.format("Market Update: %s is %s", market, change);
        
        return NotificationRequest.inApp(userId, "Market Update", content);
    }
    
    public static NotificationRequest createAccountAlert(String userId, String message) {
        return NotificationRequest.inApp(userId, "Account Alert", message);
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
    
    private enum InAppValidationResult {
        VALID,
        INVALID_USER,
        CONTENT_TOO_LONG
    }
}