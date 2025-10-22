package com.trademaster.notification.event;

import java.time.LocalDateTime;

/**
 * Order Cancelled Event consumed from Trading Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: trading-events
 * Triggers: Order cancellation notification to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderCancelledEvent(
    String correlationId,
    String eventType,
    Long userId,
    String orderId,
    String brokerOrderId,
    String symbol,
    String exchange,
    String side,
    Integer quantity,
    String cancelReason,
    LocalDateTime cancelledAt,
    LocalDateTime timestamp,
    String userEmail,
    String userName
) {
    public static final String EVENT_TYPE = "ORDER_CANCELLED";

    /**
     * Format cancellation details for notification
     */
    public String formatCancellationDetails() {
        return String.format(
            "Your %s order for %d shares of %s has been cancelled. Reason: %s",
            side,
            quantity,
            symbol,
            cancelReason != null ? cancelReason : "User requested"
        );
    }
}
