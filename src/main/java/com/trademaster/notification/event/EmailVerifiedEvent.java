package com.trademaster.notification.event;

import java.time.LocalDateTime;

/**
 * Email Verified Event consumed from User Profile Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: user-profile-events
 * Triggers: Email verification confirmation to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record EmailVerifiedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    LocalDateTime verifiedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "EMAIL_VERIFIED";

    /**
     * Format verification details for notification
     */
    public String formatVerificationDetails() {
        return String.format(
            "Your email address %s has been successfully verified at %s",
            userEmail,
            verifiedAt
        );
    }
}
