package com.trademaster.notification.event;

import java.time.LocalDateTime;

/**
 * Two-Factor Authentication Enabled Event consumed from Security Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: security-events
 * Triggers: 2FA activation confirmation to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record TwoFaEnabledEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String twoFaMethod,
    String maskedContact,
    LocalDateTime enabledAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "TWO_FA_ENABLED";

    /**
     * Format 2FA enablement details for notification
     */
    public String formatEnablementDetails() {
        return String.format(
            "Two-Factor Authentication (%s) has been successfully enabled for your account. " +
            "Verification codes will be sent to %s. Your account is now more secure!",
            twoFaMethod, maskedContact
        );
    }
}
