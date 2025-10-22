package com.trademaster.notification.agentos.model;

import java.util.List;

/**
 * MCP Response for Getting Notification Templates
 *
 * Response payload for notification.templates.list MCP method.
 * Contains list of available notification templates.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #9: Immutability & Records - Immutable data structure
 * - Rule #3: Functional Programming - Immutable records
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
public record GetTemplatesMCPResponse(
    List<TemplateInfo> templates
) {
    /**
     * Template information record
     *
     * MANDATORY: Rule #9 - Immutability
     */
    public record TemplateInfo(
        String templateName,
        String category,
        String language,
        Boolean isActive,
        String subjectPreview,
        String contentPreview
    ) {}
}
