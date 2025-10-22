package com.trademaster.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Failed Event consumed from Payment Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: payment-events
 * Triggers: Payment failure alert to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record PaymentFailedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String transactionId,
    String transactionType,
    BigDecimal amount,
    String currency,
    String failureReason,
    String errorCode,
    LocalDateTime failedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "PAYMENT_FAILED";

    /**
     * Format failure details for notification
     */
    public String formatFailureDetails() {
        return String.format(
            "Your %s transaction of %s %.2f (Transaction ID: %s) has failed. " +
            "Reason: %s. Please check your account details and try again or contact support.",
            transactionType, currency, amount, transactionId, failureReason
        );
    }
}
