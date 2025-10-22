package com.trademaster.notification.agentos;

/**
 * AgentOS Constants for Notification Service
 *
 * Centralized constants for the AgentOS framework integration following
 * TradeMaster Rule #17 - All magic numbers and strings must be extracted to constants.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #17: Constants & Magic Numbers
 * - Rule #21: Code Organization
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
public final class AgentConstants {

    // Prevent instantiation (Rule #2: Single Responsibility)
    private AgentConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Agent Identification Constants
    public static final String AGENT_ID = "notification-agent";
    public static final String AGENT_TYPE = "NOTIFICATION";

    // Agent Capabilities
    public static final String CAPABILITY_EMAIL_NOTIFICATION = "EMAIL_NOTIFICATION";
    public static final String CAPABILITY_SMS_NOTIFICATION = "SMS_NOTIFICATION";
    public static final String CAPABILITY_PUSH_NOTIFICATION = "PUSH_NOTIFICATION";
    public static final String CAPABILITY_IN_APP_NOTIFICATION = "IN_APP_NOTIFICATION";
    public static final String CAPABILITY_TEMPLATE_MANAGEMENT = "TEMPLATE_MANAGEMENT";
    public static final String CAPABILITY_PREFERENCE_CHECK = "PREFERENCE_CHECK";
    public static final String CAPABILITY_NOTIFICATION_HISTORY = "NOTIFICATION_HISTORY";
    public static final String CAPABILITY_BATCH_NOTIFICATIONS = "BATCH_NOTIFICATIONS";

    // Proficiency Levels
    public static final String PROFICIENCY_BEGINNER = "BEGINNER";
    public static final String PROFICIENCY_INTERMEDIATE = "INTERMEDIATE";
    public static final String PROFICIENCY_ADVANCED = "ADVANCED";
    public static final String PROFICIENCY_EXPERT = "EXPERT";

    // Performance Profiles
    public static final String PERFORMANCE_STANDARD = "STANDARD";
    public static final String PERFORMANCE_HIGH = "HIGH";
    public static final String PERFORMANCE_CRITICAL = "CRITICAL";

    // Health Score Thresholds
    public static final double HEALTH_SCORE_EXCELLENT = 0.9;
    public static final double HEALTH_SCORE_GOOD = 0.7;
    public static final double HEALTH_SCORE_WARNING = 0.5;
    public static final double HEALTH_SCORE_CRITICAL = 0.3;

    // Timeout Constants (milliseconds)
    public static final int DEFAULT_OPERATION_TIMEOUT_MS = 10000;
    public static final int HEALTH_CHECK_INTERVAL_MS = 30000;
    public static final int CAPABILITY_METRICS_INTERVAL_MS = 60000;
    public static final int EMAIL_SEND_TIMEOUT_MS = 5000;
    public static final int SMS_SEND_TIMEOUT_MS = 3000;
    public static final int PUSH_SEND_TIMEOUT_MS = 2000;

    // Default Event Handler Priority
    public static final int DEFAULT_EVENT_PRIORITY = 0;
    public static final int HIGH_EVENT_PRIORITY = 10;
    public static final int CRITICAL_EVENT_PRIORITY = 20;

    // MCP Method Names
    public static final String MCP_SEND_NOTIFICATION = "notification.send";
    public static final String MCP_GET_TEMPLATES = "notification.templates.list";
    public static final String MCP_CHECK_PREFERENCES = "notification.preferences.check";
    public static final String MCP_GET_CAPABILITIES = "notification.capabilities";
    public static final String MCP_HEALTH_CHECK = "notification.health";
    public static final String MCP_SEND_BATCH = "notification.send.batch";
}
