package com.trademaster.notification.service;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.dto.BulkNotificationRequest;
import com.trademaster.notification.constant.NotificationConstants;
import com.trademaster.notification.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Main Notification Service - Orchestrates all notification types
 * 
 * MANDATORY: Factory Pattern - Rule #4
 * MANDATORY: Strategy Pattern - Rule #4
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final PushNotificationService pushService;
    private final InAppNotificationService inAppService;
    private final RateLimitService rateLimitService;
    private final NotificationStore notificationStore;
    
    /**
     * Send notification using strategy pattern and virtual threads
     * 
     * MANDATORY: Strategy Pattern - Rule #4
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Pattern Matching - Rule #14
     */
    public CompletableFuture<NotificationResponse> sendNotification(NotificationRequest request) {
        return CompletableFuture
            .supplyAsync(() -> {
                log.info("Processing notification: type={}, recipient={}", 
                        request.type(), request.recipient());
                
                return switch (request.type()) {
                    case EMAIL -> processEmailNotification(request);
                    case SMS -> processSmsNotification(request);
                    case PUSH -> processPushNotification(request);
                    case IN_APP -> processInAppNotification(request);
                };
            }, Executors.newVirtualThreadPerTaskExecutor())
            .thenCompose(futureResponse -> futureResponse)
            .thenApply(this::storeNotificationResult);
    }
    
    /**
     * Send bulk notifications with rate limiting
     * 
     * MANDATORY: Stream API - Rule #13
     * MANDATORY: Virtual Threads - Rule #12
     */
    public CompletableFuture<List<NotificationResponse>> sendBulkNotifications(BulkNotificationRequest bulkRequest) {
        log.info("Processing bulk notifications: type={}, count={}", 
                bulkRequest.type(), bulkRequest.recipients().size());
        
        return CompletableFuture
            .supplyAsync(() -> {
                // Apply rate limiting
                if (!rateLimitService.isAllowed(bulkRequest.type().toString(), bulkRequest.recipients().size())) {
                    return List.of(NotificationResponse.failure(
                        UUID.randomUUID().toString(), 
                        NotificationConstants.RATE_LIMIT_EXCEEDED
                    ));
                }
                
                // Convert bulk request to individual requests
                List<CompletableFuture<NotificationResponse>> futures = bulkRequest.recipients()
                    .stream()
                    .map(recipient -> createIndividualRequest(bulkRequest, recipient))
                    .map(this::sendNotification)
                    .toList();
                
                // Wait for all notifications to complete
                return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
                    
            }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Get notification status
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    public Optional<NotificationResponse> getNotificationStatus(String notificationId) {
        return notificationStore.findById(notificationId);
    }
    
    private CompletableFuture<NotificationResponse> processEmailNotification(NotificationRequest request) {
        if (!rateLimitService.isAllowed("EMAIL", 1)) {
            return CompletableFuture.completedFuture(
                NotificationResponse.failure(UUID.randomUUID().toString(), 
                NotificationConstants.RATE_LIMIT_EXCEEDED)
            );
        }
        
        return emailService.sendEmail(request);
    }
    
    private CompletableFuture<NotificationResponse> processSmsNotification(NotificationRequest request) {
        if (!rateLimitService.isAllowed("SMS", 1)) {
            return CompletableFuture.completedFuture(
                NotificationResponse.failure(UUID.randomUUID().toString(), 
                NotificationConstants.RATE_LIMIT_EXCEEDED)
            );
        }
        
        return smsService.sendSms(request);
    }
    
    private CompletableFuture<NotificationResponse> processPushNotification(NotificationRequest request) {
        return Optional.of(rateLimitService.isAllowed("PUSH", 1))
            .filter(allowed -> allowed)
            .map(allowed -> pushService.sendPush(request))
            .orElseGet(() -> CompletableFuture.completedFuture(
                NotificationResponse.failure(UUID.randomUUID().toString(), 
                NotificationConstants.RATE_LIMIT_EXCEEDED)
            ));
    }
    
    private CompletableFuture<NotificationResponse> processInAppNotification(NotificationRequest request) {
        return Optional.of(rateLimitService.isAllowed("IN_APP", 1))
            .filter(allowed -> allowed)
            .map(allowed -> inAppService.sendInApp(request))
            .orElseGet(() -> CompletableFuture.completedFuture(
                NotificationResponse.failure(UUID.randomUUID().toString(), 
                NotificationConstants.RATE_LIMIT_EXCEEDED)
            ));
    }
    
    private NotificationRequest createIndividualRequest(BulkNotificationRequest bulkRequest, String recipient) {
        return new NotificationRequest(
            bulkRequest.type(),
            recipient,
            bulkRequest.type() == NotificationRequest.NotificationType.EMAIL ? recipient : null,
            bulkRequest.type() == NotificationRequest.NotificationType.SMS ? recipient : null,
            bulkRequest.subject(),
            bulkRequest.content(),
            bulkRequest.templateName(),
            bulkRequest.templateVariables(),
            bulkRequest.priority(),
            null, // scheduledAt
            null, // referenceId
            bulkRequest.referenceType(),
            null  // maxRetryAttempts
        );
    }
    
    private NotificationResponse storeNotificationResult(NotificationResponse response) {
        return notificationStore.store(response);
    }
    
    /**
     * Secure notification sending method for external API access
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     * MANDATORY: Result Types - Rule #11
     */
    public CompletableFuture<Result<NotificationResponse, Exception>> sendNotificationSecure(NotificationRequest request) {
        return sendNotification(request)
            .thenApply(response -> Result.<NotificationResponse, Exception>success(response))
            .exceptionally(throwable -> Result.failure((Exception) throwable));
    }
    
    /**
     * Secure bulk notification sending method for external API access
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     * MANDATORY: Result Types - Rule #11
     */
    public CompletableFuture<Result<List<NotificationResponse>, Exception>> sendBulkNotificationsSecure(BulkNotificationRequest request) {
        return sendBulkNotifications(request)
            .thenApply(responses -> Result.<List<NotificationResponse>, Exception>success(responses))
            .exceptionally(throwable -> Result.failure((Exception) throwable));
    }
}