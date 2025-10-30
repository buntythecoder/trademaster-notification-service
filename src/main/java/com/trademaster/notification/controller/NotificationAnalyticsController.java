package com.trademaster.notification.controller;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.service.NotificationAnalyticsService;
import com.trademaster.notification.service.NotificationAnalyticsService.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *  NOTIFICATION ANALYTICS CONTROLLER: Analytics API Endpoints
 *
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility - ONLY analytics endpoint handling
 * - Rule #6: Zero Trust Security - PreAuthorize for access control
 * - Rule #15: Structured logging with correlation IDs
 *
 * RESPONSIBILITIES:
 * - Handle analytics API requests
 * - Validate query parameters
 * - Invoke analytics service
 * - Return structured responses
 *
 * Cognitive Complexity: d7 per method, d15 total per class
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/notifications/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Analytics", description = "Notification analytics and reporting endpoints")
public class NotificationAnalyticsController {

    private final NotificationAnalyticsService analyticsService;

    /**
     *  FUNCTIONAL: Get delivery rate by channel
     * Cognitive Complexity: 2
     */
    @GetMapping("/delivery-rate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS_USER')")
    @Operation(summary = "Get delivery rate", description = "Get notification delivery rate by channel")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Delivery rate retrieved successfully",
            content = @Content(schema = @Schema(implementation = DeliveryRateAnalytics.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<DeliveryRateAnalytics>> getDeliveryRate(
            @RequestParam(value = "channel") final String channel,
            @RequestParam(value = "startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startTime,
            @RequestParam(value = "endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endTime) {

        log.info("Fetching delivery rate: channel={}, startTime={}, endTime={}",
            channel, startTime, endTime);

        final NotificationRequest.NotificationType channelType =
            NotificationRequest.NotificationType.valueOf(channel.toUpperCase());
        final TimeRange timeRange = new TimeRange(startTime, endTime);

        return analyticsService.calculateDeliveryRate(channelType, timeRange)
            .thenApply(ResponseEntity::ok);
    }

    /**
     *  FUNCTIONAL: Get engagement analytics for user
     * Cognitive Complexity: 2
     */
    @GetMapping("/engagement")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS_USER')")
    @Operation(summary = "Get engagement score", description = "Get notification engagement score for user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Engagement analytics retrieved successfully",
            content = @Content(schema = @Schema(implementation = EngagementAnalytics.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<EngagementAnalytics>> getEngagementScore(
            @RequestParam(value = "userId") final String userId,
            @RequestParam(value = "startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startTime,
            @RequestParam(value = "endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endTime) {

        log.info("Fetching engagement analytics: userId={}, startTime={}, endTime={}",
            userId, startTime, endTime);

        final TimeRange timeRange = new TimeRange(startTime, endTime);

        return analyticsService.calculateEngagement(userId, timeRange)
            .thenApply(ResponseEntity::ok);
    }

    /**
     *  FUNCTIONAL: Get channel performance comparison
     * Cognitive Complexity: 2
     */
    @GetMapping("/channel-performance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS_USER')")
    @Operation(summary = "Get channel performance", description = "Compare performance across all notification channels")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Channel performance retrieved successfully",
            content = @Content(schema = @Schema(implementation = ChannelPerformance.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<ChannelPerformance>>> getChannelPerformance(
            @RequestParam(value = "startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startTime,
            @RequestParam(value = "endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endTime) {

        log.info("Fetching channel performance: startTime={}, endTime={}", startTime, endTime);

        final TimeRange timeRange = new TimeRange(startTime, endTime);

        return analyticsService.calculateChannelPerformance(timeRange)
            .thenApply(ResponseEntity::ok);
    }
}
