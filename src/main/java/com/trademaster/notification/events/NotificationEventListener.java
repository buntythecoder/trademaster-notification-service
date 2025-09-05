package com.trademaster.notification.events;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.service.NotificationService;
import com.trademaster.notification.constant.NotificationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.Map;

/**
 * Kafka Event Listener for Inter-Service Communication
 * 
 * MANDATORY: Event-Driven Architecture - Rule #4
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * 
 * Listens to events from other TradeMaster services and triggers notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * Handle user registration events from User Profile Service
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Event-Driven Pattern - Rule #4
     */
    @KafkaListener(topics = "user-registration-events", groupId = "notification-service")
    public void handleUserRegistration(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("Received user registration event: topic={}, partition={}, offset={}", 
                topic, partition, offset);
        
        CompletableFuture.runAsync(() -> {
            try {
                String email = (String) event.get("email");
                String firstName = (String) event.get("firstName");
                String lastName = (String) event.get("lastName");
                String userId = (String) event.get("userId");
                
                // Send welcome email
                NotificationRequest welcomeEmail = NotificationRequest.templated(
                    NotificationRequest.NotificationType.EMAIL,
                    email,
                    NotificationConstants.WELCOME_EMAIL_TEMPLATE,
                    Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "dashboardUrl", "https://app.trademaster.com/dashboard",
                        "userId", userId
                    )
                );
                
                notificationService.sendNotification(welcomeEmail)
                    .thenAccept(response -> {
                        log.info("Welcome email sent successfully: {}", response.notificationId());
                        acknowledgment.acknowledge();
                    })
                    .exceptionally(throwable -> {
                        log.error("Failed to send welcome email for user: {}", userId, throwable);
                        // Don't acknowledge - will be retried
                        return null;
                    });
                    
            } catch (Exception e) {
                log.error("Error processing user registration event", e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Handle trade execution events from Trading Service
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Pattern Matching - Rule #14
     */
    @KafkaListener(topics = "trade-execution-events", groupId = "notification-service")
    public void handleTradeExecution(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        log.info("Received trade execution event: topic={}", topic);
        
        CompletableFuture.runAsync(() -> {
            try {
                String userId = (String) event.get("userId");
                String symbol = (String) event.get("symbol");
                String status = (String) event.get("status");
                Double quantity = (Double) event.get("quantity");
                Double price = (Double) event.get("price");
                String orderId = (String) event.get("orderId");
                String userEmail = (String) event.get("userEmail");
                String userPhone = (String) event.get("userPhone");
                
                // Send trade confirmation based on status
                String templateName = switch (status) {
                    case "EXECUTED" -> NotificationConstants.TRADE_EXECUTED_TEMPLATE;
                    case "FAILED" -> NotificationConstants.TRADE_FAILED_TEMPLATE;
                    case "CANCELLED" -> NotificationConstants.TRADE_CANCELLED_TEMPLATE;
                    default -> NotificationConstants.TRADE_STATUS_TEMPLATE;
                };
                
                // Send email notification
                if (userEmail != null) {
                    NotificationRequest emailNotification = NotificationRequest.templated(
                        NotificationRequest.NotificationType.EMAIL,
                        userEmail,
                        templateName,
                        Map.of(
                            "symbol", symbol,
                            "quantity", quantity.toString(),
                            "price", price.toString(),
                            "status", status,
                            "orderId", orderId,
                            "dashboardUrl", "https://app.trademaster.com/portfolio"
                        )
                    );
                    
                    notificationService.sendNotification(emailNotification);
                }
                
                // Send SMS for high-value trades
                if (userPhone != null && (price * quantity) > 10000) {
                    String smsContent = String.format(
                        "Trade Alert: %s %.2f shares of %s at ₹%.2f. Status: %s. Order: %s",
                        status, quantity, symbol, price, status, orderId
                    );
                    
                    NotificationRequest smsNotification = NotificationRequest.sms(
                        userPhone,
                        "Trade Alert",
                        smsContent
                    );
                    
                    notificationService.sendNotification(smsNotification);
                }
                
                acknowledgment.acknowledge();
                log.info("Trade execution notifications sent for order: {}", orderId);
                
            } catch (Exception e) {
                log.error("Error processing trade execution event", e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Handle subscription events from Subscription Service
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    @KafkaListener(topics = "subscription-events", groupId = "notification-service")
    public void handleSubscriptionEvents(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        log.info("Received subscription event: topic={}", topic);
        
        CompletableFuture.runAsync(() -> {
            try {
                String eventType = (String) event.get("eventType");
                String userId = (String) event.get("userId");
                String userEmail = (String) event.get("userEmail");
                String subscriptionTier = (String) event.get("subscriptionTier");
                
                String templateName = switch (eventType) {
                    case "SUBSCRIPTION_ACTIVATED" -> NotificationConstants.SUBSCRIPTION_ACTIVATED_TEMPLATE;
                    case "SUBSCRIPTION_UPGRADED" -> NotificationConstants.SUBSCRIPTION_UPGRADED_TEMPLATE;
                    case "SUBSCRIPTION_CANCELLED" -> NotificationConstants.SUBSCRIPTION_CANCELLED_TEMPLATE;
                    case "PAYMENT_FAILED" -> NotificationConstants.PAYMENT_FAILED_TEMPLATE;
                    default -> NotificationConstants.SUBSCRIPTION_UPDATE_TEMPLATE;
                };
                
                if (userEmail != null) {
                    NotificationRequest notification = NotificationRequest.templated(
                        NotificationRequest.NotificationType.EMAIL,
                        userEmail,
                        templateName,
                        Map.of(
                            "subscriptionTier", subscriptionTier,
                            "eventType", eventType,
                            "dashboardUrl", "https://app.trademaster.com/billing",
                            "supportUrl", "https://app.trademaster.com/support"
                        )
                    );
                    
                    notificationService.sendNotification(notification)
                        .thenRun(() -> {
                            acknowledgment.acknowledge();
                            log.info("Subscription notification sent for user: {}", userId);
                        });
                }
                
            } catch (Exception e) {
                log.error("Error processing subscription event", e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Handle payment events from Payment Service
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentEvents(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        log.info("Received payment event: topic={}", topic);
        
        CompletableFuture.runAsync(() -> {
            try {
                String eventType = (String) event.get("eventType");
                String userId = (String) event.get("userId");
                String userEmail = (String) event.get("userEmail");
                String paymentId = (String) event.get("paymentId");
                Double amount = (Double) event.get("amount");
                
                if ("PAYMENT_FAILED".equals(eventType) && userEmail != null) {
                    NotificationRequest notification = NotificationRequest.templated(
                        NotificationRequest.NotificationType.EMAIL,
                        userEmail,
                        NotificationConstants.PAYMENT_FAILED_TEMPLATE,
                        Map.of(
                            "amount", amount.toString(),
                            "paymentId", paymentId,
                            "retryUrl", "https://app.trademaster.com/billing/retry",
                            "supportUrl", "https://app.trademaster.com/support"
                        )
                    );
                    
                    notificationService.sendNotification(notification)
                        .thenRun(() -> {
                            acknowledgment.acknowledge();
                            log.info("Payment failure notification sent for user: {}", userId);
                        });
                }
                
            } catch (Exception e) {
                log.error("Error processing payment event", e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Handle security alerts from various services
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Zero Trust Security - Rule #6
     */
    @KafkaListener(topics = "security-alerts", groupId = "notification-service")
    public void handleSecurityAlerts(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        log.info("Received security alert: topic={}", topic);
        
        CompletableFuture.runAsync(() -> {
            try {
                String alertType = (String) event.get("alertType");
                String userId = (String) event.get("userId");
                String userEmail = (String) event.get("userEmail");
                String userPhone = (String) event.get("userPhone");
                String severity = (String) event.get("severity");
                String description = (String) event.get("description");
                
                // Send email alert
                if (userEmail != null) {
                    NotificationRequest emailAlert = NotificationRequest.templated(
                        NotificationRequest.NotificationType.EMAIL,
                        userEmail,
                        NotificationConstants.SECURITY_ALERT_TEMPLATE,
                        Map.of(
                            "alertType", alertType,
                            "severity", severity,
                            "description", description,
                            "securityUrl", "https://app.trademaster.com/security",
                            "supportUrl", "https://app.trademaster.com/support"
                        )
                    );
                    
                    notificationService.sendNotification(emailAlert);
                }
                
                // Send SMS for high severity alerts
                if ("HIGH".equals(severity) && userPhone != null) {
                    String smsContent = String.format(
                        "SECURITY ALERT: %s detected on your TradeMaster account. Please check immediately: https://app.trademaster.com/security",
                        alertType
                    );
                    
                    NotificationRequest smsAlert = NotificationRequest.sms(
                        userPhone,
                        "Security Alert",
                        smsContent
                    );
                    
                    notificationService.sendNotification(smsAlert);
                }
                
                acknowledgment.acknowledge();
                log.info("Security alert notifications sent for user: {}", userId);
                
            } catch (Exception e) {
                log.error("Error processing security alert", e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
}