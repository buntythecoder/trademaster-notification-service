package com.trademaster.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.service.UserNotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * WebSocket Handler for Real-Time Notifications
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Error Handling - Rule #11
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler implements WebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final UserNotificationPreferenceService preferenceService;
    
    // Active WebSocket sessions by user ID
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // Admin sessions for monitoring
    private final ConcurrentHashMap<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();
    
    /**
     * Handle new WebSocket connection
     *
     * MANDATORY: Rule #12 - Virtual Threads
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #14 - Pattern Matching with switch expressions
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        String userRole = extractUserRole(session);

        // Rule #3: NO if-else, use Optional.orElseThrow() for validation
        java.util.Optional.ofNullable(userId)
            .orElseThrow(() -> {
                log.warn("WebSocket connection without user ID, closing session: {}", session.getId());
                try {
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("User ID required"));
                } catch (IOException e) {
                    log.error("Failed to close session: {}", session.getId(), e);
                }
                return new IllegalStateException("User ID required");
            });

        // Store session based on role using switch expression
        String role = userRole != null ? userRole : "USER";
        switch (role) {
            case "ADMIN", "NOTIFICATION_MANAGER" -> {
                adminSessions.put(session.getId(), session);
                log.info("Admin WebSocket session established: userId={}, sessionId={}", userId, session.getId());
            }
            default -> {
                activeSessions.put(userId, session);
                log.info("User WebSocket session established: userId={}, sessionId={}", userId, session.getId());
            }
        }

        // Send welcome message asynchronously
        CompletableFuture
            .runAsync(() -> sendWelcomeMessage(session, userId),
                     Executors.newVirtualThreadPerTaskExecutor())
            .exceptionally(throwable -> {
                log.error("Failed to send welcome message to user: {}", userId, throwable);
                return null;
            });
    }
    
    /**
     * Handle incoming WebSocket messages
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 3
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String userId = extractUserId(session);

        // Rule #3: NO if-else, use Optional with instanceof check and pattern matching
        java.util.Optional.of(message)
            .filter(TextMessage.class::isInstance)
            .map(TextMessage.class::cast)
            .ifPresent(textMessage -> {
                String payload = textMessage.getPayload();
                log.debug("Received WebSocket message from user {}: {}", userId, payload);

                // Handle different message types
                CompletableFuture
                    .runAsync(() -> processIncomingMessage(session, userId, payload),
                             Executors.newVirtualThreadPerTaskExecutor())
                    .exceptionally(throwable -> {
                        log.error("Error processing WebSocket message from user: {}", userId, throwable);
                        return null;
                    });
            });
    }
    
    /**
     * Handle WebSocket transport errors
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = extractUserId(session);
        log.error("WebSocket transport error for user: {}, sessionId: {}", userId, session.getId(), exception);
        
        // Clean up session
        removeSession(session);
    }
    
    /**
     * Handle WebSocket connection close
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = extractUserId(session);
        log.info("WebSocket connection closed for user: {}, sessionId: {}, status: {}", 
                userId, session.getId(), closeStatus);
        
        // Clean up session
        removeSession(session);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Send real-time notification to specific user
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Functional Programming - Rule #3
     */
    public CompletableFuture<Boolean> sendNotificationToUser(String userId, NotificationResponse notification) {
        return CompletableFuture
            .supplyAsync(() -> performUserNotificationSend(userId, notification), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .exceptionally(throwable -> {
                log.error("Failed to send notification to user via WebSocket: {}", userId, throwable);
                return false;
            });
    }
    
    /**
     * Broadcast notification to all admin sessions
     */
    public CompletableFuture<Integer> broadcastToAdmins(Map<String, Object> adminNotification) {
        return CompletableFuture
            .supplyAsync(() -> performAdminBroadcast(adminNotification), 
                        Executors.newVirtualThreadPerTaskExecutor())
            .exceptionally(throwable -> {
                log.error("Failed to broadcast to admin sessions", throwable);
                return 0;
            });
    }
    
    /**
     * Get count of active user sessions
     */
    public int getActiveUserSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Get count of active admin sessions
     */
    public int getActiveAdminSessionCount() {
        return adminSessions.size();
    }
    
    /**
     * Check if user has active WebSocket session
     */
    public boolean isUserConnected(String userId) {
        WebSocketSession session = activeSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    // Private helper methods
    
    /**
     * Perform user notification send
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    private boolean performUserNotificationSend(String userId, NotificationResponse notification) {
        WebSocketSession session = activeSessions.get(userId);

        // Rule #3: NO if-else, use Optional.filter().map().orElse()
        return java.util.Optional.ofNullable(session)
            .filter(WebSocketSession::isOpen)
            .map(openSession -> {
                try {
                    WebSocketMessage message = createNotificationMessage(notification);
                    openSession.sendMessage(message);

                    log.debug("Real-time notification sent to user: {}, notificationId: {}",
                             userId, notification.notificationId());
                    return true;

                } catch (IOException e) {
                    log.error("Failed to send WebSocket message to user: {}", userId, e);
                    // Remove broken session
                    activeSessions.remove(userId);
                    return false;
                }
            })
            .orElseGet(() -> {
                log.debug("No active WebSocket session for user: {}", userId);
                return false;
            });
    }
    
    /**
     * Perform admin broadcast
     *
     * MANDATORY: Rule #13 - Stream API for collection processing (NO for-loop)
     * MANDATORY: Rule #3 - Functional Programming (NO if-else)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 3
     */
    private int performAdminBroadcast(Map<String, Object> adminNotification) {
        // Rule #13: NO for-loop, use Stream API with filter and count
        long sentCount = adminSessions.values().stream()
            .filter(WebSocketSession::isOpen)
            .filter(session -> sendAdminMessage(session, adminNotification))
            .count();

        log.debug("Admin broadcast sent to {} sessions", sentCount);
        return (int) sentCount;
    }

    /**
     * Send admin message to single session
     */
    private boolean sendAdminMessage(WebSocketSession session, Map<String, Object> adminNotification) {
        try {
            WebSocketMessage message = createAdminMessage(adminNotification);
            session.sendMessage(message);
            return true;
        } catch (IOException e) {
            log.error("Failed to send admin broadcast to session: {}", session.getId(), e);
            adminSessions.remove(session.getId());
            return false;
        }
    }
    
    private void processIncomingMessage(WebSocketSession session, String userId, String payload) {
        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String messageType = (String) messageData.get("type");
            
            switch (messageType) {
                case "ping" -> handlePingMessage(session, userId);
                case "mark_read" -> handleMarkReadMessage(session, userId, messageData);
                case "preference_update" -> handlePreferenceUpdateMessage(session, userId, messageData);
                default -> log.warn("Unknown message type from user {}: {}", userId, messageType);
            }
            
        } catch (Exception e) {
            log.error("Failed to process incoming WebSocket message from user: {}", userId, e);
        }
    }
    
    private void handlePingMessage(WebSocketSession session, String userId) {
        try {
            Map<String, Object> pongMessage = Map.of(
                "type", "pong",
                "timestamp", LocalDateTime.now(),
                "userId", userId
            );
            
            WebSocketMessage message = new TextMessage(objectMapper.writeValueAsString(pongMessage));
            session.sendMessage(message);
            
        } catch (Exception e) {
            log.error("Failed to handle ping message from user: {}", userId, e);
        }
    }
    
    /**
     * Handle mark read message
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2
     */
    private void handleMarkReadMessage(WebSocketSession session, String userId, Map<String, Object> messageData) {
        String notificationId = (String) messageData.get("notificationId");

        // Rule #3: NO if-else, use Optional.ifPresent()
        java.util.Optional.ofNullable(notificationId)
            .ifPresent(id -> {
                log.debug("User {} marked notification as read: {}", userId, id);
                // Here you would update the notification status in the database
            });
    }
    
    private void handlePreferenceUpdateMessage(WebSocketSession session, String userId, Map<String, Object> messageData) {
        log.debug("User {} updated preferences via WebSocket: {}", userId, messageData);
        // Here you would handle real-time preference updates
    }
    
    private void sendWelcomeMessage(WebSocketSession session, String userId) {
        try {
            Map<String, Object> welcomeMessage = Map.of(
                "type", "welcome",
                "message", "Connected to TradeMaster notifications",
                "userId", userId,
                "timestamp", LocalDateTime.now()
            );
            
            WebSocketMessage message = new TextMessage(objectMapper.writeValueAsString(welcomeMessage));
            session.sendMessage(message);
            
        } catch (Exception e) {
            log.error("Failed to send welcome message to user: {}", userId, e);
        }
    }
    
    private WebSocketMessage createNotificationMessage(NotificationResponse notification) throws IOException {
        Map<String, Object> messageData = Map.of(
            "type", "notification",
            "data", notification,
            "timestamp", LocalDateTime.now()
        );
        
        return new TextMessage(objectMapper.writeValueAsString(messageData));
    }
    
    private WebSocketMessage createAdminMessage(Map<String, Object> adminNotification) throws IOException {
        Map<String, Object> messageData = Map.of(
            "type", "admin_notification",
            "data", adminNotification,
            "timestamp", LocalDateTime.now()
        );
        
        return new TextMessage(objectMapper.writeValueAsString(messageData));
    }
    
    private String extractUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }
    
    private String extractUserRole(WebSocketSession session) {
        return (String) session.getAttributes().get("userRole");
    }
    
    private void removeSession(WebSocketSession session) {
        String userId = extractUserId(session);
        activeSessions.remove(userId);
        adminSessions.remove(session.getId());
    }
}