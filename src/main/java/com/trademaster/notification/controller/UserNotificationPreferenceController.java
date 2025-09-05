package com.trademaster.notification.controller;

import com.trademaster.notification.entity.UserNotificationPreference;
import com.trademaster.notification.entity.NotificationTemplate.TemplateCategory;
import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.service.UserNotificationPreferenceService;
import com.trademaster.notification.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * User Notification Preference Management Controller
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Security First - Rule #6
 * MANDATORY: User Preference Management - FRONT-020
 */
@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://app.trademaster.com"})
@Tag(name = "User Notification Preferences", description = "User notification preference management operations")
public class UserNotificationPreferenceController {
    
    private final UserNotificationPreferenceService preferenceService;
    
    /**
     * Get user's notification preferences
     * 
     * MANDATORY: Security - User can only access own preferences unless admin
     */
    @GetMapping("/{userId}")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> getUserPreferences(@PathVariable String userId) {
        
        log.info("Getting preferences for user: {}", userId);
        
        return preferenceService.getUserPreferences(userId)
            .thenApply(optionalPreference -> optionalPreference
                .map(preference -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", preference
                )))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", "Preferences not found for user: " + userId
                    ))
                ));
    }
    
    /**
     * Create default preferences for new user
     */
    @PostMapping("/{userId}/default")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> createDefaultPreferences(
            @PathVariable String userId,
            @RequestHeader("X-User-ID") String currentUserId) {
        
        log.info("Creating default preferences for user: {} by: {}", userId, currentUserId);
        
        return preferenceService.createDefaultPreferences(userId, currentUserId)
            .thenApply(result -> result.match(
                preference -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "success", true,
                        "message", "Default preferences created successfully",
                        "data", preference
                    )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Update notification preferences
     */
    @PutMapping("/{userId}/preferences")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> updateNotificationPreferences(
            @PathVariable String userId,
            @Valid @RequestBody UpdatePreferencesRequest request,
            @RequestHeader("X-User-ID") String currentUserId) {
        
        log.info("Updating preferences for user: {} by: {}", userId, currentUserId);
        
        return preferenceService.updateNotificationPreferences(
                userId,
                request.notificationsEnabled(),
                request.preferredChannel(),
                request.enabledChannels(),
                request.enabledCategories(),
                currentUserId
            )
            .thenApply(result -> result.match(
                preference -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Preferences updated successfully",
                    "data", preference
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Update contact information
     */
    @PutMapping("/{userId}/contact")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> updateContactInformation(
            @PathVariable String userId,
            @Valid @RequestBody UpdateContactRequest request,
            @RequestHeader("X-User-ID") String currentUserId) {
        
        log.info("Updating contact info for user: {} by: {}", userId, currentUserId);
        
        return preferenceService.updateContactInformation(
                userId,
                request.emailAddress(),
                request.phoneNumber(),
                currentUserId
            )
            .thenApply(result -> result.match(
                preference -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Contact information updated successfully",
                    "data", preference
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Update quiet hours settings
     */
    @PutMapping("/{userId}/quiet-hours")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> updateQuietHours(
            @PathVariable String userId,
            @Valid @RequestBody UpdateQuietHoursRequest request,
            @RequestHeader("X-User-ID") String currentUserId) {
        
        log.info("Updating quiet hours for user: {} by: {}", userId, currentUserId);
        
        return preferenceService.updateQuietHours(
                userId,
                request.quietHoursEnabled(),
                request.quietStartTime(),
                request.quietEndTime(),
                request.timeZone(),
                currentUserId
            )
            .thenApply(result -> result.match(
                preference -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Quiet hours updated successfully",
                    "data", preference
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Update frequency limits
     */
    @PutMapping("/{userId}/frequency-limits")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> updateFrequencyLimits(
            @PathVariable String userId,
            @Valid @RequestBody UpdateFrequencyLimitsRequest request,
            @RequestHeader("X-User-ID") String currentUserId) {
        
        log.info("Updating frequency limits for user: {} by: {}", userId, currentUserId);
        
        return preferenceService.updateFrequencyLimits(
                userId,
                request.frequencyLimitPerHour(),
                request.frequencyLimitPerDay(),
                currentUserId
            )
            .thenApply(result -> result.match(
                preference -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Frequency limits updated successfully",
                    "data", preference
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Check if notification is allowed
     */
    @GetMapping("/{userId}/check")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> checkNotificationAllowed(
            @PathVariable String userId,
            @RequestParam NotificationRequest.NotificationType type,
            @RequestParam TemplateCategory category) {
        
        log.info("Checking notification allowed: user={}, type={}, category={}", userId, type, category);
        
        return preferenceService.isNotificationAllowed(userId, type, category)
            .thenApply(allowed -> ResponseEntity.ok(Map.of(
                "success", true,
                "allowed", allowed,
                "userId", userId,
                "type", type,
                "category", category
            )));
    }
    
    /**
     * Check if user is in quiet hours
     */
    @GetMapping("/{userId}/quiet-hours/check")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> checkQuietHours(@PathVariable String userId) {
        
        log.info("Checking quiet hours for user: {}", userId);
        
        return preferenceService.isUserInQuietHours(userId)
            .thenApply(inQuietHours -> ResponseEntity.ok(Map.of(
                "success", true,
                "inQuietHours", inQuietHours,
                "userId", userId
            )));
    }
    
    /**
     * Get contact info for notification type
     */
    @GetMapping("/{userId}/contact/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> getContactInfo(
            @PathVariable String userId,
            @PathVariable NotificationRequest.NotificationType type) {
        
        log.info("Getting contact info: user={}, type={}", userId, type);
        
        return preferenceService.getContactInfo(userId, type)
            .thenApply(optionalContact -> optionalContact
                .map(contact -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "contactInfo", contact,
                    "type", type
                )))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", "Contact info not found for type: " + type
                    ))
                ));
    }
    
    /**
     * Get users by preferred channel (Admin only)
     */
    @GetMapping("/by-channel/{channel}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> getUsersByPreferredChannel(
            @PathVariable NotificationRequest.NotificationType channel) {
        
        log.info("Getting users by preferred channel: {}", channel);
        
        return preferenceService.getUsersByPreferredChannel(channel)
            .thenApply(users -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", users,
                "count", users.size(),
                "channel", channel
            )));
    }
    
    /**
     * Get users by category interest (Admin only)
     */
    @GetMapping("/by-category/{category}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> getUsersByCategory(@PathVariable TemplateCategory category) {
        
        log.info("Getting users by category: {}", category);
        
        return preferenceService.getUsersByCategory(category)
            .thenApply(users -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", users,
                "count", users.size(),
                "category", category
            )));
    }
    
    /**
     * Get users by language (Admin only)
     */
    @GetMapping("/by-language/{language}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> getUsersByLanguage(@PathVariable String language) {
        
        log.info("Getting users by language: {}", language);
        
        return preferenceService.getUsersByLanguage(language)
            .thenApply(users -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", users,
                "count", users.size(),
                "language", language
            )));
    }
    
    /**
     * Search preferences with pagination (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> searchPreferences(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) NotificationRequest.NotificationType channel,
            @RequestParam(required = false) String language,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Searching preferences: enabled={}, channel={}, language={}", enabled, channel, language);
        
        return preferenceService.searchPreferences(enabled, channel, language, pageable)
            .thenApply(page -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", page.getContent(),
                "pagination", Map.of(
                    "page", page.getNumber(),
                    "size", page.getSize(),
                    "totalElements", page.getTotalElements(),
                    "totalPages", page.getTotalPages()
                )
            )));
    }
    
    /**
     * Get preference statistics (Admin only)
     */
    @GetMapping("/statistics/channels")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> getChannelStatistics() {
        
        log.info("Getting channel preference statistics");
        
        return preferenceService.getChannelPreferenceStatistics()
            .thenApply(stats -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            )));
    }
    
    /**
     * Get language distribution statistics (Admin only)
     */
    @GetMapping("/statistics/languages")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> getLanguageStatistics() {
        
        log.info("Getting language distribution statistics");
        
        return preferenceService.getLanguageDistributionStatistics()
            .thenApply(stats -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            )));
    }
    
    /**
     * Get users with missing contact info (Admin only)
     */
    @GetMapping("/missing-contact")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> getUsersWithMissingContact() {
        
        log.info("Getting users with missing contact information");
        
        return preferenceService.findUsersWithMissingContactInfo()
            .thenApply(users -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", users,
                "count", users.size()
            )));
    }
    
    /**
     * Bulk update notification status (Admin only)
     */
    @PatchMapping("/bulk/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusUpdateRequest request,
            @RequestHeader("X-User-ID") String currentUserId) {
        
        log.info("Bulk updating preference status: count={}, enabled={}, user={}", 
                request.userIds().size(), request.enabled(), currentUserId);
        
        return preferenceService.updateNotificationStatusBatch(request.userIds(), request.enabled(), currentUserId)
            .thenApply(result -> result.match(
                count -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Preferences updated successfully",
                    "updatedCount", count
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Get or create user preferences
     */
    @PostMapping("/{userId}")
    @PreAuthorize("authentication.name == #userId or hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> getOrCreatePreferences(
            @PathVariable String userId,
            @RequestHeader("X-User-ID") String currentUserId) {
        
        log.info("Getting or creating preferences for user: {} by: {}", userId, currentUserId);
        
        return preferenceService.getOrCreateUserPreferences(userId, currentUserId)
            .thenApply(result -> result.match(
                preference -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", preference
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    // Request DTOs
    public record UpdatePreferencesRequest(
        Boolean notificationsEnabled,
        NotificationRequest.NotificationType preferredChannel,
        Set<NotificationRequest.NotificationType> enabledChannels,
        Set<TemplateCategory> enabledCategories
    ) {}
    
    public record UpdateContactRequest(
        @Email String emailAddress,
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format") String phoneNumber
    ) {}
    
    public record UpdateQuietHoursRequest(
        Boolean quietHoursEnabled,
        LocalTime quietStartTime,
        LocalTime quietEndTime,
        String timeZone
    ) {}
    
    public record UpdateFrequencyLimitsRequest(
        Integer frequencyLimitPerHour,
        Integer frequencyLimitPerDay
    ) {}
    
    public record BulkStatusUpdateRequest(
        @NotNull List<String> userIds,
        @NotNull Boolean enabled
    ) {}
}