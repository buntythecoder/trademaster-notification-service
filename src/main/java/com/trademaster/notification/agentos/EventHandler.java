package com.trademaster.notification.agentos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.trademaster.notification.agentos.AgentConstants.*;

/**
 * Event Handler Annotation
 *
 * Marks methods that handle specific events in the AgentOS framework.
 * The Agent Orchestration Service uses this annotation to route events
 * to the appropriate handler methods.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility - Each handler processes ONE event type
 * - Rule #21: Code Organization - Annotation for event routing
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /**
     * The event type this handler processes
     */
    String event();

    /**
     * Priority level for this handler (higher values = higher priority)
     */
    int priority() default DEFAULT_EVENT_PRIORITY;

    /**
     * Whether this handler should be called asynchronously
     */
    boolean async() default true;

    /**
     * Maximum processing time in milliseconds before timeout
     */
    long timeoutMs() default DEFAULT_OPERATION_TIMEOUT_MS;
}
