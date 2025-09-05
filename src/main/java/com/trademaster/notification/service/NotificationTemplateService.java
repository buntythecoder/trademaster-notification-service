package com.trademaster.notification.service;

import com.trademaster.notification.entity.NotificationTemplate;
import com.trademaster.notification.entity.NotificationTemplate.TemplateCategory;
import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.repository.NotificationTemplateRepository;
import com.trademaster.notification.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Notification Template Management Service
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Template Management - FRONT-020
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationTemplateService implements INotificationTemplateService {
    
    private final NotificationTemplateRepository templateRepository;
    
    /**
     * Create new notification template
     * 
     * MANDATORY: Factory Pattern - Rule #4
     * MANDATORY: Functional Programming - Rule #3
     */
    @Transactional
    public CompletableFuture<Result<NotificationTemplate, String>> createTemplate(
            String templateName,
            String displayName,
            String description,
            NotificationRequest.NotificationType notificationType,
            TemplateCategory category,
            String subjectTemplate,
            String contentTemplate,
            String createdBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performCreateTemplate(
                templateName, displayName, description, notificationType, 
                category, subjectTemplate, contentTemplate, createdBy), 
                Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleTemplateResult);
    }
    
    private Result<NotificationTemplate, String> performCreateTemplate(
            String templateName,
            String displayName,
            String description,
            NotificationRequest.NotificationType notificationType,
            TemplateCategory category,
            String subjectTemplate,
            String contentTemplate,
            String createdBy) {
        
        return Result.tryExecute(() -> {
            // Check if template name already exists
            if (templateRepository.existsByTemplateName(templateName)) {
                throw new IllegalArgumentException("Template with name '" + templateName + "' already exists");
            }
            
            NotificationTemplate template = NotificationTemplate.create(
                templateName, displayName, description, notificationType,
                category, subjectTemplate, contentTemplate, createdBy);
            
            NotificationTemplate saved = templateRepository.save(template);
            
            log.info("Template created: ID={}, name={}, category={}", 
                    saved.getTemplateId(), saved.getTemplateName(), saved.getCategory());
            
            return saved;
        }).match(
            template -> Result.success(template),
            exception -> {
                log.error("Failed to create template '{}': {}", templateName, exception.getMessage());
                return Result.failure("Failed to create template: " + exception.getMessage());
            }
        );
    }
    
    /**
     * Update existing template
     */
    @Transactional
    public CompletableFuture<Result<NotificationTemplate, String>> updateTemplate(
            String templateId,
            String displayName,
            String description,
            String subjectTemplate,
            String contentTemplate,
            String htmlTemplate,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performUpdateTemplate(
                templateId, displayName, description, subjectTemplate, 
                contentTemplate, htmlTemplate, updatedBy), 
                Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleTemplateResult);
    }
    
    private Result<NotificationTemplate, String> performUpdateTemplate(
            String templateId,
            String displayName,
            String description,
            String subjectTemplate,
            String contentTemplate,
            String htmlTemplate,
            String updatedBy) {
        
        return templateRepository.findById(templateId)
            .map(template -> Result.tryExecute(() -> {
                template.setDisplayName(displayName);
                template.setDescription(description);
                template.setSubjectTemplate(subjectTemplate);
                template.setContentTemplate(contentTemplate);
                template.setHtmlTemplate(htmlTemplate);
                template.setUpdatedBy(updatedBy);
                
                NotificationTemplate updated = templateRepository.save(template);
                
                log.info("Template updated: ID={}, name={}", updated.getTemplateId(), updated.getTemplateName());
                
                return updated;
            }).mapError(exception -> "Failed to update template: " + exception.getMessage()))
            .orElse(Result.<NotificationTemplate, String>failure("Template not found: " + templateId));
    }
    
    /**
     * Create new version of template
     */
    @Transactional
    public CompletableFuture<Result<NotificationTemplate, String>> createTemplateVersion(
            String templateId,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performCreateVersion(templateId, updatedBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleTemplateResult);
    }
    
    private Result<NotificationTemplate, String> performCreateVersion(String templateId, String updatedBy) {
        return templateRepository.findById(templateId)
            .map(template -> Result.tryExecute(() -> {
                // Deactivate current version
                template.updateStatus(false, updatedBy);
                templateRepository.save(template);
                
                // Create new version
                NotificationTemplate newVersion = template.createNewVersion(updatedBy);
                NotificationTemplate saved = templateRepository.save(newVersion);
                
                log.info("New template version created: ID={}, name={}, version={}", 
                        saved.getTemplateId(), saved.getTemplateName(), saved.getVersion());
                
                return saved;
            }).mapError(exception -> "Failed to create template version: " + exception.getMessage()))
            .orElse(Result.<NotificationTemplate, String>failure("Template not found: " + templateId));
    }
    
    /**
     * Activate/deactivate template
     */
    @Transactional
    public CompletableFuture<Result<NotificationTemplate, String>> updateTemplateStatus(
            String templateId,
            boolean active,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performUpdateStatus(templateId, active, updatedBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleTemplateResult);
    }
    
    private Result<NotificationTemplate, String> performUpdateStatus(
            String templateId, 
            boolean active, 
            String updatedBy) {
        
        return templateRepository.findById(templateId)
            .map(template -> Result.tryExecute(() -> {
                template.updateStatus(active, updatedBy);
                NotificationTemplate updated = templateRepository.save(template);
                
                log.info("Template status updated: ID={}, name={}, active={}", 
                        updated.getTemplateId(), updated.getTemplateName(), updated.getActive());
                
                return updated;
            }).mapError(exception -> "Failed to update template status: " + exception.getMessage()))
            .orElse(Result.<NotificationTemplate, String>failure("Template not found: " + templateId));
    }
    
    /**
     * Get template by name (active only)
     */
    public CompletableFuture<Optional<NotificationTemplate>> getTemplateByName(String templateName) {
        return CompletableFuture
            .supplyAsync(() -> templateRepository.findByTemplateNameAndActiveTrue(templateName), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get latest version of template by name
     */
    public CompletableFuture<Optional<NotificationTemplate>> getLatestTemplateVersion(String templateName) {
        return CompletableFuture
            .supplyAsync(() -> templateRepository.findLatestVersionByTemplateName(templateName), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get templates by category
     */
    public CompletableFuture<List<NotificationTemplate>> getTemplatesByCategory(TemplateCategory category) {
        return CompletableFuture
            .supplyAsync(() -> templateRepository.findByCategoryAndActiveTrueOrderByDisplayNameAsc(category), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get templates by notification type
     */
    public CompletableFuture<List<NotificationTemplate>> getTemplatesByType(
            NotificationRequest.NotificationType type) {
        return CompletableFuture
            .supplyAsync(() -> templateRepository.findByNotificationTypeAndActiveTrueOrderByDisplayNameAsc(type), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Search templates with pagination and filters
     */
    public CompletableFuture<Page<NotificationTemplate>> searchTemplates(
            TemplateCategory category,
            NotificationRequest.NotificationType type,
            Boolean active,
            Pageable pageable) {
        
        return CompletableFuture
            .supplyAsync(() -> templateRepository.findTemplatesWithFilters(category, type, active, pageable), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Search templates by content
     */
    public CompletableFuture<List<NotificationTemplate>> searchTemplatesByTerm(String searchTerm) {
        return CompletableFuture
            .supplyAsync(() -> templateRepository.searchActiveTemplatesByTerm(searchTerm), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get template usage statistics
     */
    public CompletableFuture<Map<String, Long>> getTemplateUsageStatistics(LocalDateTime fromDate) {
        return CompletableFuture
            .supplyAsync(() -> getUsageStats(fromDate), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    private Map<String, Long> getUsageStats(LocalDateTime fromDate) {
        return templateRepository.getTemplateUsageStatistics(fromDate)
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));
    }
    
    /**
     * Bulk update template status
     */
    @Transactional
    public CompletableFuture<Result<Integer, String>> updateTemplateStatusBatch(
            List<String> templateIds,
            Boolean active,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performBatchUpdate(templateIds, active, updatedBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleIntegerResult);
    }
    
    private Result<Integer, String> performBatchUpdate(
            List<String> templateIds, 
            Boolean active, 
            String updatedBy) {
        
        return Result.tryExecute(() -> {
            int updatedCount = templateRepository.updateTemplateStatusBatch(templateIds, active, updatedBy);
            
            log.info("Batch template status update: {} templates, active={}", updatedCount, active);
            
            return updatedCount;
        }).match(
            count -> Result.success(count),
            exception -> Result.failure("Failed to update templates: " + exception.getMessage())
        );
    }
    
    /**
     * Delete template (soft delete by deactivating)
     */
    @Transactional
    public CompletableFuture<Result<String, String>> deleteTemplate(String templateId, String updatedBy) {
        return updateTemplateStatus(templateId, false, updatedBy)
            .thenApply(result -> result.match(
                template -> Result.<String, String>success("Template deactivated successfully"),
                error -> Result.<String, String>failure(error)
            ));
    }
    
    /**
     * Get template categories statistics
     */
    public CompletableFuture<Map<TemplateCategory, Long>> getCategoryStatistics() {
        return CompletableFuture
            .supplyAsync(() -> getCategoryStats(), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    private Map<TemplateCategory, Long> getCategoryStats() {
        return java.util.Arrays.stream(TemplateCategory.values())
            .collect(Collectors.toMap(
                category -> category,
                category -> templateRepository.countByCategoryAndActiveTrue(category)
            ));
    }
    
    /**
     * Validate template for sending
     */
    public CompletableFuture<Boolean> validateTemplateForSending(String templateName) {
        return getTemplateByName(templateName)
            .thenApply(optionalTemplate -> optionalTemplate
                .map(NotificationTemplate::isValidForSending)
                .orElse(false));
    }
    
    /**
     * Generic result handler for template operations
     */
    private Result<NotificationTemplate, String> handleTemplateResult(
            Result<NotificationTemplate, String> result, 
            Throwable throwable) {
        
        if (throwable != null) {
            log.error("Template operation error", throwable);
            return Result.failure("Template operation failed: " + throwable.getMessage());
        }
        return result;
    }
    
    /**
     * Generic result handler for integer operations
     */
    private Result<Integer, String> handleIntegerResult(
            Result<Integer, String> result, 
            Throwable throwable) {
        
        if (throwable != null) {
            log.error("Template batch operation error", throwable);
            return Result.failure("Batch operation failed: " + throwable.getMessage());
        }
        return result;
    }
}