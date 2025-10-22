package com.trademaster.notification.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC Submitted Event consumed from User Profile Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: user-profile-events
 * Triggers: KYC submission acknowledgment to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record KycSubmittedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String kycReferenceId,
    List<String> documentsSubmitted,
    LocalDateTime submittedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "KYC_SUBMITTED";

    /**
     * Format submission details for notification
     */
    public String formatSubmissionDetails() {
        return String.format(
            "Your KYC verification documents (Reference: %s) have been submitted successfully. " +
            "Documents: %s. We will review them within 2-3 business days.",
            kycReferenceId,
            String.join(", ", documentsSubmitted)
        );
    }
}
