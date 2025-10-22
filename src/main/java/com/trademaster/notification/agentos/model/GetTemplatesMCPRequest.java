package com.trademaster.notification.agentos.model;

/**
 * MCP Request for Getting Notification Templates
 *
 * Request payload for notification.templates.list MCP method.
 * Allows agents to retrieve available notification templates.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records - Immutable data structure
 * - Rule #3: Functional Programming - Immutable records
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
public record GetTemplatesMCPRequest(
    String category,
    String language,
    Boolean activeOnly
) {
    /**
     * Default constructor with sensible defaults
     *
     * MANDATORY: Rule #4 - Builder Pattern for complex construction
     * Complexity: 1
     */
    public GetTemplatesMCPRequest {
        activeOnly = activeOnly != null ? activeOnly : true;
    }
}
