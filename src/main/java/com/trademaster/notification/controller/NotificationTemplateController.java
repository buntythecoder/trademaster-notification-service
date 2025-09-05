package com.trademaster.notification.controller;

import com.trademaster.notification.entity.NotificationTemplate;
import com.trademaster.notification.entity.NotificationTemplate.TemplateCategory;
import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.service.NotificationTemplateService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Notification Template Management Controller
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Security First - Rule #6
 * MANDATORY: Template Management - FRONT-020
 */
@RestController
@RequestMapping("/api/v1/notification-templates")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://app.trademaster.com"})
@Tag(name = "Notification Templates", description = "Template management operations for notification system")
public class NotificationTemplateController {
    
    private final NotificationTemplateService templateService;
    
    /**
     * Create new notification template
     * 
     * MANDATORY: Security - Admin/Manager access only
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    @Operation(
        summary = "Create notification template",
        description = "Creates a new notification template with specified configuration. Requires ADMIN or NOTIFICATION_MANAGER role.",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Template created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Object.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "success": true,
                          "message": "Template created successfully",
                          "data": {
                            "templateId": "tmpl_123",
                            "templateName": "trade_alert",
                            "displayName": "Trade Alert",
                            "active": true
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "success": false,
                          "message": "Template name already exists"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public CompletableFuture<ResponseEntity<?>> createTemplate(
            @Parameter(description = "Template creation request", required = true)
            @Valid @RequestBody CreateTemplateRequest request,
            @Parameter(description = "User ID performing the operation", required = true)
            @RequestHeader("X-User-ID") String userId) {
        
        log.info("Creating template: name={}, category={}, user={}", 
                request.templateName(), request.category(), userId);
        
        return templateService.createTemplate(
                request.templateName(),
                request.displayName(),
                request.description(),
                request.notificationType(),
                request.category(),
                request.subjectTemplate(),
                request.contentTemplate(),
                userId
            )
            .thenApply(result -> result.match(
                template -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "success", true,
                        "message", "Template created successfully",
                        "data", template
                    )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Update existing template
     */
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    @Operation(
        summary = "Update notification template",
        description = "Updates an existing notification template. Requires ADMIN or NOTIFICATION_MANAGER role.",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Template updated successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public CompletableFuture<ResponseEntity<?>> updateTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable String templateId,
            @Parameter(description = "Template update request", required = true)
            @Valid @RequestBody UpdateTemplateRequest request,
            @Parameter(description = "User ID performing the operation", required = true)
            @RequestHeader("X-User-ID") String userId) {
        
        log.info("Updating template: ID={}, user={}", templateId, userId);
        
        return templateService.updateTemplate(
                templateId,
                request.displayName(),
                request.description(),
                request.subjectTemplate(),
                request.contentTemplate(),
                request.htmlTemplate(),
                userId
            )
            .thenApply(result -> result.match(
                template -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Template updated successfully",
                    "data", template
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Create new version of template
     */
    @PostMapping("/{templateId}/versions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> createTemplateVersion(
            @PathVariable String templateId,
            @RequestHeader("X-User-ID") String userId) {
        
        log.info("Creating template version: ID={}, user={}", templateId, userId);
        
        return templateService.createTemplateVersion(templateId, userId)
            .thenApply(result -> result.match(
                template -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "success", true,
                        "message", "Template version created successfully",
                        "data", template
                    )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Update template status (activate/deactivate)
     */
    @PatchMapping("/{templateId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> updateTemplateStatus(
            @PathVariable String templateId,
            @RequestParam boolean active,
            @RequestHeader("X-User-ID") String userId) {
        
        log.info("Updating template status: ID={}, active={}, user={}", templateId, active, userId);
        
        return templateService.updateTemplateStatus(templateId, active, userId)
            .thenApply(result -> result.match(
                template -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Template status updated successfully",
                    "data", template
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Get template by name
     */
    @GetMapping("/by-name/{templateName}")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> getTemplateByName(@PathVariable String templateName) {
        
        log.info("Getting template by name: {}", templateName);
        
        return templateService.getTemplateByName(templateName)
            .thenApply(optionalTemplate -> optionalTemplate
                .map(template -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", template
                )))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", "Template not found: " + templateName
                    ))
                ));
    }
    
    /**
     * Get templates by category
     */
    @GetMapping("/by-category/{category}")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> getTemplatesByCategory(@PathVariable TemplateCategory category) {
        
        log.info("Getting templates by category: {}", category);
        
        return templateService.getTemplatesByCategory(category)
            .thenApply(templates -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", templates,
                "count", templates.size()
            )));
    }
    
    /**
     * Get templates by notification type
     */
    @GetMapping("/by-type/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> getTemplatesByType(
            @PathVariable NotificationRequest.NotificationType type) {
        
        log.info("Getting templates by type: {}", type);
        
        return templateService.getTemplatesByType(type)
            .thenApply(templates -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", templates,
                "count", templates.size()
            )));
    }
    
    /**
     * Search templates with pagination and filters
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    @Operation(
        summary = "Search notification templates",
        description = "Search and filter notification templates with pagination support",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Templates retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "success": true,
                          "data": [
                            {
                              "templateId": "tmpl_123",
                              "templateName": "trade_alert",
                              "displayName": "Trade Alert",
                              "category": "TRADING",
                              "active": true
                            }
                          ],
                          "pagination": {
                            "page": 0,
                            "size": 20,
                            "totalElements": 1,
                            "totalPages": 1
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public CompletableFuture<ResponseEntity<?>> searchTemplates(
            @Parameter(description = "Filter by template category")
            @RequestParam(required = false) TemplateCategory category,
            @Parameter(description = "Filter by notification type")
            @RequestParam(required = false) NotificationRequest.NotificationType type,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Searching templates: category={}, type={}, active={}", category, type, active);
        
        return templateService.searchTemplates(category, type, active, pageable)
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
     * Search templates by content
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> searchTemplatesByContent(
            @RequestParam String searchTerm) {
        
        log.info("Searching templates by term: {}", searchTerm);
        
        return templateService.searchTemplatesByTerm(searchTerm)
            .thenApply(templates -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", templates,
                "count", templates.size()
            )));
    }
    
    /**
     * Get template usage statistics
     */
    @GetMapping("/statistics/usage")
    @PreAuthorize("hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> getTemplateUsageStatistics(
            @RequestParam(defaultValue = "30") int days) {
        
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        
        log.info("Getting template usage statistics from: {}", fromDate);
        
        return templateService.getTemplateUsageStatistics(fromDate)
            .thenApply(stats -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats,
                "period", Map.of(
                    "fromDate", fromDate,
                    "days", days
                )
            )));
    }
    
    /**
     * Get template category statistics
     */
    @GetMapping("/statistics/categories")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> getCategoryStatistics() {
        
        log.info("Getting template category statistics");
        
        return templateService.getCategoryStatistics()
            .thenApply(stats -> ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            )));
    }
    
    /**
     * Bulk update template status
     */
    @PatchMapping("/bulk/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('NOTIFICATION_MANAGER')")
    public CompletableFuture<ResponseEntity<?>> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusUpdateRequest request,
            @RequestHeader("X-User-ID") String userId) {
        
        log.info("Bulk updating template status: count={}, active={}, user={}", 
                request.templateIds().size(), request.active(), userId);
        
        return templateService.updateTemplateStatusBatch(request.templateIds(), request.active(), userId)
            .thenApply(result -> result.match(
                count -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Templates updated successfully",
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
     * Delete template (soft delete)
     */
    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> deleteTemplate(
            @PathVariable String templateId,
            @RequestHeader("X-User-ID") String userId) {
        
        log.info("Deleting template: ID={}, user={}", templateId, userId);
        
        return templateService.deleteTemplate(templateId, userId)
            .thenApply(result -> result.match(
                message -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", error
                    ))
            ));
    }
    
    /**
     * Validate template for sending
     */
    @GetMapping("/{templateName}/validate")
    @PreAuthorize("hasRole('USER') or hasRole('NOTIFICATION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> validateTemplate(@PathVariable String templateName) {
        
        log.info("Validating template: {}", templateName);
        
        return templateService.validateTemplateForSending(templateName)
            .thenApply(isValid -> ResponseEntity.ok(Map.of(
                "success", true,
                "valid", isValid,
                "template", templateName
            )));
    }
    
    // Request DTOs
    @Schema(description = "Request for creating a new notification template")
    public record CreateTemplateRequest(
        @Schema(description = "Unique template name identifier", example = "trade_execution_alert")
        @NotBlank String templateName,
        
        @Schema(description = "Human-readable display name", example = "Trade Execution Alert")
        @NotBlank String displayName,
        
        @Schema(description = "Optional template description", example = "Notification sent when a trade is executed")
        String description,
        
        @Schema(description = "Type of notification", example = "EMAIL")
        @NotNull NotificationRequest.NotificationType notificationType,
        
        @Schema(description = "Template category", example = "TRADING")
        @NotNull TemplateCategory category,
        
        @Schema(description = "Optional subject template with placeholders", example = "Trade Alert: {symbol}")
        String subjectTemplate,
        
        @Schema(description = "Content template with placeholders", example = "Your {action} order for {quantity} shares of {symbol} has been executed at ₹{price}")
        @NotBlank String contentTemplate
    ) {}
    
    @Schema(description = "Request for updating an existing notification template")
    public record UpdateTemplateRequest(
        @Schema(description = "Human-readable display name", example = "Updated Trade Alert")
        @NotBlank String displayName,
        
        @Schema(description = "Optional template description", example = "Updated notification for trade execution")
        String description,
        
        @Schema(description = "Optional subject template with placeholders", example = "Updated Trade Alert: {symbol}")
        String subjectTemplate,
        
        @Schema(description = "Content template with placeholders", example = "Your {action} order for {quantity} shares of {symbol} has been executed at ₹{price}")
        @NotBlank String contentTemplate,
        
        @Schema(description = "Optional HTML template for email notifications")
        String htmlTemplate
    ) {}
    
    @Schema(description = "Request for bulk updating template status")
    public record BulkStatusUpdateRequest(
        @Schema(description = "List of template IDs to update", example = "[\"tmpl_123\", \"tmpl_456\"]")
        @NotNull List<String> templateIds,
        
        @Schema(description = "New active status for templates", example = "true")
        @NotNull Boolean active
    ) {}
}