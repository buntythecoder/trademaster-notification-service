package com.trademaster.notification.agentos.model;

import com.trademaster.notification.dto.NotificationRequest;

/**
 * MCP Request for Sending Notification
 *
 * Request payload for notification.send MCP method.
 * Enables agents to send notifications via the notification service.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records - Immutable data structure
 * - Rule #3: Functional Programming - Immutable records
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
public record SendNotificationMCPRequest(
    NotificationRequest.NotificationType type,
    String recipient,
    String subject,
    String content,
    String correlationId,
    String sourceAgentId
) {
    /**
     * Validates the MCP request
     *
     * MANDATORY: Rule #3 - Functional Programming (no if-else)
     * Complexity: 1
     */
    public boolean isValid() {
        return type != null &&
               recipient != null && !recipient.isBlank() &&
               content != null && !content.isBlank();
    }
}
