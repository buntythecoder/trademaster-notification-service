package com.trademaster.notification.events;

import com.trademaster.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Event Publisher for Notification Service
 * 
 * MANDATORY: Event-Driven Architecture - Rule #4
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * 
 * Publishes notification events to other TradeMaster services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish notification sent event
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Event-Driven Pattern - Rule #4
     */
    public CompletableFuture<Void> publishNotificationSent(NotificationResponse response) {
        Map<String, Object> event = Map.of(
            "eventType", "NOTIFICATION_SENT",
            "notificationId", response.notificationId(),
            "notificationType", response.type(),
            "recipient", response.recipient(),
            "status", response.status(),
            "timestamp", LocalDateTime.now().toString(),
            "success", response.success(),
            "service", "notification-service"
        );
        
        return kafkaTemplate.send("notification-status-events", response.notificationId(), event)
            .thenRun(() -> log.info("Published notification sent event: {}", response.notificationId()))
            .exceptionally(throwable -> {
                log.error("Failed to publish notification sent event: {}", response.notificationId(), throwable);
                return null;
            });
    }

    /**
     * Publish notification failed event
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<Void> publishNotificationFailed(
            String notificationId, 
            String notificationType, 
            String recipient, 
            String errorMessage) {
        
        Map<String, Object> event = Map.of(
            "eventType", "NOTIFICATION_FAILED",
            "notificationId", notificationId,
            "notificationType", notificationType,
            "recipient", recipient,
            "errorMessage", errorMessage,
            "timestamp", LocalDateTime.now().toString(),
            "success", false,
            "service", "notification-service"
        );
        
        return kafkaTemplate.send("notification-status-events", notificationId, event)
            .thenRun(() -> log.info("Published notification failed event: {}", notificationId))
            .exceptionally(throwable -> {
                log.error("Failed to publish notification failed event: {}", notificationId, throwable);
                return null;
            });
    }

    /**
     * Publish rate limit exceeded event
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Zero Trust Security - Rule #6
     */
    public CompletableFuture<Void> publishRateLimitExceeded(
            String userId, 
            String notificationType, 
            int attemptedCount) {
        
        Map<String, Object> event = Map.of(
            "eventType", "RATE_LIMIT_EXCEEDED",
            "userId", userId,
            "notificationType", notificationType,
            "attemptedCount", attemptedCount,
            "timestamp", LocalDateTime.now().toString(),
            "service", "notification-service",
            "severity", "MEDIUM"
        );
        
        return kafkaTemplate.send("security-alerts", userId, event)
            .thenRun(() -> log.info("Published rate limit exceeded event for user: {}", userId))
            .exceptionally(throwable -> {
                log.error("Failed to publish rate limit exceeded event for user: {}", userId, throwable);
                return null;
            });
    }

    /**
     * Publish bulk notification completed event
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<Void> publishBulkNotificationCompleted(
            String bulkId, 
            int totalCount, 
            int successCount, 
            int failedCount) {
        
        Map<String, Object> event = Map.of(
            "eventType", "BULK_NOTIFICATION_COMPLETED",
            "bulkId", bulkId,
            "totalCount", totalCount,
            "successCount", successCount,
            "failedCount", failedCount,
            "timestamp", LocalDateTime.now().toString(),
            "service", "notification-service",
            "successRate", (double) successCount / totalCount
        );
        
        return kafkaTemplate.send("notification-status-events", bulkId, event)
            .thenRun(() -> log.info("Published bulk notification completed event: {}", bulkId))
            .exceptionally(throwable -> {
                log.error("Failed to publish bulk notification completed event: {}", bulkId, throwable);
                return null;
            });
    }

    /**
     * Publish service health event
     * 
     * MANDATORY: Monitoring - Infrastructure Requirements
     */
    public CompletableFuture<Void> publishServiceHealth(
            String healthStatus, 
            Map<String, Object> healthMetrics) {
        
        Map<String, Object> event = Map.of(
            "eventType", "SERVICE_HEALTH",
            "service", "notification-service",
            "status", healthStatus,
            "metrics", healthMetrics,
            "timestamp", LocalDateTime.now().toString(),
            "version", "2.0.0"
        );
        
        return kafkaTemplate.send("service-health-events", "notification-service", event)
            .thenRun(() -> log.debug("Published service health event: {}", healthStatus))
            .exceptionally(throwable -> {
                log.error("Failed to publish service health event", throwable);
                return null;
            });
    }

    /**
     * Publish email delivery status event
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<Void> publishEmailDeliveryStatus(
            String notificationId, 
            String emailAddress, 
            String deliveryStatus, 
            String bounceReason) {
        
        Map<String, Object> event = Map.of(
            "eventType", "EMAIL_DELIVERY_STATUS",
            "notificationId", notificationId,
            "emailAddress", emailAddress,
            "deliveryStatus", deliveryStatus,
            "bounceReason", bounceReason != null ? bounceReason : "",
            "timestamp", LocalDateTime.now().toString(),
            "service", "notification-service"
        );
        
        return kafkaTemplate.send("email-delivery-events", notificationId, event)
            .thenRun(() -> log.info("Published email delivery status: {} for {}", deliveryStatus, notificationId))
            .exceptionally(throwable -> {
                log.error("Failed to publish email delivery status for: {}", notificationId, throwable);
                return null;
            });
    }

    /**
     * Publish SMS delivery status event
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<Void> publishSmsDeliveryStatus(
            String notificationId, 
            String phoneNumber, 
            String deliveryStatus, 
            String errorCode) {
        
        Map<String, Object> event = Map.of(
            "eventType", "SMS_DELIVERY_STATUS",
            "notificationId", notificationId,
            "phoneNumber", phoneNumber,
            "deliveryStatus", deliveryStatus,
            "errorCode", errorCode != null ? errorCode : "",
            "timestamp", LocalDateTime.now().toString(),
            "service", "notification-service"
        );
        
        return kafkaTemplate.send("sms-delivery-events", notificationId, event)
            .thenRun(() -> log.info("Published SMS delivery status: {} for {}", deliveryStatus, notificationId))
            .exceptionally(throwable -> {
                log.error("Failed to publish SMS delivery status for: {}", notificationId, throwable);
                return null;
            });
    }

    /**
     * Publish notification template usage analytics
     * 
     * MANDATORY: Analytics Requirements
     */
    public CompletableFuture<Void> publishTemplateUsageAnalytics(
            String templateName, 
            String notificationType, 
            boolean successful) {
        
        Map<String, Object> event = Map.of(
            "eventType", "TEMPLATE_USAGE_ANALYTICS",
            "templateName", templateName,
            "notificationType", notificationType,
            "successful", successful,
            "timestamp", LocalDateTime.now().toString(),
            "service", "notification-service"
        );
        
        return kafkaTemplate.send("analytics-events", templateName, event)
            .thenRun(() -> log.debug("Published template usage analytics for: {}", templateName))
            .exceptionally(throwable -> {
                log.error("Failed to publish template usage analytics for: {}", templateName, throwable);
                return null;
            });
    }
}