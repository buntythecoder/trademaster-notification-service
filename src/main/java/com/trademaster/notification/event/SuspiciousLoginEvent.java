package com.trademaster.notification.event;

import java.time.LocalDateTime;

/**
 * Suspicious Login Event consumed from Security Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: security-events
 * Triggers: Suspicious login alert to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record SuspiciousLoginEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String ipAddress,
    String location,
    String device,
    String browser,
    String suspiciousReason,
    LocalDateTime attemptedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "SUSPICIOUS_LOGIN";

    /**
     * Format suspicious login details for notification
     */
    public String formatLoginDetails() {
        return String.format(
            "Suspicious login attempt detected on your account from %s (%s) using %s. " +
            "Reason: %s. If this was not you, please change your password immediately and enable 2FA.",
            location, ipAddress, device, suspiciousReason
        );
    }
}
