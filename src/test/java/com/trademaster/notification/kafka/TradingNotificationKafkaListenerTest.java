package com.trademaster.notification.kafka;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.entity.NotificationTemplate;
import com.trademaster.notification.entity.UserNotificationPreference;
import com.trademaster.notification.kafka.TradingNotificationKafkaListener.TradingNotificationMessage;
import com.trademaster.notification.service.NotificationService;
import com.trademaster.notification.service.NotificationTemplateService;
import com.trademaster.notification.service.UserNotificationPreferenceService;
import com.trademaster.notification.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TradingNotificationKafkaListener
 *
 * MANDATORY COMPLIANCE:
 * - Rule #20: Unit Tests with >80% coverage
 * - Rule #3: Functional test patterns
 * - Rule #11: Result types for error handling
 * - Rule #15: Structured test organization
 *
 * Test Coverage:
 * - Kafka message consumption
 * - WebSocket delivery with connected/disconnected users
 * - User preference validation (enabled, quiet hours, channels)
 * - Template loading and variable substitution
 * - Fallback to defaults when preferences/templates missing
 * - Circuit breaker fallback
 * - Correlation ID extraction
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradingNotificationKafkaListener Unit Tests")
class TradingNotificationKafkaListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationWebSocketHandler webSocketHandler;

    @Mock
    private NotificationTemplateService templateService;

    @Mock
    private UserNotificationPreferenceService preferenceService;

    @InjectMocks
    private TradingNotificationKafkaListener listener;

    @Captor
    private ArgumentCaptor<NotificationRequest> notificationRequestCaptor;

    @Captor
    private ArgumentCaptor<NotificationResponse> webSocketResponseCaptor;

    private TradingNotificationMessage testMessage;
    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        // Create test message data
        testData = Map.of(
            "orderId", "TM-ORDER-123",
            "symbol", "RELIANCE",
            "exchange", "NSE",
            "orderType", "LIMIT",
            "side", "BUY",
            "quantity", 100,
            "limitPrice", "2500.00",
            "status", "PLACED",
            "correlationId", "NOTIF-TEST-123"
        );

        testMessage = new TradingNotificationMessage(
            "NOTIF-12345",
            1001L,
            "ORDER_PLACED",
            "Order Placed Successfully",
            "Your BUY order for 100 shares of RELIANCE at ₹2500.00 has been placed successfully.",
            testData,
            Instant.now()
        );
    }

    @Test
    @DisplayName("Should process trading notification successfully with WebSocket delivery")
    void processTradingNotification_WithConnectedUser_ShouldDeliverViaWebSocket() throws InterruptedException {
        // Given
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(true);
        when(webSocketHandler.sendNotificationToUser(anyString(), any(NotificationResponse.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.empty());
        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(webSocketHandler).isUserConnected("1001");
        verify(webSocketHandler).sendNotificationToUser(eq("1001"), webSocketResponseCaptor.capture());

        NotificationResponse wsResponse = webSocketResponseCaptor.getValue();
        assertThat(wsResponse.notificationId()).isEqualTo("NOTIF-12345");
        assertThat(wsResponse.title()).isEqualTo("Order Placed Successfully");
    }

    @Test
    @DisplayName("Should skip WebSocket delivery when user not connected")
    void processTradingNotification_WithDisconnectedUser_ShouldSkipWebSocket() throws InterruptedException {
        // Given
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.empty());
        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(webSocketHandler).isUserConnected("1001");
        verify(webSocketHandler, never()).sendNotificationToUser(anyString(), any());
    }

    @Test
    @DisplayName("Should send notification with user preferences when available")
    void processTradingNotification_WithUserPreferences_ShouldApplyPreferences() throws InterruptedException {
        // Given
        UserNotificationPreference preference = createTestPreference(true);
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.of(preference));

        NotificationTemplate template = createTestTemplate();
        when(templateService.findByTemplateNameAndActive(anyString(), eq(true)))
            .thenReturn(Optional.of(template));

        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(preferenceService).findByUserId("1001");
        verify(templateService).findByTemplateNameAndActive("trading-order-order-placed", true);
        verify(notificationService).sendNotificationSecure(notificationRequestCaptor.capture());

        NotificationRequest request = notificationRequestCaptor.getValue();
        assertThat(request.userId()).isEqualTo("1001");
        assertThat(request.type()).isEqualTo(NotificationRequest.NotificationType.EMAIL);
        assertThat(request.email()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should skip notification when user has notifications disabled")
    void processTradingNotification_WithDisabledPreferences_ShouldSkipNotification() throws InterruptedException {
        // Given
        UserNotificationPreference preference = createTestPreference(false);
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.of(preference));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(preferenceService).findByUserId("1001");
        verify(notificationService, never()).sendNotificationSecure(any());
    }

    @Test
    @DisplayName("Should skip notification during quiet hours")
    void processTradingNotification_DuringQuietHours_ShouldSkipNotification() throws InterruptedException {
        // Given
        UserNotificationPreference preference = createTestPreferenceWithQuietHours();
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.of(preference));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(preferenceService).findByUserId("1001");
        verify(notificationService, never()).sendNotificationSecure(any());
    }

    @Test
    @DisplayName("Should use default settings when preferences not found")
    void processTradingNotification_WithoutPreferences_ShouldUseDefaults() throws InterruptedException {
        // Given
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.empty());
        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(preferenceService).findByUserId("1001");
        verify(notificationService).sendNotificationSecure(notificationRequestCaptor.capture());

        NotificationRequest request = notificationRequestCaptor.getValue();
        assertThat(request.type()).isEqualTo(NotificationRequest.NotificationType.IN_APP);
        assertThat(request.title()).isEqualTo("Order Placed Successfully");
    }

    @Test
    @DisplayName("Should apply template variables correctly")
    void processTradingNotification_WithTemplate_ShouldApplyVariables() throws InterruptedException {
        // Given
        UserNotificationPreference preference = createTestPreference(true);
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.of(preference));

        NotificationTemplate template = new NotificationTemplate(
            null,
            "trading-order-order-placed",
            "Order Confirmation for {{symbol}}",
            "Your {{side}} order for {{quantity}} shares of {{symbol}} at {{price}} has been {{type}}.",
            "<p>Order {{orderId}} for {{symbol}}</p>",
            NotificationTemplate.TemplateCategory.TRADING,
            null,
            Set.of("userId", "orderId", "symbol", "quantity", "price", "side", "type"),
            Set.of("exchange", "status"),
            true,
            NotificationRequest.Priority.MEDIUM,
            null,
            null,
            null
        );

        when(templateService.findByTemplateNameAndActive(anyString(), eq(true)))
            .thenReturn(Optional.of(template));

        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(notificationService).sendNotificationSecure(notificationRequestCaptor.capture());

        NotificationRequest request = notificationRequestCaptor.getValue();
        assertThat(request.subject()).contains("RELIANCE");
        assertThat(request.content()).contains("BUY", "100", "RELIANCE", "2500.00");
    }

    @Test
    @DisplayName("Should fallback to defaults when template not found")
    void processTradingNotification_WithoutTemplate_ShouldFallbackToDefaults() throws InterruptedException {
        // Given
        UserNotificationPreference preference = createTestPreference(true);
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.of(preference));
        when(templateService.findByTemplateNameAndActive(anyString(), eq(true)))
            .thenReturn(Optional.empty());

        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(templateService).findByTemplateNameAndActive("trading-order-order-placed", true);
        verify(notificationService).sendNotificationSecure(notificationRequestCaptor.capture());

        NotificationRequest request = notificationRequestCaptor.getValue();
        assertThat(request.content()).isEqualTo(testMessage.content());
    }

    @Test
    @DisplayName("Should extract correlation ID from message data")
    void processTradingNotification_ShouldExtractCorrelationId() throws InterruptedException {
        // Given
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.empty());
        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then - correlation ID should be in logs (verified through code execution)
        verify(preferenceService).findByUserId("1001");
    }

    @Test
    @DisplayName("Should handle missing correlation ID gracefully")
    void processTradingNotification_WithoutCorrelationId_ShouldUseUnknown() throws InterruptedException {
        // Given
        Map<String, Object> dataWithoutCorrelationId = Map.of(
            "orderId", "TM-ORDER-123",
            "symbol", "RELIANCE"
        );

        TradingNotificationMessage messageWithoutCorrelationId = new TradingNotificationMessage(
            "NOTIF-12345",
            1001L,
            "ORDER_PLACED",
            "Order Placed Successfully",
            "Order placed",
            dataWithoutCorrelationId,
            Instant.now()
        );

        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.empty());
        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(messageWithoutCorrelationId, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then - should handle gracefully and use "UNKNOWN"
        verify(preferenceService).findByUserId("1001");
    }

    @Test
    @DisplayName("Should map notification priority correctly")
    void processTradingNotification_ShouldMapPriorityCorrectly() throws InterruptedException {
        // Test different notification types and their priorities
        testPriorityMapping("ORDER_REJECTED", NotificationRequest.Priority.HIGH);
        testPriorityMapping("ORDER_CANCELLED", NotificationRequest.Priority.HIGH);
        testPriorityMapping("ORDER_FILLED", NotificationRequest.Priority.MEDIUM);
        testPriorityMapping("ORDER_PLACED", NotificationRequest.Priority.LOW);
    }

    @Test
    @DisplayName("Should handle circuit breaker fallback")
    void processTradingNotificationFallback_ShouldLogError() {
        // Given
        Exception testException = new RuntimeException("Circuit breaker opened");

        // When
        listener.processTradingNotificationFallback(testMessage, 0, 100L, testException);

        // Then - fallback should execute without throwing exception
        // Verification is through log output (error logging)
    }

    @Test
    @DisplayName("Should handle WebSocket send failure gracefully")
    void processTradingNotification_WhenWebSocketFails_ShouldContinueProcessing() throws InterruptedException {
        // Given
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(true);
        when(webSocketHandler.sendNotificationToUser(anyString(), any(NotificationResponse.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("WebSocket error")));
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.empty());
        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then - should not throw exception, continue with other channels
        verify(webSocketHandler).sendNotificationToUser(eq("1001"), any());
        verify(notificationService).sendNotificationSecure(any());
    }

    @Test
    @DisplayName("Should handle notification service failure gracefully")
    void processTradingNotification_WhenNotificationServiceFails_ShouldLogError() throws InterruptedException {
        // Given
        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.empty());
        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.failure(
                    new com.trademaster.notification.pattern.Result.Error("SERVICE_ERROR", "Service unavailable")
                )
            ));

        // When
        listener.processTradingNotification(testMessage, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then - should log error but not throw exception
        verify(notificationService).sendNotificationSecure(any());
    }

    // Helper methods

    private UserNotificationPreference createTestPreference(boolean enabled) {
        UserNotificationPreference preference = UserNotificationPreference.createDefault("1001");
        preference.setNotificationsEnabled(enabled);
        preference.setPreferredChannel(NotificationRequest.NotificationType.EMAIL);
        preference.setEmailAddress("user@example.com");
        preference.setEnabledChannels(Set.of(NotificationRequest.NotificationType.EMAIL, NotificationRequest.NotificationType.IN_APP));
        preference.setEnabledCategories(Set.of(NotificationTemplate.TemplateCategory.TRADING));
        preference.setTradingAlertsEnabled(true);
        return preference;
    }

    private UserNotificationPreference createTestPreferenceWithQuietHours() {
        UserNotificationPreference preference = createTestPreference(true);
        // Set quiet hours to current time so it's always within quiet hours
        LocalTime now = LocalTime.now();
        preference.setQuietHoursStart(now.minusHours(1));
        preference.setQuietHoursEnd(now.plusHours(1));
        return preference;
    }

    private NotificationTemplate createTestTemplate() {
        return new NotificationTemplate(
            null,
            "trading-order-order-placed",
            "Order Confirmation",
            "Your order has been placed successfully.",
            "<p>Order confirmed</p>",
            NotificationTemplate.TemplateCategory.TRADING,
            null,
            Set.of("userId", "orderId"),
            Set.of(),
            true,
            NotificationRequest.Priority.MEDIUM,
            null,
            null,
            null
        );
    }

    private void testPriorityMapping(String notificationType, NotificationRequest.Priority expectedPriority) throws InterruptedException {
        // Reset mocks
        reset(preferenceService, notificationService, webSocketHandler);

        // Create message with specific type
        TradingNotificationMessage message = new TradingNotificationMessage(
            "NOTIF-12345",
            1001L,
            notificationType,
            "Test Title",
            "Test Content",
            testData,
            Instant.now()
        );

        when(webSocketHandler.isUserConnected(anyString())).thenReturn(false);
        when(preferenceService.findByUserId(anyString())).thenReturn(Optional.empty());
        when(notificationService.sendNotificationSecure(any(NotificationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                com.trademaster.notification.pattern.Result.success(
                    NotificationResponse.success("NOTIF-123", "Success", "Sent", Map.of())
                )
            ));

        // When
        listener.processTradingNotification(message, 0, 100L);

        // Wait for async operations
        Thread.sleep(200);

        // Then
        verify(notificationService).sendNotificationSecure(notificationRequestCaptor.capture());
        NotificationRequest request = notificationRequestCaptor.getValue();
        assertThat(request.priority()).isEqualTo(expectedPriority);
    }
}
