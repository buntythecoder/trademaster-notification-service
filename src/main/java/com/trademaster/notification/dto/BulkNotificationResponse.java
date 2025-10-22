package com.trademaster.notification.dto;

import lombok.Builder;
import java.util.List;

/**
 * Bulk Notification Response DTO
 *
 * MANDATORY: Immutability & Records - Rule #9
 * MANDATORY: Lombok Standards - Rule #10
 */
@Builder(toBuilder = true)
public record BulkNotificationResponse(
    int totalRequests,
    int successfulRequests,
    int failedRequests,
    List<NotificationResponse> notifications,
    List<String> failureReasons
) {

    /**
     * Factory method for successful bulk operation
     */
    public static BulkNotificationResponse success(List<NotificationResponse> notifications) {
        return new BulkNotificationResponse(
            notifications.size(),
            notifications.size(),
            0,
            notifications,
            List.of()
        );
    }

    /**
     * Factory method with mixed results
     */
    public static BulkNotificationResponse mixed(
            int total,
            List<NotificationResponse> successful,
            List<String> failures) {
        return new BulkNotificationResponse(
            total,
            successful.size(),
            failures.size(),
            successful,
            failures
        );
    }
}
