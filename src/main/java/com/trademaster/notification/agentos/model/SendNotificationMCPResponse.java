package com.trademaster.notification.agentos.model;

/**
 * MCP Response for Sending Notification
 *
 * Response payload for notification.send MCP method.
 * Contains notification ID, status, and success indicator.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records - Immutable data structure
 * - Rule #3: Functional Programming - Immutable records
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
public record SendNotificationMCPResponse(
    String notificationId,
    String status,
    Boolean success,
    String message
) {
    /**
     * Factory method for successful response
     *
     * MANDATORY: Rule #4 - Factory Pattern
     * Complexity: 1
     */
    public static SendNotificationMCPResponse success(String notificationId, String message) {
        return new SendNotificationMCPResponse(notificationId, "SUCCESS", true, message);
    }

    /**
     * Factory method for failed response
     *
     * MANDATORY: Rule #4 - Factory Pattern
     * Complexity: 1
     */
    public static SendNotificationMCPResponse failure(String message) {
        return new SendNotificationMCPResponse(null, "FAILED", false, message);
    }
}
