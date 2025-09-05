package com.trademaster.notification.controller;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.dto.NotificationResponse;
import com.trademaster.notification.dto.BulkNotificationRequest;
import com.trademaster.notification.service.NotificationService;
import com.trademaster.notification.constant.NotificationConstants;
import com.trademaster.notification.security.SecurityFacade;
import com.trademaster.notification.security.SecurityError;
import com.trademaster.notification.common.Result;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Notification REST Controller with Zero Trust Security
 * 
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: REST API Access - FRONT-020 Requirements
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Functional Programming - Rule #3
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    private final SecurityFacade securityFacade;
    
    /**
     * Send single notification with Zero Trust Security
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Pattern Matching - Rule #14
     */
    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<NotificationResponse>> sendNotification(
            @Valid @RequestBody NotificationRequest request,
            HttpServletRequest httpRequest) {
        
        return securityFacade.secureExternalAccess(
            httpRequest,
            "SEND_NOTIFICATION",
            () -> notificationService.sendNotificationSecure(request)
        ).thenApply(result -> 
            result.match(
                response -> ResponseEntity.ok(response),
                securityError -> createErrorResponse(securityError)
            )
        );
    }
    
    /**
     * Send bulk notifications with Zero Trust Security
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     * MANDATORY: Bulk Operations - FRONT-020 Requirements
     * MANDATORY: Pattern Matching - Rule #14
     */
    @PostMapping("/send/bulk")
    public CompletableFuture<ResponseEntity<List<NotificationResponse>>> sendBulkNotifications(
            @Valid @RequestBody BulkNotificationRequest request,
            HttpServletRequest httpRequest) {
        
        return securityFacade.secureExternalAccess(
            httpRequest,
            "BULK_NOTIFICATION",
            () -> notificationService.sendBulkNotificationsSecure(request)
        ).thenApply(result -> 
            result.match(
                responses -> ResponseEntity.ok(responses),
                securityError -> ResponseEntity.status(getHttpStatus(securityError))
                    .body(List.of(createErrorNotificationResponse(securityError)))
            )
        );
    }
    
    /**
     * Get notification status with security validation
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     */
    @GetMapping("/status/{notificationId}")
    public CompletableFuture<ResponseEntity<NotificationResponse>> getNotificationStatus(
            @PathVariable String notificationId,
            HttpServletRequest httpRequest) {
        
        return securityFacade.secureExternalAccess(
            httpRequest,
            "GET_STATUS",
            () -> CompletableFuture.completedFuture(
                notificationService.getNotificationStatus(notificationId)
                    .map(Result::<NotificationResponse, Exception>success)
                    .orElse(Result.failure(new RuntimeException("Notification not found")))
            )
        ).thenApply(result -> 
            result.match(
                response -> ResponseEntity.ok(response),
                securityError -> createErrorResponse(securityError)
            )
        );
    }
    
    /**
     * Send welcome email with security validation
     */
    @PostMapping("/welcome/email")
    public CompletableFuture<ResponseEntity<NotificationResponse>> sendWelcomeEmail(
            @RequestParam String email,
            @RequestParam String firstName,
            @RequestParam String lastName,
            HttpServletRequest httpRequest) {
        
        NotificationRequest request = NotificationRequest.templated(
            NotificationRequest.NotificationType.EMAIL,
            email,
            NotificationConstants.WELCOME_EMAIL_TEMPLATE,
            java.util.Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "dashboardUrl", "https://app.trademaster.com/dashboard"
            )
        );
        
        return sendNotification(request, httpRequest);
    }
    
    /**
     * Send OTP SMS with security validation
     */
    @PostMapping("/otp/sms")
    public CompletableFuture<ResponseEntity<NotificationResponse>> sendOtpSms(
            @RequestParam String phoneNumber,
            @RequestParam String otp,
            HttpServletRequest httpRequest) {
        
        NotificationRequest request = NotificationRequest.sms(
            phoneNumber,
            "Verification Code",
            String.format("Your TradeMaster verification code is: %s. Valid for 10 minutes. Do not share this code.", otp)
        );
        
        return sendNotification(request, httpRequest);
    }
    
    /**
     * Health check endpoint (no security required)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is healthy");
    }
    
    /**
     * Create error response from security error
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    private ResponseEntity<NotificationResponse> createErrorResponse(SecurityError securityError) {
        HttpStatus status = getHttpStatus(securityError);
        NotificationResponse errorResponse = createErrorNotificationResponse(securityError);
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Map security error to HTTP status
     * 
     * MANDATORY: Pattern Matching - Rule #14
     */
    private HttpStatus getHttpStatus(SecurityError securityError) {
        return switch (securityError) {
            case SecurityError.AuthenticationError ignored -> HttpStatus.UNAUTHORIZED;
            case SecurityError.AuthorizationError ignored -> HttpStatus.FORBIDDEN;
            case SecurityError.ValidationError ignored -> HttpStatus.BAD_REQUEST;
            case SecurityError.RiskError ignored -> HttpStatus.TOO_MANY_REQUESTS;
            case SecurityError.AuditError ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * Create notification response from security error
     */
    private NotificationResponse createErrorNotificationResponse(SecurityError securityError) {
        return NotificationResponse.failure(securityError.getCode(), securityError.getMessage());
    }
}