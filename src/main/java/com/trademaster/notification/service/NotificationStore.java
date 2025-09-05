package com.trademaster.notification.service;

import com.trademaster.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Immutable Notification Storage Service
 * 
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Immutability - Rule #9
 * MANDATORY: Single Responsibility - Rule #2
 */
@Service
@Slf4j
public class NotificationStore {
    
    private final ConcurrentMap<String, NotificationResponse> storage = new ConcurrentHashMap<>();
    
    /**
     * Store notification response immutably
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    public NotificationResponse store(NotificationResponse response) {
        return Optional.ofNullable(response.notificationId())
            .map(id -> {
                storage.put(id, response);
                log.debug("Stored notification: {}", id);
                return response;
            })
            .orElse(response);
    }
    
    /**
     * Retrieve notification by ID
     * 
     * MANDATORY: Optional Usage - Rule #11
     */
    public Optional<NotificationResponse> findById(String notificationId) {
        return Optional.ofNullable(storage.get(notificationId));
    }
    
    /**
     * Check if notification exists
     */
    public boolean exists(String notificationId) {
        return storage.containsKey(notificationId);
    }
    
    /**
     * Get total stored notifications count
     */
    public int size() {
        return storage.size();
    }
    
    /**
     * Clear all stored notifications (for cleanup)
     */
    public void clear() {
        storage.clear();
        log.info("Notification storage cleared");
    }
}