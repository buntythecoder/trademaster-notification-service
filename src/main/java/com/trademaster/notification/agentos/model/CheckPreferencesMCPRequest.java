package com.trademaster.notification.agentos.model;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.entity.NotificationTemplate.TemplateCategory;

/**
 * MCP Request for Checking Notification Preferences
 *
 * Request payload for notification.preferences.check MCP method.
 * Enables agents to verify if a user allows specific notifications.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records - Immutable data structure
 * - Rule #3: Functional Programming - Immutable records
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
public record CheckPreferencesMCPRequest(
    String userId,
    NotificationRequest.NotificationType type,
    TemplateCategory category
) {
    /**
     * Validates the MCP request
     *
     * MANDATORY: Rule #3 - Functional Programming (no if-else)
     * Complexity: 1
     */
    public boolean isValid() {
        return userId != null && !userId.isBlank() &&
               type != null &&
               category != null;
    }
}
