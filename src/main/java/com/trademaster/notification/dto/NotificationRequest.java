package com.trademaster.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Notification Request DTO
 *
 * MANDATORY: Immutability & Records - Rule #9
 * MANDATORY: Validation - Rule #23
 * MANDATORY: Lombok Standards - Rule #10
 */
@Builder(toBuilder = true)
public record NotificationRequest(
    @NotNull NotificationType type,
    @NotBlank String recipient,
    @Email String emailRecipient,
    String phoneRecipient,
    @NotBlank String subject,
    @NotBlank String content,
    String templateName,
    Map<String, Object> templateVariables,
    @NotNull Priority priority,
    LocalDateTime scheduledAt,
    String referenceId,
    String referenceType,
    Integer maxRetryAttempts
) {
    
    /**
     * Compact constructor with default values
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #9 - Records with validation in compact constructor
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2
     */
    public NotificationRequest {
        // Rule #3: NO if-else, use Optional.orElse() for default values
        maxRetryAttempts = Optional.ofNullable(maxRetryAttempts).orElse(3);
        scheduledAt = Optional.ofNullable(scheduledAt).orElse(LocalDateTime.now());
    }
    
    // Factory methods for common notifications
    public static NotificationRequest email(String recipient, String subject, String content) {
        return new NotificationRequest(
            NotificationType.EMAIL,
            recipient,
            recipient,
            null,
            subject,
            content,
            null,
            null,
            Priority.MEDIUM,
            null,
            null,
            null,
            null
        );
    }
    
    public static NotificationRequest sms(String phoneNumber, String subject, String content) {
        return new NotificationRequest(
            NotificationType.SMS,
            phoneNumber,
            null,
            phoneNumber,
            subject,
            content,
            null,
            null,
            Priority.MEDIUM,
            null,
            null,
            null,
            null
        );
    }
    
    public static NotificationRequest templated(NotificationType type, String recipient, 
                                              String templateName, Map<String, Object> variables) {
        return new NotificationRequest(
            type,
            recipient,
            type == NotificationType.EMAIL ? recipient : null,
            type == NotificationType.SMS ? recipient : null,
            "Template: " + templateName,
            "",
            templateName,
            variables,
            Priority.MEDIUM,
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Create push notification request
     */
    public static NotificationRequest push(String deviceToken, String title, String content) {
        return new NotificationRequest(
            NotificationType.PUSH,
            deviceToken,
            null,
            null,
            title,
            content,
            null,
            null,
            Priority.MEDIUM,
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Create in-app notification request
     */
    public static NotificationRequest inApp(String userId, String title, String content) {
        return new NotificationRequest(
            NotificationType.IN_APP,
            userId,
            null,
            null,
            title,
            content,
            null,
            null,
            Priority.MEDIUM,
            null,
            null,
            null,
            null
        );
    }
    
    public enum NotificationType {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
    
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}