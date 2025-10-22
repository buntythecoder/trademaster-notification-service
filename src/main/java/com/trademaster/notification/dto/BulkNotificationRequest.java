package com.trademaster.notification.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * Bulk Notification Request DTO
 *
 * MANDATORY: Immutability & Records - Rule #9
 * MANDATORY: Validation - Rule #23
 * MANDATORY: Lombok Standards - Rule #10
 */
@Builder(toBuilder = true)
public record BulkNotificationRequest(
    @NotNull NotificationRequest.NotificationType type,
    @NotEmpty List<String> recipients,
    @NotNull String subject,
    @NotNull String content,
    String templateName,
    Map<String, Object> templateVariables,
    @NotNull NotificationRequest.Priority priority,
    String referenceType,
    boolean personalizeContent
) {
    
    // Factory methods
    public static BulkNotificationRequest email(List<String> recipients, String subject, String content) {
        return new BulkNotificationRequest(
            NotificationRequest.NotificationType.EMAIL,
            recipients,
            subject,
            content,
            null,
            null,
            NotificationRequest.Priority.MEDIUM,
            "BULK_EMAIL",
            false
        );
    }
    
    public static BulkNotificationRequest sms(List<String> recipients, String content) {
        return new BulkNotificationRequest(
            NotificationRequest.NotificationType.SMS,
            recipients,
            "Bulk SMS",
            content,
            null,
            null,
            NotificationRequest.Priority.MEDIUM,
            "BULK_SMS",
            false
        );
    }
    
    public static BulkNotificationRequest templated(NotificationRequest.NotificationType type,
                                                   List<String> recipients,
                                                   String templateName,
                                                   Map<String, Object> variables) {
        return new BulkNotificationRequest(
            type,
            recipients,
            "Template: " + templateName,
            "",
            templateName,
            variables,
            NotificationRequest.Priority.MEDIUM,
            "BULK_TEMPLATE",
            true
        );
    }
}