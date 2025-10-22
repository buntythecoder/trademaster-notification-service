package com.trademaster.notification.event;

import java.time.LocalDateTime;

/**
 * KYC Verified Event consumed from User Profile Service
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records Usage
 * - Rule #3: Functional Programming (immutable data)
 *
 * Consumed from Kafka topic: user-profile-events
 * Triggers: KYC verification success notification to user
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record KycVerifiedEvent(
    String correlationId,
    String eventType,
    Long userId,
    String userEmail,
    String userName,
    String kycReferenceId,
    String verificationLevel,
    LocalDateTime verifiedAt,
    LocalDateTime timestamp
) {
    public static final String EVENT_TYPE = "KYC_VERIFIED";

    /**
     * Format verification success details for notification
     */
    public String formatVerificationDetails() {
        return String.format(
            "Congratulations! Your KYC verification (Reference: %s) has been approved. " +
            "Verification Level: %s. You now have full access to all trading features.",
            kycReferenceId,
            verificationLevel
        );
    }
}
