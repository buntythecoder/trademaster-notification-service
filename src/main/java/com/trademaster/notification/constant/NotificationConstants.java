package com.trademaster.notification.constant;

/**
 * Notification Service Constants
 * 
 * MANDATORY: Constants & Magic Numbers - Rule #17
 * MANDATORY: Zero Placeholders - Rule #7
 */
public final class NotificationConstants {
    
    // Notification Limits
    public static final int MAX_EMAIL_RECIPIENTS_BULK = 1000;
    public static final int MAX_SMS_RECIPIENTS_BULK = 500;
    public static final int MAX_PUSH_RECIPIENTS_BULK = 5000;
    public static final int MAX_SUBJECT_LENGTH = 200;
    public static final int MAX_CONTENT_LENGTH = 10000;
    public static final int MAX_TEMPLATE_NAME_LENGTH = 100;
    
    // Retry Configuration
    public static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    public static final int RETRY_DELAY_SECONDS = 5;
    public static final int MAX_RETRY_DELAY_SECONDS = 60;
    
    // Rate Limiting
    public static final int EMAIL_RATE_LIMIT_PER_HOUR = 1000;
    public static final int SMS_RATE_LIMIT_PER_HOUR = 100;
    public static final int PUSH_RATE_LIMIT_PER_HOUR = 10000;
    
    // Session Expiry
    public static final int NOTIFICATION_SESSION_HOURS = 24;
    public static final int AUDIT_RETENTION_DAYS = 90;
    public static final int ANALYTICS_DATA_RETENTION_DAYS = 365;
    
    // Template Configuration
    public static final String DEFAULT_EMAIL_TEMPLATE = "default";
    public static final String DEFAULT_SMS_TEMPLATE = "sms-default";
    public static final String WELCOME_EMAIL_TEMPLATE = "welcome";
    public static final String KYC_APPROVAL_TEMPLATE = "kyc-approved";
    public static final String TRADE_EXECUTION_TEMPLATE = "trade-execution";
    public static final String SECURITY_ALERT_TEMPLATE = "security-alert";
    
    // Queue Names
    public static final String EMAIL_NOTIFICATION_QUEUE = "email-notifications";
    public static final String SMS_NOTIFICATION_QUEUE = "sms-notifications";
    public static final String PUSH_NOTIFICATION_QUEUE = "push-notifications";
    public static final String BULK_NOTIFICATION_QUEUE = "bulk-notifications";
    
    // Kafka Topics
    public static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";
    public static final String NOTIFICATION_ANALYTICS_TOPIC = "notification-analytics";
    public static final String NOTIFICATION_DELIVERY_TOPIC = "notification-delivery";
    
    // HTTP Response Messages
    public static final String NOTIFICATION_SENT_SUCCESS = "Notification sent successfully";
    public static final String BULK_NOTIFICATIONS_QUEUED = "Bulk notifications queued for processing";
    public static final String TEMPLATE_CREATED_SUCCESS = "Template created successfully";
    public static final String PREFERENCES_UPDATED_SUCCESS = "User preferences updated successfully";
    
    // Error Messages
    public static final String INVALID_NOTIFICATION_TYPE = "Invalid notification type provided";
    public static final String RECIPIENT_REQUIRED = "Recipient is required";
    public static final String CONTENT_REQUIRED = "Content is required";
    public static final String TEMPLATE_NOT_FOUND = "Template not found";
    public static final String RATE_LIMIT_EXCEEDED = "Rate limit exceeded for notification type";
    public static final String DELIVERY_FAILED = "Notification delivery failed";
    
    // Database Table Names
    public static final String NOTIFICATIONS_TABLE = "notifications";
    public static final String NOTIFICATION_TEMPLATES_TABLE = "notification_templates";
    public static final String USER_PREFERENCES_TABLE = "user_notification_preferences";
    public static final String NOTIFICATION_ANALYTICS_TABLE = "notification_analytics";
    public static final String NOTIFICATION_AUDIT_TABLE = "notification_audit";
    
    // Priority Levels
    public static final int LOW_PRIORITY_LEVEL = 1;
    public static final int MEDIUM_PRIORITY_LEVEL = 2;
    public static final int HIGH_PRIORITY_LEVEL = 3;
    public static final int URGENT_PRIORITY_LEVEL = 4;
    
    // Delivery Status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    
    private NotificationConstants() {
        // Utility class - prevent instantiation
    }
}