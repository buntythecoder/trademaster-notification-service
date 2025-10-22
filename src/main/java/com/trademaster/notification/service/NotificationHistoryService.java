package com.trademaster.notification.service;

import com.trademaster.notification.entity.NotificationHistory;
import com.trademaster.notification.entity.NotificationHistory.NotificationStatus;
import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.repository.NotificationHistoryRepository;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Notification History Service
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Repository Pattern - Rule #4
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationHistoryService {
    
    private final NotificationHistoryRepository historyRepository;
    
    /**
     * Create notification history with functional approach
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    @Transactional
    public CompletableFuture<Result<NotificationHistory, String>> createNotificationHistory(
            NotificationRequest request, 
            String correlationId) {
        
        return CompletableFuture
            .supplyAsync(() -> createHistoryRecord(request, correlationId), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleHistoryResult);
    }
    
    private Result<NotificationHistory, String> createHistoryRecord(
            NotificationRequest request, 
            String correlationId) {
        
        return Result.tryExecute(() -> {
            String notificationId = UUID.randomUUID().toString();
            NotificationHistory history = NotificationHistory.fromRequest(request, notificationId, correlationId);
            
            NotificationHistory saved = historyRepository.save(history);
            
            log.info("Notification history created: ID={}, recipient={}, type={}", 
                    saved.getNotificationId(), saved.getRecipient(), saved.getType());
            
            return saved;
        }).match(
            history -> Result.success(history),
            exception -> {
                log.error("Failed to create notification history: {}", exception.getMessage());
                return Result.failure("Failed to create notification history: " + exception.getMessage());
            }
        );
    }
    
    /**
     * Update notification status with functional approach
     */
    @Transactional
    public CompletableFuture<Result<NotificationHistory, String>> updateNotificationStatus(
            String notificationId, 
            NotificationStatus status, 
            String externalMessageId, 
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> updateStatus(notificationId, status, externalMessageId, updatedBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleHistoryResult);
    }
    
    private Result<NotificationHistory, String> updateStatus(
            String notificationId, 
            NotificationStatus status, 
            String externalMessageId, 
            String updatedBy) {
        
        return historyRepository.findById(notificationId)
            .map(history -> Result.tryExecute(() -> {
                history.updateStatus(status, updatedBy);
                // Rule #3: NO if-else, use Optional for conditional assignment
                Optional.ofNullable(externalMessageId).ifPresent(history::setExternalMessageId);
                return historyRepository.save(history);
            }).mapError(exception -> "Failed to update status: " + exception.getMessage()))
            .orElse(Result.<NotificationHistory, String>failure("Notification not found: " + notificationId));
    }
    
    /**
     * Mark notification as failed with functional approach
     */
    @Transactional
    public CompletableFuture<Result<NotificationHistory, String>> markNotificationFailed(
            String notificationId, 
            String errorMessage, 
            String updatedBy) {
        
        return CompletableFuture
            .supplyAsync(() -> markFailed(notificationId, errorMessage, updatedBy), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleHistoryResult);
    }
    
    private Result<NotificationHistory, String> markFailed(
            String notificationId, 
            String errorMessage, 
            String updatedBy) {
        
        return historyRepository.findById(notificationId)
            .map(history -> Result.tryExecute(() -> {
                history.markAsFailed(errorMessage, updatedBy);
                return historyRepository.save(history);
            }).mapError(exception -> "Failed to mark as failed: " + exception.getMessage()))
            .orElse(Result.<NotificationHistory, String>failure("Notification not found: " + notificationId));
    }
    
    /**
     * Get user notification history with pagination
     */
    public CompletableFuture<Page<NotificationHistory>> getUserNotificationHistory(
            String recipient,
            Pageable pageable) {

        return CompletableFuture
            .supplyAsync(() -> historyRepository.findByRecipientOrderByCreatedAtDesc(recipient, pageable),
                        Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Get user notifications with optional type and status filters
     *
     * MANDATORY: Rule #1 - Java 24 Virtual Threads for async operations
     * MANDATORY: Rule #12 - CompletableFuture with virtual thread executors
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 1
     *
     * @param userId User ID to filter by
     * @param type Optional notification type filter (null = all types)
     * @param status Optional notification status filter (null = all statuses)
     * @param pageable Pagination parameters
     * @return CompletableFuture containing page of filtered notification history
     */
    public CompletableFuture<Page<NotificationHistory>> getUserNotifications(
            String userId,
            NotificationRequest.NotificationType type,
            NotificationStatus status,
            Pageable pageable) {

        return CompletableFuture
            .supplyAsync(() -> historyRepository.findByRecipientWithOptionalFilters(
                    userId, type, status, pageable),
                        Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Get notifications eligible for retry
     */
    public CompletableFuture<List<NotificationHistory>> getNotificationsEligibleForRetry(
            LocalDateTime cutoffTime) {
        
        return CompletableFuture
            .supplyAsync(() -> historyRepository.findEligibleForRetry(cutoffTime), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get delivery statistics with functional composition
     */
    public CompletableFuture<Map<NotificationStatus, Long>> getDeliveryStatistics(
            LocalDateTime startDate, 
            LocalDateTime endDate) {
        
        return CompletableFuture
            .supplyAsync(() -> getStatistics(startDate, endDate), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    private Map<NotificationStatus, Long> getStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        return historyRepository.getDeliveryStatistics(startDate, endDate)
            .stream()
            .collect(Collectors.toMap(
                row -> (NotificationStatus) row[0],
                row -> (Long) row[1],
                Long::sum
            ));
    }
    
    /**
     * Find notifications by correlation ID
     */
    public CompletableFuture<Result<List<NotificationHistory>, String>> findByCorrelationId(
            String correlationId) {
        
        return CompletableFuture
            .supplyAsync(() -> findByCorrelation(correlationId), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleListResult);
    }
    
    private Result<List<NotificationHistory>, String> findByCorrelation(String correlationId) {
        return historyRepository.findByCorrelationIdOrderByCreatedAtDesc(correlationId)
            .map(histories -> Result.<List<NotificationHistory>, String>success(histories))
            .orElse(Result.<List<NotificationHistory>, String>failure("No notifications found for correlation ID: " + correlationId));
    }
    
    /**
     * Increment retry count with functional approach
     */
    @Transactional
    public CompletableFuture<Result<NotificationHistory, String>> incrementRetryCount(
            String notificationId) {
        
        return CompletableFuture
            .supplyAsync(() -> incrementRetry(notificationId), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleHistoryResult);
    }
    
    private Result<NotificationHistory, String> incrementRetry(String notificationId) {
        return historyRepository.findById(notificationId)
            .map(history -> Result.tryExecute(() -> {
                history.incrementRetry();
                return historyRepository.save(history);
            }).mapError(exception -> "Failed to increment retry: " + exception.getMessage()))
            .orElse(Result.<NotificationHistory, String>failure("Notification not found: " + notificationId));
    }
    
    /**
     * Get failed notifications with error patterns
     */
    public CompletableFuture<List<NotificationHistory>> getFailedNotificationsWithPattern(
            String errorPattern, 
            LocalDateTime fromDate) {
        
        return CompletableFuture
            .supplyAsync(() -> historyRepository.findFailedNotificationsWithErrorPattern(errorPattern, fromDate), 
                        Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Generic result handler for history operations
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 1
     */
    private Result<NotificationHistory, String> handleHistoryResult(
            Result<NotificationHistory, String> result,
            Throwable throwable) {

        return Optional.ofNullable(throwable)
            .map(error -> {
                log.error("Notification history operation error", error);
                return Result.<NotificationHistory, String>failure("Operation failed: " + error.getMessage());
            })
            .orElse(result);
    }
    
    /**
     * Generic result handler for list operations
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 1
     */
    private Result<List<NotificationHistory>, String> handleListResult(
            Result<List<NotificationHistory>, String> result,
            Throwable throwable) {

        return Optional.ofNullable(throwable)
            .map(error -> {
                log.error("Notification history list operation error", error);
                return Result.<List<NotificationHistory>, String>failure("Operation failed: " + error.getMessage());
            })
            .orElse(result);
    }
    
    /**
     * Check notification can retry
     */
    public boolean canNotificationRetry(String notificationId) {
        return historyRepository.findById(notificationId)
            .map(NotificationHistory::canRetry)
            .orElse(false);
    }
}