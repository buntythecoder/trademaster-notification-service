package com.trademaster.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Placed Event consumed from Trading Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 * - Rule #14: Pattern Matching with helper methods
 *
 * Consumed from Kafka topic: trading-events
 * Triggers: Order confirmation email/SMS to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderPlacedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String orderId,
    String brokerOrderId,
    String symbol,
    String exchange,
    String side,
    String orderType,
    Integer quantity,
    BigDecimal price,
    BigDecimal stopLoss,
    BigDecimal takeProfit,
    BigDecimal orderValue,
    String brokerName,
    LocalDateTime timestamp,
    String source,
    String userEmail,
    String userName
) {
    public static final String EVENT_TYPE = "ORDER_PLACED";

    /**
     * Format order details for notification message
     *
     * MANDATORY: Rule #3 - Functional Programming
     */
    public String formatOrderDetails() {
        return String.format(
            "Order %s: %s %d shares of %s at %s on %s",
            orderId,
            side,
            quantity,
            symbol,
            formatPrice(),
            exchange
        );
    }

    /**
     * Format price based on order type
     *
     * MANDATORY: Rule #14 - Pattern Matching
     */
    private String formatPrice() {
        return switch (orderType) {
            case "MARKET" -> "market price";
            case "LIMIT" -> String.format("₹%.2f", price);
            case "STOP_LOSS" -> String.format("stop at ₹%.2f", stopLoss);
            default -> "unknown price";
        };
    }
}
