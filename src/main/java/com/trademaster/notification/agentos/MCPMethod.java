package com.trademaster.notification.agentos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP Method Annotation
 *
 * Marks methods that are exposed via the Multi-Agent Communication Protocol (MCP).
 * These methods can be invoked by other agents in the TradeMaster ecosystem.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #2: Single Responsibility - Each MCP method performs ONE operation
 * - Rule #21: Code Organization - MCP protocol integration
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MCPMethod {

    /**
     * The MCP method name (e.g., "notification.send")
     */
    String value();
}
