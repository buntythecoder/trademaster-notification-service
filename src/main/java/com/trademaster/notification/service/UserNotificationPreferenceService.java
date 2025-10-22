package com.trademaster.notification.service;

import com.trademaster.notification.entity.UserNotificationPreference;
import com.trademaster.notification.entity.NotificationTemplate.TemplateCategory;
import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.repository.UserNotificationPreferenceRepository;
import com.trademaster.notification.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * User Notification Preference Management Service
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: User Preference Management - FRONT-020
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserNotificationPreferenceService {
    
    private final UserNotificationPreferenceRepository preferenceRepository;
    
    /**
     * Create default preferences for new user
     * 
     * MANDATORY: Factory Pattern - Rule #4
     * MANDATORY: Functional Programming - Rule #3
     */
    @Transactional
    public CompletableFuture<Result<UserNotificationPreference, String>> createDefaultPreferences(
            String userId, 
            String createdBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performCreateDefault(userId, createdBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handlePreferenceResult);
    }
    
    private Result<UserNotificationPreference, String> performCreateDefault(String userId, String createdBy) {
        return Result.tryExecute(() -> {
            // Rule #3: NO if-else, use Optional.filter() for validation
            java.util.Optional.of(preferenceRepository.existsByUserId(userId))
                .filter(exists -> !exists)
                .orElseThrow(() -> new IllegalArgumentException("Preferences already exist for user: " + userId));

            UserNotificationPreference preference = UserNotificationPreference.createDefault(userId, createdBy);
            UserNotificationPreference saved = preferenceRepository.save(preference);
            
            log.info("Default preferences created for user: {}", userId);
            
            return saved;
        }).match(
            preference -> Result.success(preference),
            exception -> {
                log.error("Failed to create default preferences for user '{}': {}", userId, exception.getMessage());
                return Result.failure("Failed to create preferences: " + exception.getMessage());
            }
        );
    }
    
    /**
     * Get user preferences
     */
    public CompletableFuture<Optional<UserNotificationPreference>> getUserPreferences(String userId) {
        return CompletableFuture
            .supplyAsync(() -> preferenceRepository.findByUserId(userId), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Update notification preferences
     */
    @Transactional
    public CompletableFuture<Result<UserNotificationPreference, String>> updateNotificationPreferences(
            String userId,
            Boolean notificationsEnabled,
            NotificationRequest.NotificationType preferredChannel,
            Set<NotificationRequest.NotificationType> enabledChannels,
            Set<TemplateCategory> enabledCategories,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performUpdatePreferences(
                userId, notificationsEnabled, preferredChannel, enabledChannels, 
                enabledCategories, updatedBy), 
                Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handlePreferenceResult);
    }
    
    private Result<UserNotificationPreference, String> performUpdatePreferences(
            String userId,
            Boolean notificationsEnabled,
            NotificationRequest.NotificationType preferredChannel,
            Set<NotificationRequest.NotificationType> enabledChannels,
            Set<TemplateCategory> enabledCategories,
            String updatedBy) {
        
        return preferenceRepository.findByUserId(userId)
            .map(preference -> Result.tryExecute(() -> {
                preference.updatePreferences(notificationsEnabled, preferredChannel, enabledChannels, updatedBy);

                // Rule #3: NO if-else, use Optional.ifPresent() for conditional update
                java.util.Optional.ofNullable(enabledCategories).ifPresent(preference::setEnabledCategories);

                UserNotificationPreference updated = preferenceRepository.save(preference);
                
                log.info("Preferences updated for user: {}", userId);
                
                return updated;
            }).mapError(exception -> "Failed to update preferences: " + exception.getMessage()))
            .orElse(Result.<UserNotificationPreference, String>failure("User preferences not found: " + userId));
    }
    
    /**
     * Update contact information
     */
    @Transactional
    public CompletableFuture<Result<UserNotificationPreference, String>> updateContactInformation(
            String userId,
            String emailAddress,
            String phoneNumber,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performUpdateContact(userId, emailAddress, phoneNumber, updatedBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handlePreferenceResult);
    }
    
    private Result<UserNotificationPreference, String> performUpdateContact(
            String userId, 
            String emailAddress, 
            String phoneNumber, 
            String updatedBy) {
        
        return preferenceRepository.findByUserId(userId)
            .map(preference -> Result.tryExecute(() -> {
                // Rule #3: NO if-else, use Optional.ifPresent() for conditional updates
                java.util.Optional.ofNullable(emailAddress).ifPresent(preference::setEmailAddress);
                java.util.Optional.ofNullable(phoneNumber).ifPresent(preference::setPhoneNumber);

                preference.setUpdatedBy(updatedBy);
                
                UserNotificationPreference updated = preferenceRepository.save(preference);
                
                log.info("Contact information updated for user: {}", userId);
                
                return updated;
            }).mapError(exception -> "Failed to update contact info: " + exception.getMessage()))
            .orElse(Result.<UserNotificationPreference, String>failure("User preferences not found: " + userId));
    }
    
    /**
     * Update quiet hours settings
     */
    @Transactional
    public CompletableFuture<Result<UserNotificationPreference, String>> updateQuietHours(
            String userId,
            Boolean quietHoursEnabled,
            LocalTime quietStartTime,
            LocalTime quietEndTime,
            String timeZone,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performUpdateQuietHours(
                userId, quietHoursEnabled, quietStartTime, quietEndTime, timeZone, updatedBy), 
                Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handlePreferenceResult);
    }
    
    private Result<UserNotificationPreference, String> performUpdateQuietHours(
            String userId,
            Boolean quietHoursEnabled,
            LocalTime quietStartTime,
            LocalTime quietEndTime,
            String timeZone,
            String updatedBy) {
        
        return preferenceRepository.findByUserId(userId)
            .map(preference -> Result.tryExecute(() -> {
                // Rule #3: NO if-else, use Optional.ifPresent() for conditional updates
                java.util.Optional.ofNullable(quietHoursEnabled).ifPresent(preference::setQuietHoursEnabled);
                java.util.Optional.ofNullable(quietStartTime).ifPresent(preference::setQuietStartTime);
                java.util.Optional.ofNullable(quietEndTime).ifPresent(preference::setQuietEndTime);
                java.util.Optional.ofNullable(timeZone).ifPresent(preference::setTimeZone);

                preference.setUpdatedBy(updatedBy);
                
                UserNotificationPreference updated = preferenceRepository.save(preference);
                
                log.info("Quiet hours updated for user: {}", userId);
                
                return updated;
            }).mapError(exception -> "Failed to update quiet hours: " + exception.getMessage()))
            .orElse(Result.<UserNotificationPreference, String>failure("User preferences not found: " + userId));
    }
    
    /**
     * Update frequency limits
     */
    @Transactional
    public CompletableFuture<Result<UserNotificationPreference, String>> updateFrequencyLimits(
            String userId,
            Integer frequencyLimitPerHour,
            Integer frequencyLimitPerDay,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performUpdateFrequency(userId, frequencyLimitPerHour, frequencyLimitPerDay, updatedBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handlePreferenceResult);
    }
    
    private Result<UserNotificationPreference, String> performUpdateFrequency(
            String userId, 
            Integer frequencyLimitPerHour, 
            Integer frequencyLimitPerDay, 
            String updatedBy) {
        
        return preferenceRepository.findByUserId(userId)
            .map(preference -> Result.tryExecute(() -> {
                // Rule #3: NO if-else, use Optional.ifPresent() for conditional updates
                java.util.Optional.ofNullable(frequencyLimitPerHour).ifPresent(preference::setFrequencyLimitPerHour);
                java.util.Optional.ofNullable(frequencyLimitPerDay).ifPresent(preference::setFrequencyLimitPerDay);

                preference.setUpdatedBy(updatedBy);
                
                UserNotificationPreference updated = preferenceRepository.save(preference);
                
                log.info("Frequency limits updated for user: {}", userId);
                
                return updated;
            }).mapError(exception -> "Failed to update frequency limits: " + exception.getMessage()))
            .orElse(Result.<UserNotificationPreference, String>failure("User preferences not found: " + userId));
    }
    
    /**
     * Check if notification is allowed for user
     */
    public CompletableFuture<Boolean> isNotificationAllowed(
            String userId,
            NotificationRequest.NotificationType type,
            TemplateCategory category) {
        
        return getUserPreferences(userId)
            .thenApply(optionalPreference -> optionalPreference
                .map(preference -> preference.isNotificationAllowed(type, category))
                .orElse(true)); // Default allow if no preferences found
    }
    
    /**
     * Check if user is in quiet hours
     */
    public CompletableFuture<Boolean> isUserInQuietHours(String userId) {
        return getUserPreferences(userId)
            .thenApply(optionalPreference -> optionalPreference
                .map(UserNotificationPreference::isWithinQuietHours)
                .orElse(false));
    }
    
    /**
     * Get contact info for notification type
     */
    public CompletableFuture<Optional<String>> getContactInfo(
            String userId, 
            NotificationRequest.NotificationType type) {
        
        return getUserPreferences(userId)
            .thenApply(optionalPreference -> optionalPreference
                .map(preference -> preference.getContactInfo(type)));
    }
    
    /**
     * Get users by preferred channel
     */
    public CompletableFuture<List<UserNotificationPreference>> getUsersByPreferredChannel(
            NotificationRequest.NotificationType channel) {
        
        return CompletableFuture
            .supplyAsync(() -> preferenceRepository.findByPreferredChannelAndNotificationsEnabledTrue(channel), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get users by enabled category
     */
    public CompletableFuture<List<UserNotificationPreference>> getUsersByCategory(TemplateCategory category) {
        return CompletableFuture
            .supplyAsync(() -> preferenceRepository.findByEnabledCategoriesContainingAndNotificationsEnabledTrue(category), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get users by language
     */
    public CompletableFuture<List<UserNotificationPreference>> getUsersByLanguage(String language) {
        return CompletableFuture
            .supplyAsync(() -> preferenceRepository.findByLanguageAndNotificationsEnabledTrue(language), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Search preferences with pagination
     */
    public CompletableFuture<Page<UserNotificationPreference>> searchPreferences(
            Boolean enabled,
            NotificationRequest.NotificationType channel,
            String language,
            Pageable pageable) {
        
        return CompletableFuture
            .supplyAsync(() -> preferenceRepository.findPreferencesWithFilters(enabled, channel, language, pageable), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get preference statistics
     */
    public CompletableFuture<Map<NotificationRequest.NotificationType, Long>> getChannelPreferenceStatistics() {
        return CompletableFuture
            .supplyAsync(() -> getChannelStats(), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    private Map<NotificationRequest.NotificationType, Long> getChannelStats() {
        return preferenceRepository.getChannelPreferenceStatistics()
            .stream()
            .collect(Collectors.toMap(
                row -> (NotificationRequest.NotificationType) row[0],
                row -> (Long) row[1]
            ));
    }
    
    /**
     * Get language distribution statistics
     */
    public CompletableFuture<Map<String, Long>> getLanguageDistributionStatistics() {
        return CompletableFuture
            .supplyAsync(() -> getLanguageStats(), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    private Map<String, Long> getLanguageStats() {
        return preferenceRepository.getLanguageDistributionStatistics()
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));
    }
    
    /**
     * Find users with missing contact information
     */
    public CompletableFuture<List<UserNotificationPreference>> findUsersWithMissingContactInfo() {
        return CompletableFuture
            .supplyAsync(() -> preferenceRepository.findUsersWithMissingContactInfo(), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Bulk update notification status
     */
    @Transactional
    public CompletableFuture<Result<Integer, String>> updateNotificationStatusBatch(
            List<String> userIds,
            Boolean enabled,
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> performBatchUpdate(userIds, enabled, updatedBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleIntegerResult);
    }
    
    private Result<Integer, String> performBatchUpdate(List<String> userIds, Boolean enabled, String updatedBy) {
        return Result.tryExecute(() -> {
            int updatedCount = preferenceRepository.updateNotificationStatusBatch(userIds, enabled, updatedBy);
            
            log.info("Batch preference update: {} users, enabled={}", updatedCount, enabled);
            
            return updatedCount;
        }).match(
            count -> Result.success(count),
            exception -> Result.failure("Failed to update preferences: " + exception.getMessage())
        );
    }
    
    /**
     * Get users who opted out recently
     */
    public CompletableFuture<List<UserNotificationPreference>> getUsersOptedOutSince(LocalDateTime fromDate) {
        return CompletableFuture
            .supplyAsync(() -> preferenceRepository.findUsersOptedOutSince(fromDate), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Count active users by category
     */
    public CompletableFuture<Long> countActiveUsersByCategory(TemplateCategory category) {
        return CompletableFuture
            .supplyAsync(() -> preferenceRepository.countActiveUsersByCategory(category), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get or create user preferences
     */
    public CompletableFuture<Result<UserNotificationPreference, String>> getOrCreateUserPreferences(
            String userId, 
            String createdBy) {
        
        return getUserPreferences(userId)
            .thenCompose(optionalPreference -> 
                optionalPreference.isPresent() 
                    ? CompletableFuture.completedFuture(Result.success(optionalPreference.get()))
                    : createDefaultPreferences(userId, createdBy)
            );
    }
    
    /**
     * Generic result handler for preference operations
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #11 - Result Type error handling
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2
     */
    private Result<UserNotificationPreference, String> handlePreferenceResult(
            Result<UserNotificationPreference, String> result,
            Throwable throwable) {

        // Rule #3: NO if-else, use Optional.map() for conditional error handling
        return java.util.Optional.ofNullable(throwable)
            .map(error -> {
                log.error("Preference operation error", error);
                return Result.<UserNotificationPreference, String>failure("Preference operation failed: " + error.getMessage());
            })
            .orElse(result);
    }

    /**
     * Generic result handler for integer operations
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #11 - Result Type error handling
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2
     */
    private Result<Integer, String> handleIntegerResult(
            Result<Integer, String> result,
            Throwable throwable) {

        // Rule #3: NO if-else, use Optional.map() for conditional error handling
        return java.util.Optional.ofNullable(throwable)
            .map(error -> {
                log.error("Preference batch operation error", error);
                return Result.<Integer, String>failure("Batch operation failed: " + error.getMessage());
            })
            .orElse(result);
    }
}