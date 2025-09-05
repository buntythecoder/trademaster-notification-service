package com.trademaster.notification.repository;

import com.trademaster.notification.entity.UserNotificationPreference;
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
 * User Notification Preference Repository
 * 
 * MANDATORY: JPA Best Practices - Rule #21
 * MANDATORY: User Preference Management - FRONT-020
 * MANDATORY: Functional Programming - Rule #3
 */
@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, String> {
    
    /**
     * Find preferences by user ID
     * 
     * MANDATORY: Functional Queries - Rule #3
     */
    Optional<UserNotificationPreference> findByUserId(String userId);
    
    /**
     * Find users with notifications enabled
     */
    @Query("SELECT p FROM UserNotificationPreference p WHERE p.notificationsEnabled = true")
    List<UserNotificationPreference> findByNotificationsEnabledTrue();
    
    /**
     * Find users by preferred notification channel
     */
    @Query("SELECT p FROM UserNotificationPreference p WHERE p.preferredChannel = :channel AND p.notificationsEnabled = true")
    List<UserNotificationPreference> findByPreferredChannelAndNotificationsEnabledTrue(
            @Param("channel") NotificationRequest.NotificationType channel);
    
    /**
     * Find users who allow marketing notifications
     */
    @Query("SELECT p FROM UserNotificationPreference p WHERE p.marketingEnabled = true AND p.notificationsEnabled = true")
    List<UserNotificationPreference> findByMarketingEnabledTrueAndNotificationsEnabledTrue();
    
    /**
     * Find users by enabled categories for targeted notifications
     */
    @Query("""
           SELECT p FROM UserNotificationPreference p 
           JOIN p.enabledCategories c 
           WHERE c = :category 
           AND p.notificationsEnabled = true
           """)
    List<UserNotificationPreference> findByEnabledCategoriesContainingAndNotificationsEnabledTrue(
            @Param("category") TemplateCategory category);
    
    /**
     * Find users by enabled channels for bulk operations
     */
    @Query("""
           SELECT p FROM UserNotificationPreference p 
           JOIN p.enabledChannels c 
           WHERE c = :channel 
           AND p.notificationsEnabled = true
           """)
    List<UserNotificationPreference> findByEnabledChannelsContainingAndNotificationsEnabledTrue(
            @Param("channel") NotificationRequest.NotificationType channel);
    
    /**
     * Find users with quiet hours enabled (for scheduling)
     */
    @Query("SELECT p FROM UserNotificationPreference p WHERE p.quietHoursEnabled = true AND p.notificationsEnabled = true")
    List<UserNotificationPreference> findByQuietHoursEnabledTrueAndNotificationsEnabledTrue();
    
    /**
     * Find users by language for localization
     */
    @Query("SELECT p FROM UserNotificationPreference p WHERE p.language = :language AND p.notificationsEnabled = true")
    List<UserNotificationPreference> findByLanguageAndNotificationsEnabledTrue(@Param("language") String language);
    
    /**
     * Find users with frequency limits set
     */
    @Query("""
           SELECT p FROM UserNotificationPreference p 
           WHERE (p.frequencyLimitPerHour IS NOT NULL OR p.frequencyLimitPerDay IS NOT NULL) 
           AND p.notificationsEnabled = true
           """)
    List<UserNotificationPreference> findUsersWithFrequencyLimits();
    
    /**
     * Find preferences with pagination and filtering
     */
    @Query("""
           SELECT p FROM UserNotificationPreference p 
           WHERE (:enabled IS NULL OR p.notificationsEnabled = :enabled) 
           AND (:channel IS NULL OR p.preferredChannel = :channel) 
           AND (:language IS NULL OR p.language = :language) 
           ORDER BY p.createdAt DESC
           """)
    Page<UserNotificationPreference> findPreferencesWithFilters(
            @Param("enabled") Boolean enabled, 
            @Param("channel") NotificationRequest.NotificationType channel, 
            @Param("language") String language, 
            Pageable pageable);
    
    /**
     * Bulk update notification status for users
     */
    @Modifying
    @Transactional
    @Query("""
           UPDATE UserNotificationPreference p 
           SET p.notificationsEnabled = :enabled, p.updatedBy = :updatedBy 
           WHERE p.userId IN :userIds
           """)
    int updateNotificationStatusBatch(
            @Param("userIds") List<String> userIds, 
            @Param("enabled") Boolean enabled, 
            @Param("updatedBy") String updatedBy);
    
    /**
     * Find users who opted out recently (for analysis)
     */
    @Query("""
           SELECT p FROM UserNotificationPreference p 
           WHERE p.notificationsEnabled = false 
           AND p.updatedAt >= :fromDate 
           ORDER BY p.updatedAt DESC
           """)
    List<UserNotificationPreference> findUsersOptedOutSince(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Get preference statistics for dashboard
     */
    @Query("""
           SELECT p.preferredChannel, COUNT(p) 
           FROM UserNotificationPreference p 
           WHERE p.notificationsEnabled = true 
           GROUP BY p.preferredChannel 
           ORDER BY COUNT(p) DESC
           """)
    List<Object[]> getChannelPreferenceStatistics();
    
    /**
     * Get language distribution for localization planning
     */
    @Query("""
           SELECT p.language, COUNT(p) 
           FROM UserNotificationPreference p 
           WHERE p.notificationsEnabled = true 
           GROUP BY p.language 
           ORDER BY COUNT(p) DESC
           """)
    List<Object[]> getLanguageDistributionStatistics();
    
    /**
     * Find users needing preference updates (missing contact info)
     */
    @Query("""
           SELECT p FROM UserNotificationPreference p 
           WHERE p.notificationsEnabled = true 
           AND (
               (p.preferredChannel = 'EMAIL' AND (p.emailAddress IS NULL OR p.emailAddress = '')) 
               OR (p.preferredChannel = 'SMS' AND (p.phoneNumber IS NULL OR p.phoneNumber = ''))
           )
           """)
    List<UserNotificationPreference> findUsersWithMissingContactInfo();
    
    /**
     * Check if user exists for validation
     */
    boolean existsByUserId(String userId);
    
    /**
     * Count active users by category interest
     */
    @Query("""
           SELECT COUNT(DISTINCT p.userId) FROM UserNotificationPreference p 
           JOIN p.enabledCategories c 
           WHERE c = :category 
           AND p.notificationsEnabled = true
           """)
    Long countActiveUsersByCategory(@Param("category") TemplateCategory category);
    
    /**
     * Find users created within timeframe for onboarding analytics
     */
    @Query("SELECT p FROM UserNotificationPreference p WHERE p.createdAt >= :fromDate ORDER BY p.createdAt DESC")
    List<UserNotificationPreference> findByCreatedAtAfterOrderByCreatedAtDesc(@Param("fromDate") LocalDateTime fromDate);
}