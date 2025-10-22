package com.trademaster.notification.entity;

import com.trademaster.notification.dto.NotificationRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * User Notification Preferences Entity
 *
 * MANDATORY: JPA Best Practices - Rule #21
 * MANDATORY: User Preference Management - FRONT-020
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Lombok Standards - Rule #10
 */
@Entity
@Table(name = "user_notification_preferences", indexes = {
    @Index(name = "idx_user_preferences", columnList = "user_id", unique = true),
    @Index(name = "idx_preferences_enabled", columnList = "notifications_enabled"),
    @Index(name = "idx_preferences_channel", columnList = "preferred_channel"),
    @Index(name = "idx_preferences_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserNotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "preference_id", length = 36)
    private String preferenceId;
    
    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;
    
    @Column(name = "notifications_enabled", nullable = false)
    private Boolean notificationsEnabled = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_channel", nullable = false, length = 20)
    private NotificationRequest.NotificationType preferredChannel = NotificationRequest.NotificationType.EMAIL;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_enabled_notification_types", 
                    joinColumns = @JoinColumn(name = "preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 20)
    private Set<NotificationRequest.NotificationType> enabledChannels;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_notification_categories", 
                    joinColumns = @JoinColumn(name = "preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private Set<NotificationTemplate.TemplateCategory> enabledCategories;
    
    @Column(name = "email_address", length = 255)
    private String emailAddress;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "quiet_hours_enabled", nullable = false)
    private Boolean quietHoursEnabled = false;
    
    @Column(name = "quiet_start_time")
    private LocalTime quietStartTime;
    
    @Column(name = "quiet_end_time")
    private LocalTime quietEndTime;
    
    @Column(name = "time_zone", length = 50)
    private String timeZone = "UTC";
    
    @Column(name = "frequency_limit_per_hour")
    private Integer frequencyLimitPerHour;
    
    @Column(name = "frequency_limit_per_day")
    private Integer frequencyLimitPerDay;
    
    @Column(name = "marketing_enabled", nullable = false)
    private Boolean marketingEnabled = false;
    
    @Column(name = "system_alerts_enabled", nullable = false)
    private Boolean systemAlertsEnabled = true;
    
    @Column(name = "trading_alerts_enabled", nullable = false)
    private Boolean tradingAlertsEnabled = true;
    
    @Column(name = "account_alerts_enabled", nullable = false)
    private Boolean accountAlertsEnabled = true;
    
    @Column(name = "language", length = 10)
    private String language = "en";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100, nullable = false)
    private String updatedBy;
    
    /**
     * Factory method for creating default preferences
     * 
     * MANDATORY: Factory Pattern - Rule #4
     */
    public static UserNotificationPreference createDefault(String userId, String createdBy) {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUserId(userId);
        preference.setNotificationsEnabled(true);
        preference.setPreferredChannel(NotificationRequest.NotificationType.EMAIL);
        preference.setEnabledChannels(Set.of(
            NotificationRequest.NotificationType.EMAIL,
            NotificationRequest.NotificationType.IN_APP
        ));
        preference.setEnabledCategories(Set.of(
            NotificationTemplate.TemplateCategory.SYSTEM,
            NotificationTemplate.TemplateCategory.TRADING,
            NotificationTemplate.TemplateCategory.ACCOUNT,
            NotificationTemplate.TemplateCategory.AUTHENTICATION
        ));
        preference.setQuietHoursEnabled(false);
        preference.setTimeZone("UTC");
        preference.setMarketingEnabled(false);
        preference.setSystemAlertsEnabled(true);
        preference.setTradingAlertsEnabled(true);
        preference.setAccountAlertsEnabled(true);
        preference.setLanguage("en");
        preference.setCreatedBy(createdBy);
        preference.setUpdatedBy(createdBy);
        return preference;
    }
    
    /**
     * Update preferences with functional approach
     * 
     * MANDATORY: Functional Programming - Rule #3
     */
    public UserNotificationPreference updatePreferences(
            Boolean notificationsEnabled,
            NotificationRequest.NotificationType preferredChannel,
            Set<NotificationRequest.NotificationType> enabledChannels,
            String updatedBy) {

        // Rule #3: NO if-else, use Optional.ifPresent() for conditional updates
        java.util.Optional.ofNullable(notificationsEnabled).ifPresent(this::setNotificationsEnabled);
        java.util.Optional.ofNullable(preferredChannel).ifPresent(this::setPreferredChannel);
        java.util.Optional.ofNullable(enabledChannels)
            .filter(channels -> !channels.isEmpty())
            .ifPresent(this::setEnabledChannels);

        this.setUpdatedBy(updatedBy);
        return this;
    }
    
    /**
     * Check if notification is allowed based on preferences
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, functional chain)
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 4
     */
    public boolean isNotificationAllowed(
            NotificationRequest.NotificationType type,
            NotificationTemplate.TemplateCategory category) {

        // Rule #3: NO if-else, use functional chain with Optional and filter
        return java.util.Optional.of(this.notificationsEnabled)
            .filter(Boolean::booleanValue)
            .filter(_ -> this.enabledChannels.contains(type))
            .filter(_ -> this.enabledCategories.contains(category))
            .map(_ -> switch (category) {
                case MARKETING -> this.marketingEnabled;
                case SYSTEM -> this.systemAlertsEnabled;
                case TRADING -> this.tradingAlertsEnabled;
                case ACCOUNT -> this.accountAlertsEnabled;
                default -> true;
            })
            .orElse(false);
    }
    
    /**
     * Check if current time is within quiet hours
     *
     * MANDATORY: Rule #3 - Functional Programming (NO if-else, pattern matching)
     * MANDATORY: Rule #14 - Pattern Matching with switch expression
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 3
     */
    public boolean isWithinQuietHours() {
        // Rule #3: NO if-else, use Optional chain with filter and map
        return java.util.Optional.of(this.quietHoursEnabled)
            .filter(Boolean::booleanValue)
            .flatMap(_ -> java.util.Optional.ofNullable(this.quietStartTime))
            .flatMap(start -> java.util.Optional.ofNullable(this.quietEndTime)
                .map(end -> {
                    LocalTime now = LocalTime.now();
                    // Rule #14: Pattern matching with switch on boolean
                    return switch (start.isBefore(end)) {
                        case true -> !now.isBefore(start) && !now.isAfter(end);
                        case false -> !now.isAfter(end) && !now.isBefore(start);
                    };
                }))
            .orElse(false);
    }
    
    /**
     * Get contact info for notification type
     */
    public String getContactInfo(NotificationRequest.NotificationType type) {
        return switch (type) {
            case EMAIL -> this.emailAddress;
            case SMS -> this.phoneNumber;
            case IN_APP -> this.userId;
            default -> this.userId;
        };
    }
}