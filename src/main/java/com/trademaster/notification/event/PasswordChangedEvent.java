package com.trademaster.notification.event;

import java.time.LocalDateTime;

/**
 * Password Changed Event consumed from Security Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: security-events
 * Triggers: Password change confirmation to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record PasswordChangedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String ipAddress,
    String location,
    String device,
    LocalDateTime changedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "PASSWORD_CHANGED";

    /**
     * Format password change details for notification
     */
    public String formatChangeDetails() {
        return String.format(
            "Your password was successfully changed from %s (%s) using %s at %s. " +
            "If you did not make this change, please contact support immediately.",
            location, ipAddress, device, changedAt
        );
    }
}
