package com.trademaster.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Withdrawal Completed Event consumed from Payment Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: payment-events
 * Triggers: Withdrawal confirmation notification to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record WithdrawalCompletedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String transactionId,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String destinationAccount,
    BigDecimal fees,
    BigDecimal netAmount,
    LocalDateTime completedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "WITHDRAWAL_COMPLETED";

    /**
     * Format withdrawal details for notification
     */
    public String formatWithdrawalDetails() {
        return String.format(
            "Your withdrawal of %s %.2f has been successfully processed. " +
            "Transaction ID: %s. Net Amount: %s %.2f (Fees: %s %.2f). " +
            "Funds will be credited to %s within 1-2 business days.",
            currency, amount, transactionId, currency, netAmount, currency, fees, destinationAccount
        );
    }
}
