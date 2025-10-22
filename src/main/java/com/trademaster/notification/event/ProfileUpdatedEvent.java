package com.trademaster.notification.event;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Profile Updated Event consumed from User Profile Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: user-profile-events
 * Triggers: Profile update confirmation to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record ProfileUpdatedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    Map<String, String> updatedFields,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "PROFILE_UPDATED";

    /**
     * Format updated fields for notification
     */
    public String formatUpdatedFields() {
        return String.format(
            "Your profile has been updated. Changed fields: %s",
            String.join(", ", updatedFields.keySet())
        );
    }
}
