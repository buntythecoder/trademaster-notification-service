package com.trademaster.notification.service;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.constant.NotificationConstants;
import com.trademaster.notification.common.Result;
import com.trademaster.notification.entity.NotificationHistory.NotificationStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Email Notification Service
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Error Handling - Rule #11
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true", matchIfMissing = true)
public class EmailNotificationService {
    
    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;
    private final NotificationHistoryService historyService;
    private final CircuitBreaker emailCircuitBreaker;
    private final Retry emailRetry;
    private final TimeLimiter emailTimeLimiter;
    
    private static final java.util.concurrent.ScheduledExecutorService scheduler = 
        java.util.concurrent.Executors.newScheduledThreadPool(2);
    
    @Value("${notification.email.default-sender:noreply@trademaster.com}")
    private String defaultSender;
    
    /**
     * Send email notification asynchronously with virtual threads and circuit breaker
     * 
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Circuit Breaker - Rule #24
     * MANDATORY: CompletableFuture - Rule #11
     */
    public CompletableFuture<NotificationResponse> sendEmail(NotificationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        
        return historyService.createNotificationHistory(request, correlationId)
            .thenCompose(historyResult -> historyResult.match(
                history -> sendEmailWithResilience(request, history.getNotificationId())
                    .thenCompose(response -> updateHistoryAfterSend(response, history.getNotificationId())),
                error -> CompletableFuture.completedFuture(NotificationResponse.failure("HISTORY_ERROR", error))
            ));
    }
    
    /**
     * Send email with circuit breaker, retry, and timeout protection
     * 
     * MANDATORY: Resilience Patterns - Rule #24
     * MANDATORY: Virtual Threads - Rule #12
     */
    private CompletableFuture<NotificationResponse> sendEmailWithResilience(NotificationRequest request, String notificationId) {
        // Create resilient email sending function with enterprise-grade decorator pattern
        try {
            // Use Resilience4j Decorators with proper chaining order
            var decoratedSupplier = Decorators.ofSupplier(() -> performEmailSend(request, notificationId))
                .withCircuitBreaker(emailCircuitBreaker)
                .withRetry(emailRetry)
                .decorate();
            
            // Apply TimeLimiter with CompletionStage
            return emailTimeLimiter.executeCompletionStage(
                scheduler,
                () -> CompletableFuture.supplyAsync(decoratedSupplier, Executors.newVirtualThreadPerTaskExecutor())
            ).toCompletableFuture()
                .exceptionally(throwable -> {
                    log.error("Email sending failed: notificationId={}, error={}", 
                             notificationId, throwable.getMessage());
                    return NotificationResponse.failure(notificationId, 
                        "Email service unavailable: " + throwable.getMessage());
                });
                
        } catch (Exception e) {
            log.error("Email sending setup failed: notificationId={}, error={}", 
                     notificationId, e.getMessage());
            return CompletableFuture.completedFuture(
                NotificationResponse.failure(notificationId, 
                    "Email service unavailable: " + e.getMessage()));
        }
    }
    
    /**
     * Perform email sending with pattern matching
     * 
     * MANDATORY: Pattern Matching - Rule #14
     * MANDATORY: Functional Programming - Rule #3
     */
    private NotificationResponse performEmailSend(NotificationRequest request, String notificationId) {
        log.info("Sending email notification to: {}, subject: {}, ID: {}", 
                request.emailRecipient(), request.subject(), notificationId);
        
        return switch (determineEmailType(request)) {
            case TEMPLATED -> sendTemplatedEmail(request, notificationId);
            case SIMPLE -> sendSimpleEmail(request, notificationId);
            case INVALID -> NotificationResponse.failure(notificationId, 
                "Invalid email configuration");
        };
    }
    
    private EmailType determineEmailType(NotificationRequest request) {
        return Optional.ofNullable(request.emailRecipient())
            .filter(email -> !email.isEmpty())
            .map(email -> Optional.ofNullable(request.templateName())
                .filter(template -> !template.isEmpty())
                .map(template -> EmailType.TEMPLATED)
                .orElse(EmailType.SIMPLE))
            .orElse(EmailType.INVALID);
    }
    
    private NotificationResponse sendSimpleEmail(NotificationRequest request, String notificationId) {
        return Result.tryExecute(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(defaultSender);
            message.setTo(request.emailRecipient());
            message.setSubject(request.subject());
            message.setText(request.content());
            
            emailSender.send(message);
            log.info("Simple email sent successfully to: {}", request.emailRecipient());
            
            return NotificationResponse.success(notificationId, "EMAIL_" + System.currentTimeMillis());
        }).match(
            response -> response,
            exception -> {
                log.error("Failed to send simple email to: {}, error: {}", 
                         request.emailRecipient(), exception.getMessage());
                return NotificationResponse.failure(notificationId, exception.getMessage());
            }
        );
    }
    
    private NotificationResponse sendTemplatedEmail(NotificationRequest request, String notificationId) {
        return Result.tryExecuteChecked(() -> {
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(defaultSender);
            helper.setTo(request.emailRecipient());
            helper.setSubject(request.subject());
            
            String htmlContent = processTemplate(request.templateName(), request.templateVariables());
            helper.setText(htmlContent, true);
            
            emailSender.send(mimeMessage);
            log.info("Templated email sent successfully to: {}", request.emailRecipient());
            
            return NotificationResponse.success(notificationId, "EMAIL_TEMPLATE_" + System.currentTimeMillis());
        }).match(
            response -> response,
            exception -> {
                log.error("Failed to send templated email to: {}, error: {}", 
                         request.emailRecipient(), exception.getMessage());
                return NotificationResponse.failure(notificationId, exception.getMessage());
            }
        );
    }
    
    /**
     * Process email template with variables
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2
     */
    private String processTemplate(String templateName, Map<String, Object> variables) {
        return Result.tryExecute(() -> {
            Context context = new Context();

            Optional.ofNullable(variables)
                .filter(vars -> !vars.isEmpty())
                .ifPresent(context::setVariables);

            return templateEngine.process(templateName, context);
        }).match(
            content -> content,
            exception -> {
                log.error("Failed to process email template: {}, error: {}", templateName, exception.getMessage());
                throw new RuntimeException("Template processing failed: " + exception.getMessage(), exception);
            }
        );
    }
    
    /**
     * Handle email result with error checking
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, Optional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 1
     */
    private NotificationResponse handleEmailResult(NotificationResponse result, Throwable throwable) {
        return Optional.ofNullable(throwable)
            .map(error -> {
                log.error("Email notification error", error);
                return NotificationResponse.failure("EMAIL_ERROR", error.getMessage());
            })
            .orElse(result);
    }
    
    // Factory methods for common email types
    public static NotificationRequest createWelcomeEmail(String email, String firstName, String lastName) {
        return NotificationRequest.templated(
            NotificationRequest.NotificationType.EMAIL,
            email,
            NotificationConstants.WELCOME_EMAIL_TEMPLATE,
            Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "dashboardUrl", "https://app.trademaster.com/dashboard"
            )
        );
    }
    
    public static NotificationRequest createKycApprovalEmail(String email, String firstName) {
        return NotificationRequest.templated(
            NotificationRequest.NotificationType.EMAIL,
            email,
            NotificationConstants.KYC_APPROVAL_TEMPLATE,
            Map.of(
                "firstName", firstName,
                "tradingUrl", "https://app.trademaster.com/trading"
            )
        );
    }
    
    /**
     * Update notification history after send attempt
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, pattern matching)
     * MANDATORY: Rule #14 - Pattern Matching with switch expression
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2
     */
    private CompletableFuture<NotificationResponse> updateHistoryAfterSend(
            NotificationResponse response,
            String notificationId) {

        return switch (response.success()) {
            case true -> historyService.updateNotificationStatus(
                    notificationId,
                    NotificationStatus.SENT,
                    response.deliveryId(),
                    "system")
                .thenApply(historyResult -> response);
            case false -> historyService.markNotificationFailed(
                    notificationId,
                    response.message(),
                    "system")
                .thenApply(historyResult -> response);
        };
    }
    
    private enum EmailType {
        SIMPLE,
        TEMPLATED,
        INVALID
    }
}