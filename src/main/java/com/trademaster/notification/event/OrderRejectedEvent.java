package com.trademaster.notification.event;

import java.time.LocalDateTime;

/**
 * Order Rejected Event consumed from Trading Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: trading-events
 * Triggers: Order rejection alert to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderRejectedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String orderId,
    String symbol,
    String exchange,
    String side,
    Integer quantity,
    String rejectionReason,
    LocalDateTime rejectedAt,
    LocalDateTime timestamp,
    String userEmail,
    String userName
) {
    public static final String EVENT_TYPE = "ORDER_REJECTED";

    /**
     * Format rejection details for notification
     */
    public String formatRejectionDetails() {
        return String.format(
            "Your %s order for %d shares of %s was rejected. Reason: %s",
            side,
            quantity,
            symbol,
            rejectionReason
        );
    }
}
