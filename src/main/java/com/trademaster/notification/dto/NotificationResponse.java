package com.trademaster.notification.dto;

import java.time.LocalDateTime;

/**
 * Notification Response DTO
 * 
 * MANDATORY: Immutability & Records - Rule #9
 * MANDATORY: Functional Programming - Rule #3
 */
public record NotificationResponse(
    String notificationId,
    NotificationStatus status,
    String message,
    LocalDateTime sentAt,
    String deliveryId,
    boolean success
) {
    
    // Factory methods for different outcomes
    public static NotificationResponse success(String notificationId, String deliveryId) {
        return new NotificationResponse(
            notificationId,
            NotificationStatus.SENT,
            "Notification sent successfully",
            LocalDateTime.now(),
            deliveryId,
            true
        );
    }
    
    public static NotificationResponse failure(String notificationId, String errorMessage) {
        return new NotificationResponse(
            notificationId,
            NotificationStatus.FAILED,
            errorMessage,
            LocalDateTime.now(),
            null,
            false
        );
    }
    
    public static NotificationResponse queued(String notificationId) {
        return new NotificationResponse(
            notificationId,
            NotificationStatus.PENDING,
            "Notification queued for processing",
            LocalDateTime.now(),
            null,
            true
        );
    }
    
    public enum NotificationStatus {
        PENDING,
        PROCESSING,
        SENT,
        DELIVERED,
        FAILED,
        CANCELLED
    }
}