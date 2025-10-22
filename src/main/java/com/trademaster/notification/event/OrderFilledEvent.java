package com.trademaster.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Filled Event consumed from Trading Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 * - Rule #14: Pattern Matching with helper methods
 *
 * Consumed from Kafka topic: trading-events
 * Triggers: Order execution notification to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderFilledEvent(
    String correlationId,
    String eventType,
    Long userId,
    String orderId,
    String brokerOrderId,
    String symbol,
    String exchange,
    String side,
    String orderType,
    Integer filledQuantity,
    BigDecimal avgExecutionPrice,
    BigDecimal executionValue,
    BigDecimal fees,
    BigDecimal netValue,
    String brokerName,
    LocalDateTime executedAt,
    LocalDateTime timestamp,
    String userEmail,
    String userName
) {
    public static final String EVENT_TYPE = "ORDER_FILLED";

    /**
     * Format execution details for notification
     *
     * MANDATORY: Rule #3 - Functional Programming
     */
    public String formatExecutionDetails() {
        return String.format(
            "Your %s order for %d shares of %s executed at ₹%.2f. " +
            "Total value: ₹%.2f, Fees: ₹%.2f, Net: ₹%.2f",
            side,
            filledQuantity,
            symbol,
            avgExecutionPrice,
            executionValue,
            fees,
            netValue
        );
    }

    /**
     * Check if this is a profitable execution
     *
     * MANDATORY: Rule #3 - Functional Programming
     */
    public boolean isProfitable(BigDecimal costBasis) {
        return "SELL".equals(side) &&
               netValue.compareTo(costBasis) > 0;
    }
}
