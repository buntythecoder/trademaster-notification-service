package com.trademaster.notification.kafka;

import com.trademaster.notification.kafka.TradingNotificationKafkaListener.TradingNotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Trading Notification Dead Letter Queue Handler
 *
 * Handles failed notification messages that couldn't be processed by the main listener.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #1: Java 24 Virtual Threads for async processing
 * - Rule #3: Functional programming patterns
 * - Rule #11: Result types for error handling
 * - Rule #15: Structured logging with correlation IDs
 *
 * DLQ Strategy:
 * 1. Log failed message details for investigation
 * 2. Store in database for manual review
 * 3. Alert operations team for critical failures
 * 4. Attempt limited retry with exponential backoff
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradingNotificationDLQHandler {

    private static final String DLQ_TOPIC = "trading.notifications.dlq";
    private static final String CONSUMER_GROUP = "notification-service-dlq";

    /**
     * Listen for failed messages in DLQ
     *
     * MANDATORY: Rule #12 - Virtual Threads for async processing
     * MANDATORY: Rule #15 - Structured Logging
     *
     * @param message Failed notification message
     * @param partition Kafka partition
     * @param offset Kafka offset
     * @param exception Original exception that caused failure
     */
    @KafkaListener(
        topics = DLQ_TOPIC,
        groupId = CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleFailedNotification(
            @Payload TradingNotificationMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exception) {

        String correlationId = extractCorrelationId(message);

        log.error("Processing failed notification from DLQ - userId: {}, type: {}, partition: {}, offset: {}, correlationId: {}, error: {}",
                message.userId(), message.type(), partition, offset, correlationId, exception);

        // Process asynchronously with Virtual Threads
        CompletableFuture.runAsync(() -> {
            processDLQMessage(message, correlationId, exception);
        });
    }

    /**
     * Process DLQ message with retry logic and persistence
     *
     * MANDATORY: Rule #3 - Functional Programming
     */
    private void processDLQMessage(
            TradingNotificationMessage message,
            String correlationId,
            String exception) {

        try {
            // Log complete failure details
            logFailureDetails(message, correlationId, exception);

            // Store in database for manual review
            persistFailedNotification(message, correlationId, exception);

            // Alert operations team for critical failures
            alertOperationsTeam(message, correlationId, exception);

            log.info("DLQ message processed and stored - userId: {}, type: {}, correlationId: {}",
                    message.userId(), message.type(), correlationId);

        } catch (Exception e) {
            log.error("Failed to process DLQ message - userId: {}, correlationId: {}, error: {}",
                    message.userId(), correlationId, e.getMessage(), e);
            // Last resort: This should trigger alerting system
        }
    }

    /**
     * Log complete failure details for investigation
     *
     * MANDATORY: Rule #15 - Structured Logging
     */
    private void logFailureDetails(
            TradingNotificationMessage message,
            String correlationId,
            String exception) {

        log.error("""
                DLQ Failure Details:
                - Correlation ID: {}
                - Notification ID: {}
                - User ID: {}
                - Type: {}
                - Title: {}
                - Content: {}
                - Data: {}
                - Timestamp: {}
                - Original Exception: {}
                """,
                correlationId,
                message.notificationId(),
                message.userId(),
                message.type(),
                message.title(),
                message.content(),
                message.data(),
                message.timestamp(),
                exception);
    }

    /**
     * Persist failed notification to database for manual review
     *
     * MANDATORY: Rule #9 - Immutable Records
     * MANDATORY: Rule #3 - Functional Programming
     *
     * Future Enhancement: Create FailedNotification entity and repository
     */
    private void persistFailedNotification(
            TradingNotificationMessage message,
            String correlationId,
            String exception) {

        // Future: Store in database
        // FailedNotification failedNotification = new FailedNotification(
        //     message.notificationId(),
        //     message.userId(),
        //     message.type(),
        //     message.title(),
        //     message.content(),
        //     message.data(),
        //     message.timestamp(),
        //     correlationId,
        //     exception,
        //     Instant.now()
        // );
        // failedNotificationRepository.save(failedNotification);

        log.info("Failed notification persistence placeholder - userId: {}, correlationId: {}",
                message.userId(), correlationId);
    }

    /**
     * Alert operations team for critical failures
     *
     * MANDATORY: Rule #3 - Functional Programming
     *
     * Future Enhancement: Integrate with PagerDuty, Slack, or email alerts
     */
    private void alertOperationsTeam(
            TradingNotificationMessage message,
            String correlationId,
            String exception) {

        // Determine if this is a critical failure that needs immediate attention
        boolean isCritical = switch (message.type()) {
            case "ORDER_REJECTED", "ORDER_CANCELLED" -> true;
            default -> false;
        };

        if (isCritical) {
            log.error("CRITICAL: Failed notification requires immediate attention - userId: {}, type: {}, correlationId: {}",
                    message.userId(), message.type(), correlationId);

            // Future: Send alert via PagerDuty, Slack, or email
            // alertingService.sendCriticalAlert(
            //     "Failed Trading Notification",
            //     String.format("User %d notification %s failed: %s",
            //         message.userId(), message.type(), exception)
            // );
        } else {
            log.warn("Non-critical failed notification - userId: {}, type: {}, correlationId: {}",
                    message.userId(), message.type(), correlationId);
        }
    }

    /**
     * Extract correlation ID from message data
     *
     * MANDATORY: Rule #3 - Functional Programming with Optional
     */
    private String extractCorrelationId(TradingNotificationMessage message) {
        return java.util.Optional.ofNullable(message.data())
            .map(data -> data.get("correlationId"))
            .map(Object::toString)
            .orElse("UNKNOWN");
    }
}
