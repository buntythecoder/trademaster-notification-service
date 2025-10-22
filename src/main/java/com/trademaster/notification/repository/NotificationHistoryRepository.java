package com.trademaster.notification.repository;

import com.trademaster.notification.entity.NotificationHistory;
import com.trademaster.notification.entity.NotificationHistory.NotificationStatus;
import com.trademaster.notification.dto.NotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Notification History Repository
 * 
 * MANDATORY: JPA Best Practices - Rule #21
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Repository Pattern - Rule #4
 */
@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, String> {
    
    /**
     * Find notifications by recipient with pagination
     * 
     * MANDATORY: Functional Queries - Rule #3
     */
    @Query("SELECT n FROM NotificationHistory n WHERE n.recipient = :recipient ORDER BY n.createdAt DESC")
    Page<NotificationHistory> findByRecipientOrderByCreatedAtDesc(
            @Param("recipient") String recipient, 
            Pageable pageable);
    
    /**
     * Find notifications by status and type
     */
    @Query("SELECT n FROM NotificationHistory n WHERE n.status = :status AND n.type = :type ORDER BY n.createdAt ASC")
    List<NotificationHistory> findByStatusAndTypeOrderByCreatedAtAsc(
            @Param("status") NotificationStatus status, 
            @Param("type") NotificationRequest.NotificationType type);
    
    /**
     * Find pending notifications eligible for retry
     */
    @Query("""
           SELECT n FROM NotificationHistory n 
           WHERE n.status = 'FAILED' 
           AND n.retryCount < n.maxRetryAttempts 
           AND n.createdAt > :cutoffTime
           ORDER BY n.createdAt ASC
           """)
    List<NotificationHistory> findEligibleForRetry(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find notifications by correlation ID
     */
    Optional<List<NotificationHistory>> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);
    
    /**
     * Find notifications by reference ID and type
     */
    @Query("SELECT n FROM NotificationHistory n WHERE n.referenceId = :referenceId AND n.referenceType = :referenceType")
    Optional<List<NotificationHistory>> findByReferenceIdAndReferenceType(
            @Param("referenceId") String referenceId, 
            @Param("referenceType") String referenceType);
    
    /**
     * Find notifications by template name for analytics
     */
    @Query("SELECT n FROM NotificationHistory n WHERE n.templateName = :templateName AND n.createdAt >= :fromDate")
    List<NotificationHistory> findByTemplateNameAndCreatedAtAfter(
            @Param("templateName") String templateName, 
            @Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Count notifications by status within time period
     */
    @Query("SELECT COUNT(n) FROM NotificationHistory n WHERE n.status = :status AND n.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndCreatedAtBetween(
            @Param("status") NotificationStatus status, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find failed notifications with error patterns
     */
    @Query("""
           SELECT n FROM NotificationHistory n 
           WHERE n.status = 'FAILED' 
           AND n.errorMessage LIKE %:errorPattern% 
           AND n.createdAt >= :fromDate
           """)
    List<NotificationHistory> findFailedNotificationsWithErrorPattern(
            @Param("errorPattern") String errorPattern, 
            @Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find notifications by recipient and status for user management
     */
    @Query("""
           SELECT n FROM NotificationHistory n 
           WHERE n.recipient = :recipient 
           AND n.status IN :statuses 
           AND n.createdAt >= :fromDate 
           ORDER BY n.createdAt DESC
           """)
    List<NotificationHistory> findByRecipientAndStatusInAndCreatedAtAfter(
            @Param("recipient") String recipient, 
            @Param("statuses") List<NotificationStatus> statuses, 
            @Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get delivery statistics for reporting
     */
    @Query("""
           SELECT n.status, COUNT(n)
           FROM NotificationHistory n
           WHERE n.createdAt BETWEEN :startDate AND :endDate
           GROUP BY n.status
           """)
    List<Object[]> getDeliveryStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find notifications by user with optional type and status filters
     *
     * MANDATORY: Rule #3 - Functional Programming (JPQL with conditionals)
     * MANDATORY: Rule #16 - Dynamic Configuration (supports optional filters)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 1
     *
     * @param recipient User ID to filter by (required)
     * @param type Notification type filter (optional - pass null to ignore)
     * @param status Notification status filter (optional - pass null to ignore)
     * @param pageable Pagination parameters
     * @return Page of notification history matching filters
     */
    @Query("""
           SELECT n FROM NotificationHistory n
           WHERE n.recipient = :recipient
           AND (:type IS NULL OR n.type = :type)
           AND (:status IS NULL OR n.status = :status)
           ORDER BY n.createdAt DESC
           """)
    Page<NotificationHistory> findByRecipientWithOptionalFilters(
            @Param("recipient") String recipient,
            @Param("type") NotificationRequest.NotificationType type,
            @Param("status") NotificationStatus status,
            Pageable pageable);
}