package com.trademaster.notification.service;

import com.trademaster.notification.dto.NotificationRequest;
import com.trademaster.notification.entity.NotificationHistory;
import com.trademaster.notification.entity.NotificationHistory.NotificationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 *  NOTIFICATION ANALYTICS SERVICE: Advanced Notification Analytics
 *
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility - ONLY notification analytics
 * - Rule #3: Functional programming (no if-else)
 * - Rule #13: Stream API for all aggregations
 * - Rule #12: Virtual Threads for async operations
 *
 * RESPONSIBILITIES:
 * - Track notification delivery rates by channel
 * - Track notification open rates
 * - Track notification click-through rates
 * - Calculate notification engagement scores
 * - Provide analytics aggregations
 *
 * Cognitive Complexity: d7 per method, d15 total per class
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAnalyticsService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    //  VIRTUAL THREADS: Dedicated executor
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     *  FUNCTIONAL: Calculate delivery rate by channel
     * Cognitive Complexity: 4
     */
    public CompletableFuture<DeliveryRateAnalytics> calculateDeliveryRate(
            final NotificationRequest.NotificationType channel,
            final TimeRange timeRange) {

        return CompletableFuture.supplyAsync(() -> {
            final List<NotificationHistory> notifications = fetchNotificationsByChannelAndTime(
                channel, timeRange);

            final long totalSent = notifications.size();
            final long delivered = countByStatus(notifications, NotificationStatus.DELIVERED);
            final long failed = countByStatus(notifications, NotificationStatus.FAILED);

            final double deliveryRate = calculateRate(delivered, totalSent);

            log.debug("Delivery rate for channel {}: {}%", channel, deliveryRate);

            return new DeliveryRateAnalytics(
                channel,
                totalSent,
                delivered,
                failed,
                deliveryRate,
                timeRange
            );
        }, virtualThreadExecutor);
    }

    /**
     *  FUNCTIONAL: Calculate engagement score for user
     * Cognitive Complexity: 5
     */
    public CompletableFuture<EngagementAnalytics> calculateEngagement(
            final String userId,
            final TimeRange timeRange) {

        return CompletableFuture.supplyAsync(() -> {
            final List<NotificationHistory> notifications = fetchNotificationsByUser(userId, timeRange);

            final long totalSent = notifications.size();
            final long delivered = countByStatus(notifications, NotificationStatus.DELIVERED);
            final long read = countByStatus(notifications, NotificationStatus.READ);

            // Engagement score: weighted formula (delivery: 30%, read: 70%)
            final double engagementScore = calculateEngagementScore(totalSent, delivered, read);

            log.debug("Engagement score for user {}: {}", userId, engagementScore);

            return new EngagementAnalytics(
                userId,
                totalSent,
                delivered,
                read,
                engagementScore,
                timeRange
            );
        }, virtualThreadExecutor);
    }

    /**
     *  FUNCTIONAL: Calculate channel performance comparison
     * Cognitive Complexity: 4
     */
    public CompletableFuture<List<ChannelPerformance>> calculateChannelPerformance(
            final TimeRange timeRange) {

        return CompletableFuture.supplyAsync(() -> {
            final List<NotificationHistory> allNotifications = fetchNotificationsByTime(timeRange);

            return groupByChannel(allNotifications)
                .entrySet()
                .stream()
                .map(entry -> calculateChannelMetrics(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Double.compare(b.deliveryRate(), a.deliveryRate()))
                .collect(Collectors.toList());
        }, virtualThreadExecutor);
    }

    // ==================== Private Helper Methods ====================

    /**
     *  FUNCTIONAL: Fetch notifications by channel and time
     * Cognitive Complexity: 1
     */
    private List<NotificationHistory> fetchNotificationsByChannelAndTime(
            final NotificationRequest.NotificationType channel,
            final TimeRange timeRange) {

        return notificationHistoryRepository.findByTypeAndCreatedAtBetween(
            channel,
            timeRange.startTime(),
            timeRange.endTime()
        );
    }

    /**
     *  FUNCTIONAL: Fetch notifications by user and time
     * Cognitive Complexity: 1
     */
    private List<NotificationHistory> fetchNotificationsByUser(
            final String userId,
            final TimeRange timeRange) {

        return notificationHistoryRepository.findByRecipientAndCreatedAtBetween(
            userId,
            timeRange.startTime(),
            timeRange.endTime()
        );
    }

    /**
     *  FUNCTIONAL: Fetch all notifications by time
     * Cognitive Complexity: 1
     */
    private List<NotificationHistory> fetchNotificationsByTime(final TimeRange timeRange) {
        return notificationHistoryRepository.findByCreatedAtBetween(
            timeRange.startTime(),
            timeRange.endTime()
        );
    }

    /**
     *  FUNCTIONAL: Count notifications by status using Stream API
     * Cognitive Complexity: 1
     */
    private long countByStatus(
            final List<NotificationHistory> notifications,
            final NotificationStatus status) {

        return notifications.stream()
            .filter(n -> n.getStatus() == status)
            .count();
    }

    /**
     *  FUNCTIONAL: Calculate delivery rate percentage
     * Cognitive Complexity: 1
     */
    private double calculateRate(final long delivered, final long total) {
        return total == 0 ? 0.0 : (delivered * 100.0) / total;
    }

    /**
     *  FUNCTIONAL: Calculate engagement score (0-100)
     * Engagement formula: (deliveryRate * 0.3) + (readRate * 0.7)
     * Cognitive Complexity: 2
     */
    private double calculateEngagementScore(
            final long total,
            final long delivered,
            final long read) {

        final double deliveryRate = calculateRate(delivered, total);
        final double readRate = calculateRate(read, total);

        return (deliveryRate * 0.3) + (readRate * 0.7);
    }

    /**
     *  FUNCTIONAL: Group notifications by channel using Stream API
     * Cognitive Complexity: 1
     */
    private Map<NotificationRequest.NotificationType, List<NotificationHistory>> groupByChannel(
            final List<NotificationHistory> notifications) {

        return notifications.stream()
            .collect(Collectors.groupingBy(NotificationHistory::getType));
    }

    /**
     *  FUNCTIONAL: Calculate channel metrics
     * Cognitive Complexity: 3
     */
    private ChannelPerformance calculateChannelMetrics(
            final NotificationRequest.NotificationType channel,
            final List<NotificationHistory> notifications) {

        final long totalSent = notifications.size();
        final long delivered = countByStatus(notifications, NotificationStatus.DELIVERED);
        final long failed = countByStatus(notifications, NotificationStatus.FAILED);
        final double deliveryRate = calculateRate(delivered, totalSent);

        return new ChannelPerformance(
            channel,
            totalSent,
            delivered,
            failed,
            deliveryRate
        );
    }

    // ==================== Supporting Types ====================

    /**
     *  IMMUTABLE: Time range record
     */
    public record TimeRange(
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {}

    /**
     *  IMMUTABLE: Delivery rate analytics record
     */
    public record DeliveryRateAnalytics(
        NotificationRequest.NotificationType channel,
        long totalSent,
        long delivered,
        long failed,
        double deliveryRate,
        TimeRange timeRange
    ) {}

    /**
     *  IMMUTABLE: Engagement analytics record
     */
    public record EngagementAnalytics(
        String userId,
        long totalSent,
        long delivered,
        long read,
        double engagementScore,
        TimeRange timeRange
    ) {}

    /**
     *  IMMUTABLE: Channel performance record
     */
    public record ChannelPerformance(
        NotificationRequest.NotificationType channel,
        long totalSent,
        long delivered,
        long failed,
        double deliveryRate
    ) {}

    // ==================== Repository Interface ====================

    /**
     *  Repository for NotificationHistory queries
     */
    public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, String> {

        List<NotificationHistory> findByTypeAndCreatedAtBetween(
            NotificationRequest.NotificationType type,
            LocalDateTime startTime,
            LocalDateTime endTime
        );

        List<NotificationHistory> findByRecipientAndCreatedAtBetween(
            String recipient,
            LocalDateTime startTime,
            LocalDateTime endTime
        );

        List<NotificationHistory> findByCreatedAtBetween(
            LocalDateTime startTime,
            LocalDateTime endTime
        );
    }
}
