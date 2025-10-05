package com.trademaster.notification.entity;

import com.trademaster.notification.dto.NotificationRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Notification History Entity for Persistence
 * 
 * MANDATORY: JPA Best Practices - Rule #21
 * MANDATORY: Immutability where possible - Rule #9
 * MANDATORY: Constants Usage - Rule #17
 */
@Entity
@Table(name = "notification_history", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipient"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_created", columnList = "created_at"),
    @Index(name = "idx_notification_correlation", columnList = "correlation_id")
})
@Getter
@Setter
@NoArgsConstructor
public class NotificationHistory {
    
    @Id
    @Column(name = "notification_id", length = 36)
    private String notificationId;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationRequest.NotificationType type;
    
    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;
    
    @Column(name = "email_recipient", length = 255)
    private String emailRecipient;
    
    @Column(name = "phone_recipient", length = 50)
    private String phoneRecipient;
    
    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "template_name", length = 100)
    private String templateName;
    
    @ElementCollection
    @CollectionTable(name = "notification_template_variables", 
                    joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "variable_key", length = 100)
    @Column(name = "variable_value", length = 1000)
    private Map<String, String> templateVariables;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private NotificationRequest.Priority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;
    
    @Column(name = "external_message_id", length = 255)
    private String externalMessageId;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "max_retry_attempts")
    private Integer maxRetryAttempts;
    
    @Column(name = "reference_id", length = 100)
    private String referenceId;
    
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100, nullable = false)
    private String updatedBy;
    
    /**
     * Notification status enumeration
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    public enum NotificationStatus {
        PENDING,
        SENT,
        DELIVERED,
        READ,
        FAILED,
        CANCELLED,
        EXPIRED
    }
    
    /**
     * Factory method to create from NotificationRequest
     * 
     * MANDATORY: Factory Pattern - Rule #4
     */
    public static NotificationHistory fromRequest(NotificationRequest request, String notificationId, String correlationId) {
        NotificationHistory history = new NotificationHistory();
        history.setNotificationId(notificationId);
        history.setCorrelationId(correlationId);
        history.setType(request.type());
        history.setRecipient(request.recipient());
        history.setEmailRecipient(request.emailRecipient());
        history.setPhoneRecipient(request.phoneRecipient());
        history.setSubject(request.subject());
        history.setContent(request.content());
        history.setTemplateName(request.templateName());
        // Convert Map<String, Object> to Map<String, String> for database storage        Map<String, String> stringVariables = request.templateVariables() != null            ? request.templateVariables().entrySet().stream()                .collect(java.util.stream.Collectors.toMap(                    Map.Entry::getKey,                    entry -> entry.getValue() != null ? entry.getValue().toString() : null))            : null;        history.setTemplateVariables(stringVariables);
        history.setPriority(request.priority());
        history.setStatus(NotificationStatus.PENDING);
        history.setScheduledAt(request.scheduledAt());
        history.setRetryCount(0);
        history.setMaxRetryAttempts(request.maxRetryAttempts());
        history.setReferenceId(request.referenceId());
        history.setReferenceType(request.referenceType());
        history.setCreatedBy("system");
        history.setUpdatedBy("system");
        return history;
    }
    
    /**
     * Update status with timestamp
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    public NotificationHistory updateStatus(NotificationStatus newStatus, String updatedBy) {
        this.setStatus(newStatus);
        this.setUpdatedBy(updatedBy);
        
        return switch (newStatus) {
            case SENT -> {
                this.setSentAt(LocalDateTime.now());
                yield this;
            }
            case DELIVERED -> {
                this.setDeliveredAt(LocalDateTime.now());
                yield this;
            }
            case READ -> {
                this.setReadAt(LocalDateTime.now());
                yield this;
            }
            default -> this;
        };
    }
    
    /**
     * Mark as failed with error message
     */
    public NotificationHistory markAsFailed(String errorMessage, String updatedBy) {
        this.setStatus(NotificationStatus.FAILED);
        this.setErrorMessage(errorMessage);
        this.setUpdatedBy(updatedBy);
        return this;
    }
    
    /**
     * Increment retry count
     */
    public NotificationHistory incrementRetry() {
        this.setRetryCount(this.getRetryCount() + 1);
        return this;
    }
    
    /**
     * Check if retry is allowed
     */
    public boolean canRetry() {
        return this.maxRetryAttempts != null && 
               this.retryCount < this.maxRetryAttempts;
    }
}