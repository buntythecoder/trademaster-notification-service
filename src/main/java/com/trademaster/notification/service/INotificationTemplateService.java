package com.trademaster.notification.service;

import com.trademaster.notification.entity.NotificationTemplate;
import com.trademaster.notification.entity.NotificationTemplate.TemplateCategory;
import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.common.Result;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Notification Template Service Interface
 * 
 * MANDATORY: Dependency Inversion Principle - Rule #2
 * MANDATORY: Interface Segregation - Rule #2
 * MANDATORY: Virtual Threads - Rule #12
 */
public interface INotificationTemplateService {
    
    /**
     * Create new notification template
     */
    CompletableFuture<Result<NotificationTemplate, String>> createTemplate(
            String templateName,
            String displayName,
            String description,
            NotificationRequest.NotificationType notificationType,
            TemplateCategory category,
            String subjectTemplate,
            String contentTemplate,
            String createdBy);
    
    /**
     * Update existing template
     */
    CompletableFuture<Result<NotificationTemplate, String>> updateTemplate(
            String templateId,
            String displayName,
            String description,
            String subjectTemplate,
            String contentTemplate,
            String htmlTemplate,
            String updatedBy);
    
    /**
     * Create new version of template
     */
    CompletableFuture<Result<NotificationTemplate, String>> createTemplateVersion(
            String templateId,
            String updatedBy);
    
    /**
     * Activate/deactivate template
     */
    CompletableFuture<Result<NotificationTemplate, String>> updateTemplateStatus(
            String templateId,
            boolean active,
            String updatedBy);
    
    /**
     * Get template by name (active only)
     */
    CompletableFuture<Optional<NotificationTemplate>> getTemplateByName(String templateName);
    
    /**
     * Get latest version of template by name
     */
    CompletableFuture<Optional<NotificationTemplate>> getLatestTemplateVersion(String templateName);
    
    /**
     * Get templates by category
     */
    CompletableFuture<List<NotificationTemplate>> getTemplatesByCategory(TemplateCategory category);
    
    /**
     * Get templates by notification type
     */
    CompletableFuture<List<NotificationTemplate>> getTemplatesByType(
            NotificationRequest.NotificationType type);
    
    /**
     * Search templates with pagination and filters
     */
    CompletableFuture<Page<NotificationTemplate>> searchTemplates(
            TemplateCategory category,
            NotificationRequest.NotificationType type,
            Boolean active,
            Pageable pageable);
    
    /**
     * Search templates by content
     */
    CompletableFuture<List<NotificationTemplate>> searchTemplatesByTerm(String searchTerm);
    
    /**
     * Get template usage statistics
     */
    CompletableFuture<Map<String, Long>> getTemplateUsageStatistics(LocalDateTime fromDate);
    
    /**
     * Bulk update template status
     */
    CompletableFuture<Result<Integer, String>> updateTemplateStatusBatch(
            List<String> templateIds,
            Boolean active,
            String updatedBy);
    
    /**
     * Delete template (soft delete)
     */
    CompletableFuture<Result<String, String>> deleteTemplate(String templateId, String updatedBy);
    
    /**
     * Get template categories statistics
     */
    CompletableFuture<Map<TemplateCategory, Long>> getCategoryStatistics();
    
    /**
     * Validate template for sending
     */
    CompletableFuture<Boolean> validateTemplateForSending(String templateName);
}