package com.trademaster.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Position Closed Event consumed from Portfolio Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: portfolio-events
 * Triggers: Position closure notification to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record PositionClosedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String symbol,
    String exchange,
    Integer quantity,
    BigDecimal avgBuyPrice,
    BigDecimal avgSellPrice,
    BigDecimal realizedPnL,
    BigDecimal returnPercentage,
    LocalDateTime closedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "POSITION_CLOSED";

    /**
     * Format position closure details for notification
     */
    public String formatClosureDetails() {
        String pnlStatus = realizedPnL.compareTo(BigDecimal.ZERO) > 0 ? "profit" : "loss";
        return String.format(
            "Your position in %s (%d shares) has been fully closed. " +
            "Avg Buy: ₹%.2f, Avg Sell: ₹%.2f, Realized %s: ₹%.2f (%.2f%%)",
            symbol, quantity, avgBuyPrice, avgSellPrice, pnlStatus,
            realizedPnL.abs(), returnPercentage
        );
    }
}
