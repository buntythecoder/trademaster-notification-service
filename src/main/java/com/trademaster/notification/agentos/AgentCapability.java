package com.trademaster.notification.agentos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent Capability Annotation for Notification Service
 *
 * Marks methods that represent agent capabilities in the AgentOS framework.
 * Used by the Agent Orchestration Service for capability discovery,
 * load balancing, and task routing.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility - Each capability represents ONE specific function
 * - Rule #21: Code Organization - Annotation for framework integration
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentCapability {

    /**
     * The name of the capability this method provides
     */
    String name();

    /**
     * The proficiency level of this capability
     * Values: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
     */
    String proficiency() default "INTERMEDIATE";

    /**
     * Whether this capability is currently active
     */
    boolean isActive() default true;

    /**
     * Resource requirements for this capability (CPU, memory, etc.)
     */
    String[] resourceRequirements() default {};

    /**
     * Dependencies required for this capability to function
     */
    String[] dependencies() default {};

    /**
     * Performance characteristics of this capability
     * Values: STANDARD, HIGH, CRITICAL
     */
    String performanceProfile() default "STANDARD";
}
