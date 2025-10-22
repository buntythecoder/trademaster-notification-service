package com.trademaster.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Deposit Completed Event consumed from Payment Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: payment-events
 * Triggers: Deposit confirmation notification to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record DepositCompletedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String transactionId,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    BigDecimal fees,
    BigDecimal netAmount,
    LocalDateTime completedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "DEPOSIT_COMPLETED";

    /**
     * Format deposit details for notification
     */
    public String formatDepositDetails() {
        return String.format(
            "Your deposit of %s %.2f has been successfully credited to your account. " +
            "Transaction ID: %s. Net Amount: %s %.2f (Fees: %s %.2f)",
            currency, amount, transactionId, currency, netAmount, currency, fees
        );
    }
}
