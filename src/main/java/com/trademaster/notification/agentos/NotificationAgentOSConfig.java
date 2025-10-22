package com.trademaster.notification.agentos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Notification Agent OS Configuration
 *
 * Spring configuration for AgentOS integration. Initializes the notification agent
 * and registers it with the Agent Orchestration Service on application startup.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #1: Java 24 Virtual Threads for async operations
 * - Rule #2: Single Responsibility - Agent initialization only
 * - Rule #3: Functional Programming - NO if-else statements
 * - Rule #5: Cognitive Complexity ≤7 per method
 * - Rule #7: Zero TODOs - production ready implementation
 * - Rule #10: Lombok - @Slf4j, @RequiredArgsConstructor
 * - Rule #16: Dynamic Configuration - externalized config
 *
 * Lifecycle:
 * 1. Application startup
 * 2. ApplicationReadyEvent triggered
 * 3. NotificationAgent initialized and registered
 * 4. Capability registry initialized
 * 5. Health checks scheduled
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationAgentOSConfig {

    private final NotificationAgent notificationAgent;
    private final NotificationCapabilityRegistry capabilityRegistry;

    /**
     * Initializes Agent OS integration on application startup
     *
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * Complexity: 2 (well below limit of 7)
     *
     * @param event Application ready event triggered by Spring Boot
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAgentOS(ApplicationReadyEvent event) {
        log.info("=== Initializing TradeMaster Notification Agent OS ===");

        // Initialize capability registry
        capabilityRegistry.initializeCapabilities();
        log.info("Capability registry initialized with {} capabilities",
                notificationAgent.getCapabilities().size());

        // Trigger agent registration
        notificationAgent.onRegistration();

        // Log agent metadata
        logAgentMetadata();

        log.info("=== Notification Agent OS Initialization Complete ===");
    }

    /**
     * Logs agent metadata for monitoring and debugging
     *
     * MANDATORY: Rule #5 - Cognitive Complexity ≤7
     * MANDATORY: Rule #15 - Structured Logging
     * Complexity: 1
     */
    private void logAgentMetadata() {
        log.info("Agent Metadata: " +
                "agentId={}, agentType={}, capabilities={}, healthScore={}",
                notificationAgent.getAgentId(),
                notificationAgent.getAgentType(),
                notificationAgent.getCapabilities(),
                notificationAgent.getHealthScore()
        );
    }
}
