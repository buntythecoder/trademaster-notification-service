package com.trademaster.notification.repository;

import com.trademaster.notification.entity.NotificationTemplate;
import com.trademaster.notification.entity.NotificationTemplate.TemplateCategory;
import com.trademaster.notification.dto.NotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Notification Template Repository
 * 
 * MANDATORY: JPA Best Practices - Rule #21
 * MANDATORY: Template Management - FRONT-020
 * MANDATORY: Functional Programming - Rule #3
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
    
    /**
     * Find template by unique name
     * 
     * MANDATORY: Functional Queries - Rule #3
     */
    Optional<NotificationTemplate> findByTemplateNameAndActiveTrue(String templateName);
    
    /**
     * Find active templates by category
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.category = :category AND t.active = true ORDER BY t.displayName ASC")
    List<NotificationTemplate> findByCategoryAndActiveTrueOrderByDisplayNameAsc(@Param("category") TemplateCategory category);
    
    /**
     * Find active templates by notification type
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.notificationType = :type AND t.active = true ORDER BY t.displayName ASC")
    List<NotificationTemplate> findByNotificationTypeAndActiveTrueOrderByDisplayNameAsc(
            @Param("type") NotificationRequest.NotificationType type);
    
    /**
     * Find templates by category and type
     */
    @Query("""
           SELECT t FROM NotificationTemplate t 
           WHERE t.category = :category 
           AND t.notificationType = :type 
           AND t.active = true 
           ORDER BY t.displayName ASC
           """)
    List<NotificationTemplate> findByCategoryAndNotificationTypeAndActiveTrueOrderByDisplayNameAsc(
            @Param("category") TemplateCategory category, 
            @Param("type") NotificationRequest.NotificationType type);
    
    /**
     * Find all templates with pagination and filtering
     */
    @Query("""
           SELECT t FROM NotificationTemplate t 
           WHERE (:category IS NULL OR t.category = :category) 
           AND (:type IS NULL OR t.notificationType = :type) 
           AND (:active IS NULL OR t.active = :active) 
           ORDER BY t.createdAt DESC
           """)
    Page<NotificationTemplate> findTemplatesWithFilters(
            @Param("category") TemplateCategory category, 
            @Param("type") NotificationRequest.NotificationType type, 
            @Param("active") Boolean active, 
            Pageable pageable);
    
    /**
     * Find templates by version for template management
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.templateName = :templateName ORDER BY t.version DESC")
    List<NotificationTemplate> findByTemplateNameOrderByVersionDesc(@Param("templateName") String templateName);
    
    /**
     * Get latest version of template
     */
    @Query("""
           SELECT t FROM NotificationTemplate t 
           WHERE t.templateName = :templateName 
           AND t.version = (
               SELECT MAX(t2.version) FROM NotificationTemplate t2 
               WHERE t2.templateName = :templateName AND t2.active = true
           )
           """)
    Optional<NotificationTemplate> findLatestVersionByTemplateName(@Param("templateName") String templateName);
    
    /**
     * Find templates needing rate limiting
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.rateLimitPerHour IS NOT NULL AND t.active = true")
    List<NotificationTemplate> findTemplatesWithRateLimit();
    
    /**
     * Search templates by content or name
     */
    @Query("""
           SELECT t FROM NotificationTemplate t 
           WHERE (LOWER(t.templateName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
           OR LOWER(t.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
           OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) 
           AND t.active = true 
           ORDER BY t.displayName ASC
           """)
    List<NotificationTemplate> searchActiveTemplatesByTerm(@Param("searchTerm") String searchTerm);
    
    /**
     * Find templates by tags
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE LOWER(t.tags) LIKE LOWER(CONCAT('%', :tag, '%')) AND t.active = true")
    List<NotificationTemplate> findByTagsContainingIgnoreCaseAndActiveTrue(@Param("tag") String tag);
    
    /**
     * Bulk update template status
     */
    @Modifying
    @Transactional
    @Query("UPDATE NotificationTemplate t SET t.active = :active, t.updatedBy = :updatedBy WHERE t.templateId IN :templateIds")
    int updateTemplateStatusBatch(
            @Param("templateIds") List<String> templateIds, 
            @Param("active") Boolean active, 
            @Param("updatedBy") String updatedBy);
    
    /**
     * Find templates created by user
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.createdBy = :createdBy ORDER BY t.createdAt DESC")
    List<NotificationTemplate> findByCreatedByOrderByCreatedAtDesc(@Param("createdBy") String createdBy);
    
    /**
     * Find templates modified within timeframe
     */
    @Query("SELECT t FROM NotificationTemplate t WHERE t.updatedAt >= :fromDate ORDER BY t.updatedAt DESC")
    List<NotificationTemplate> findByUpdatedAtAfterOrderByUpdatedAtDesc(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get template usage statistics
     */
    @Query("""
           SELECT t.templateName, COUNT(n.notificationId) 
           FROM NotificationTemplate t 
           LEFT JOIN NotificationHistory n ON t.templateName = n.templateName 
           AND n.createdAt >= :fromDate 
           WHERE t.active = true 
           GROUP BY t.templateName 
           ORDER BY COUNT(n.notificationId) DESC
           """)
    List<Object[]> getTemplateUsageStatistics(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Check if template name exists (for unique validation)
     */
    boolean existsByTemplateName(String templateName);
    
    /**
     * Count active templates by category
     */
    @Query("SELECT COUNT(t) FROM NotificationTemplate t WHERE t.category = :category AND t.active = true")
    Long countByCategoryAndActiveTrue(@Param("category") TemplateCategory category);
}