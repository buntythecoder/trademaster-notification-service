package com.trademaster.notification.agentos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP Parameter Annotation
 *
 * Marks parameters in MCP methods for protocol-based parameter mapping.
 * Enables automatic parameter resolution from MCP request payloads.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #21: Code Organization - MCP protocol parameter mapping
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MCPParam {

    /**
     * The parameter name in the MCP request payload
     */
    String value();
}
