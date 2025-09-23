package com.trademaster.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * TradeMaster Notification Service Application
 *
 * Main application class for the notification service implementing
 * TradeMaster Golden Specification patterns.
 *
 * Features:
 * - Multi-channel notifications (Email, SMS, Push, In-App)
 * - Real-time WebSocket communication
 * - Consul service discovery integration
 * - Kong API Gateway compatibility
 * - Circuit breaker patterns for resilience
 * - Comprehensive monitoring and observability
 *
 * Architecture:
 * - Spring Boot 3.5+ with Virtual Threads
 * - Java 24 with preview features enabled
 * - Functional programming patterns
 * - Zero Trust security model
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@SpringBootApplication(exclude = {
    // MANDATORY: Exclude deprecated auto-configurations per TradeMaster Standards
    org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration.class
})
@EnableFeignClients
@EnableKafka
public class NotificationServiceApplication {

    public static void main(String[] args) {
        // MANDATORY: Enable Virtual Threads per TradeMaster Standards
        System.setProperty("spring.threads.virtual.enabled", "true");

        SpringApplication app = new SpringApplication(NotificationServiceApplication.class);

        // MANDATORY: Java 24 Preview Features per TradeMaster Standards
        app.setDefaultProperties(java.util.Map.of(
            "spring.jmx.enabled", "true",
            "management.endpoints.jmx.exposure.include", "*",
            "spring.threads.virtual.enabled", "true"
        ));

        app.run(args);
    }
}