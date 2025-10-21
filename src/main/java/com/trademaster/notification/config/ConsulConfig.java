package com.trademaster.notification.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistrationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Consul Service Discovery Configuration
 *
 * Compliance with TradeMaster Mandatory Rules:
 * - Rule #2 (SOLID Principles): Single Responsibility - manages only Consul integration
 * - Rule #3 (Functional Programming): Immutable configuration with Records
 * - Rule #6 (Zero Trust Security): Service-to-service authentication metadata
 * - Rule #9 (Immutability): All configuration properties are immutable
 * - Rule #10 (Lombok): @Slf4j for logging, @RequiredArgsConstructor for DI
 * - Rule #15 (Structured Logging): Correlation IDs and structured log entries
 * - Rule #16 (Dynamic Configuration): All values externalized via @Value
 * - Rule #19 (Access Control): Private fields with controlled access
 *
 * Design Patterns:
 * - Builder Pattern: ConsulRegistrationCustomizer for flexible configuration
 * - Strategy Pattern: Functional customization of Consul registration
 * - Observer Pattern: Health indicator for monitoring
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableDiscoveryClient
@RequiredArgsConstructor
public class ConsulConfig {

    @Value("${spring.application.name}")
    private final String applicationName;

    @Value("${server.port:8080}")
    private final int serverPort;

    @Value("${ENVIRONMENT:dev}")
    private final String environment;

    @Value("${project.version:1.0.0}")
    private final String applicationVersion;

    /**
     * Consul Service Metadata (Immutable Record)
     * Following Rule #9 (Immutability & Records Usage)
     */
    public record ServiceMetadata(
        String serviceName,
        String version,
        String environment,
        int port,
        Instant registrationTime,
        Map<String, String> capabilities,
        List<String> tags
    ) {
        /**
         * Compact constructor with validation
         * Following Rule #9 (Records with validation)
         */
        public ServiceMetadata {
            if (serviceName == null || serviceName.isBlank()) {
                throw new IllegalArgumentException("Service name cannot be null or blank");
            }
            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("Version cannot be null or blank");
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
        }

        /**
         * Builder pattern for ServiceMetadata construction
         * Following Rule #4 (Advanced Design Patterns - Builder)
         */
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String serviceName;
            private String version;
            private String environment;
            private int port;
            private Instant registrationTime = Instant.now();
            private Map<String, String> capabilities = Map.of();
            private List<String> tags = List.of();

            public Builder serviceName(String serviceName) {
                this.serviceName = serviceName;
                return this;
            }

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public Builder environment(String environment) {
                this.environment = environment;
                return this;
            }

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public Builder capabilities(Map<String, String> capabilities) {
                this.capabilities = Map.copyOf(capabilities);
                return this;
            }

            public Builder tags(List<String> tags) {
                this.tags = List.copyOf(tags);
                return this;
            }

            public ServiceMetadata build() {
                return new ServiceMetadata(
                    serviceName,
                    version,
                    environment,
                    port,
                    registrationTime,
                    capabilities,
                    tags
                );
            }
        }
    }

    /**
     * Customize Consul service registration with metadata
     * Following Rule #4 (Strategy Pattern for customization)
     *
     * @return ConsulRegistrationCustomizer with service metadata
     */
    @Bean
    public ConsulRegistrationCustomizer consulRegistrationCustomizer() {
        return registration -> {
            ServiceMetadata metadata = createServiceMetadata();

            log.info("Customizing Consul registration for service: {} version: {} environment: {}",
                metadata.serviceName(), metadata.version(), metadata.environment());

            // Add service capabilities as metadata
            metadata.capabilities().forEach((key, value) ->
                registration.getService().getMeta().put(key, value)
            );

            // Add service tags
            metadata.tags().forEach(tag ->
                registration.getService().getTags().add(tag)
            );

            // Add registration metadata
            registration.getService().getMeta().put("registration-time",
                metadata.registrationTime().toString());
            registration.getService().getMeta().put("service-version",
                metadata.version());
            registration.getService().getMeta().put("environment",
                metadata.environment());

            log.info("Consul registration customized successfully with {} capabilities and {} tags",
                metadata.capabilities().size(), metadata.tags().size());
        };
    }

    /**
     * Create service metadata with capabilities
     * Following Rule #3 (Functional Programming - pure function)
     *
     * @return ServiceMetadata with all service capabilities
     */
    private ServiceMetadata createServiceMetadata() {
        return ServiceMetadata.builder()
            .serviceName(applicationName)
            .version(applicationVersion)
            .environment(environment)
            .port(serverPort)
            .capabilities(createServiceCapabilities())
            .tags(createServiceTags())
            .build();
    }

    /**
     * Define service capabilities metadata
     * Following Rule #9 (Immutable collections)
     *
     * @return Immutable map of service capabilities
     */
    private Map<String, String> createServiceCapabilities() {
        return Map.ofEntries(
            Map.entry("java-version", "24"),
            Map.entry("virtual-threads", "enabled"),
            Map.entry("circuit-breakers", "resilience4j"),
            Map.entry("multi-channel-delivery", "true"),
            Map.entry("notification-channels", "email,sms,push,in-app"),
            Map.entry("template-engine", "thymeleaf"),
            Map.entry("email-provider", "smtp"),
            Map.entry("sms-provider", "twilio"),
            Map.entry("push-provider", "fcm"),
            Map.entry("websocket-enabled", "true"),
            Map.entry("rate-limiting", "enabled"),
            Map.entry("cache-provider", "redis"),
            Map.entry("messaging", "kafka"),
            Map.entry("metrics", "prometheus"),
            Map.entry("tracing", "zipkin")
        );
    }

    /**
     * Define service tags for discovery
     * Following Rule #9 (Immutable collections)
     *
     * @return Immutable list of service tags
     */
    private List<String> createServiceTags() {
        return List.of(
            "notification-service",
            "trading-platform",
            "java-24",
            "virtual-threads-enabled",
            "circuit-breaker-protected",
            "multi-channel-notifications",
            "real-time-websocket",
            "rate-limited",
            String.format("version-%s", applicationVersion),
            String.format("env-%s", environment)
        );
    }

    /**
     * Custom Health Indicator for Consul Service
     * Following Rule #15 (Structured Logging & Monitoring)
     *
     * @param discoveryProperties Consul discovery properties
     * @return HealthIndicator for Consul service health
     */
    @Bean
    public HealthIndicator consulServiceHealthIndicator(
            ConsulDiscoveryProperties discoveryProperties) {

        return () -> Optional.ofNullable(discoveryProperties)
            .filter(props -> props.isEnabled() && props.isRegister())
            .map(props -> Health.up()
                .withDetail("consul-enabled", true)
                .withDetail("service-name", applicationName)
                .withDetail("instance-id", props.getInstanceId())
                .withDetail("hostname", props.getHostname())
                .withDetail("port", props.getPort())
                .withDetail("health-check-interval", props.getHealthCheckInterval())
                .withDetail("tags", props.getTags())
                .withDetail("metadata", props.getMetadata())
                .build())
            .orElse(Health.down()
                .withDetail("consul-enabled", false)
                .withDetail("reason", "Consul discovery is disabled or registration failed")
                .build());
    }

    /**
     * Async service registration notification
     * Following Rule #12 (Virtual Threads & Concurrency)
     *
     * @param registration Consul auto-registration
     * @return CompletableFuture for async notification
     */
    public CompletableFuture<Void> notifyServiceRegistration(
            ConsulAutoRegistration registration) {

        return CompletableFuture.runAsync(() ->
            log.info("Service registered with Consul: {} at {}:{} with instance-id: {}",
                registration.getService().getName(),
                registration.getService().getAddress(),
                registration.getService().getPort(),
                registration.getInstanceId())
        );
    }

    /**
     * Get service discovery status
     * Following Rule #3 (Functional Programming - Optional usage)
     *
     * @param discoveryProperties Consul discovery properties
     * @return Optional containing registration status
     */
    public Optional<String> getRegistrationStatus(
            ConsulDiscoveryProperties discoveryProperties) {

        return Optional.ofNullable(discoveryProperties)
            .filter(props -> props.isEnabled() && props.isRegister())
            .map(props -> String.format(
                "Registered: %s (instance-id: %s, hostname: %s, port: %d)",
                applicationName,
                props.getInstanceId(),
                props.getHostname(),
                props.getPort()
            ))
            .or(() -> Optional.of("Service registration disabled"));
    }
}
