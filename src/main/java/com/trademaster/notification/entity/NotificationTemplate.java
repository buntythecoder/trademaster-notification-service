package com.trademaster.notification.entity;

import com.trademaster.notification.dto.NotificationRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Notification Template Entity for Template Management
 * 
 * MANDATORY: JPA Best Practices - Rule #21
 * MANDATORY: Template Management - FRONT-020
 * MANDATORY: Version Control - Rule #25
 */
@Entity
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_template_name", columnList = "template_name", unique = true),
    @Index(name = "idx_template_category", columnList = "category"),
    @Index(name = "idx_template_type", columnList = "notification_type"),
    @Index(name = "idx_template_active", columnList = "active"),
    @Index(name = "idx_template_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class NotificationTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id", length = 36)
    private String templateId;
    
    @Column(name = "template_name", nullable = false, unique = true, length = 100)
    private String templateName;
    
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationRequest.NotificationType notificationType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private TemplateCategory category;
    
    @Column(name = "subject_template", length = 500)
    private String subjectTemplate;
    
    @Lob
    @Column(name = "content_template", nullable = false)
    private String contentTemplate;
    
    @Lob
    @Column(name = "html_template")
    private String htmlTemplate;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "template_variables", 
                    joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "variable_name", length = 100)
    private Set<String> requiredVariables;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "template_optional_variables", 
                    joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "variable_name", length = 100)
    private Set<String> optionalVariables;
    
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "default_priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationRequest.Priority defaultPriority = NotificationRequest.Priority.MEDIUM;
    
    @Column(name = "rate_limit_per_hour")
    private Integer rateLimitPerHour;
    
    @Column(name = "tags", length = 500)
    private String tags;
    
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
     * Template categories for organization
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    public enum TemplateCategory {
        AUTHENTICATION,     // Login, OTP, password reset
        TRANSACTIONAL,      // Order confirmations, receipts
        MARKETING,          // Promotions, newsletters
        SYSTEM,             // Alerts, maintenance notices
        TRADING,            // Trade confirmations, market alerts
        ACCOUNT,            // Account updates, KYC notices
        SUPPORT,            // Help desk, customer service
        COMPLIANCE,         // Regulatory notices
        WELCOME,            // Welcome messages, onboarding
        NOTIFICATION        // General notifications
    }
    
    /**
     * Factory method for creating new template
     * 
     * MANDATORY: Factory Pattern - Rule #4
     */
    public static NotificationTemplate create(
            String templateName,
            String displayName,
            String description,
            NotificationRequest.NotificationType notificationType,
            TemplateCategory category,
            String subjectTemplate,
            String contentTemplate,
            String createdBy) {
        
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateName(templateName);
        template.setDisplayName(displayName);
        template.setDescription(description);
        template.setNotificationType(notificationType);
        template.setCategory(category);
        template.setSubjectTemplate(subjectTemplate);
        template.setContentTemplate(contentTemplate);
        template.setVersion(1);
        template.setActive(true);
        template.setCreatedBy(createdBy);
        template.setUpdatedBy(createdBy);
        return template;
    }
    
    /**
     * Create new version of template
     * 
     * MANDATORY: Factory Pattern - Rule #4
     */
    public NotificationTemplate createNewVersion(String updatedBy) {
        NotificationTemplate newVersion = new NotificationTemplate();
        newVersion.setTemplateName(this.templateName);
        newVersion.setDisplayName(this.displayName);
        newVersion.setDescription(this.description);
        newVersion.setNotificationType(this.notificationType);
        newVersion.setCategory(this.category);
        newVersion.setSubjectTemplate(this.subjectTemplate);
        newVersion.setContentTemplate(this.contentTemplate);
        newVersion.setHtmlTemplate(this.htmlTemplate);
        newVersion.setRequiredVariables(this.requiredVariables);
        newVersion.setOptionalVariables(this.optionalVariables);
        newVersion.setVersion(this.version + 1);
        newVersion.setActive(true);
        newVersion.setDefaultPriority(this.defaultPriority);
        newVersion.setRateLimitPerHour(this.rateLimitPerHour);
        newVersion.setTags(this.tags);
        newVersion.setCreatedBy(updatedBy);
        newVersion.setUpdatedBy(updatedBy);
        return newVersion;
    }
    
    /**
     * Activate/deactivate template
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    public NotificationTemplate updateStatus(boolean active, String updatedBy) {
        this.setActive(active);
        this.setUpdatedBy(updatedBy);
        return this;
    }
    
    /**
     * Check if template is valid for sending
     */
    public boolean isValidForSending() {
        return this.active && 
               this.contentTemplate != null && 
               !this.contentTemplate.isEmpty();
    }
}