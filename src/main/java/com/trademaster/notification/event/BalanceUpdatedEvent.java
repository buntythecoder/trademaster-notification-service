package com.trademaster.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Balance Updated Event consumed from Portfolio Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: portfolio-events
 * Triggers: Balance update notification to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record BalanceUpdatedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    BigDecimal previousBalance,
    BigDecimal currentBalance,
    BigDecimal changeAmount,
    String changeReason,
    LocalDateTime updatedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "BALANCE_UPDATED";

    /**
     * Format balance update details for notification
     */
    public String formatBalanceDetails() {
        String direction = changeAmount.compareTo(BigDecimal.ZERO) > 0 ? "increased" : "decreased";
        return String.format(
            "Your account balance has %s by ₹%.2f. " +
            "Previous: ₹%.2f, Current: ₹%.2f. Reason: %s",
            direction, changeAmount.abs(), previousBalance, currentBalance, changeReason
        );
    }
}
