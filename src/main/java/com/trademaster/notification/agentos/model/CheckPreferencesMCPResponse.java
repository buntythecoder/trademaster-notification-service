package com.trademaster.notification.agentos.model;

/**
 * MCP Response for Checking Notification Preferences
 *
 * Response payload for notification.preferences.check MCP method.
 * Indicates whether notification is allowed for the user.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records - Immutable data structure
 * - Rule #3: Functional Programming - Immutable records
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
public record CheckPreferencesMCPResponse(
    String userId,
    Boolean allowed,
    String message
) {
    /**
     * Factory method for allowed response
     *
     * MANDATORY: Rule #4 - Factory Pattern
     * Complexity: 1
     */
    public static CheckPreferencesMCPResponse allowed(String userId) {
        return new CheckPreferencesMCPResponse(userId, true, "Notification allowed");
    }

    /**
     * Factory method for blocked response
     *
     * MANDATORY: Rule #4 - Factory Pattern
     * Complexity: 1
     */
    public static CheckPreferencesMCPResponse blocked(String userId, String reason) {
        return new CheckPreferencesMCPResponse(userId, false, reason);
    }
}
