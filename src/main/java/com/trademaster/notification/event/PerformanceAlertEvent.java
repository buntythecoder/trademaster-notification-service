package com.trademaster.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Performance Alert Event consumed from Portfolio Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: portfolio-events
 * Triggers: Performance milestone notification to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record PerformanceAlertEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String alertType,
    BigDecimal totalPnL,
    BigDecimal returnPercentage,
    BigDecimal portfolioValue,
    String milestoneDescription,
    LocalDateTime triggeredAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "PERFORMANCE_ALERT";

    /**
     * Format performance alert details for notification
     */
    public String formatAlertDetails() {
        String pnlStatus = totalPnL.compareTo(BigDecimal.ZERO) > 0 ? "profit" : "loss";
        return String.format(
            "%s! Your portfolio has achieved: %s. " +
            "Total %s: ₹%.2f (%.2f%%), Portfolio Value: ₹%.2f",
            alertType, milestoneDescription, pnlStatus,
            totalPnL.abs(), returnPercentage, portfolioValue
        );
    }
}
