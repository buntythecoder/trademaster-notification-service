package com.trademaster.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Documentation Configuration
 *
 * Configures Swagger/OpenAPI documentation for the Notification Service.
 * Implements TradeMaster standards for API documentation and security schemas.
 *
 * Features:
 * - Comprehensive API documentation
 * - JWT Bearer token authentication
 * - API key authentication support
 * - Server configuration for different environments
 * - Security requirements and schemas
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfiguration {

    @Value("${server.port:8084}")
    private int serverPort;

    @Value("${spring.application.name:notification-service}")
    private String applicationName;

    @Value("${kong.gateway.proxy-url:http://localhost:8000}")
    private String kongProxyUrl;

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development Server"),
                new Server()
                    .url(kongProxyUrl + "/notification")
                    .description("Kong API Gateway"),
                new Server()
                    .url("https://api.trademaster.com/notification")
                    .description("Production Server")
            ))
            .addSecurityItem(new SecurityRequirement()
                .addList("Bearer Authentication")
                .addList("API Key Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", bearerTokenSchema())
                .addSecuritySchemes("API Key Authentication", apiKeySchema()));
    }

    private Info apiInfo() {
        return new Info()
            .title("TradeMaster Notification Service API")
            .description("""
                Comprehensive notification service for the TradeMaster trading platform.

                Features:
                - Multi-channel notifications (Email, SMS, In-App, Push)
                - Real-time WebSocket notifications
                - Template-based messaging
                - Rate limiting and throttling
                - Audit trail and delivery tracking
                - Circuit breaker patterns for reliability

                Security:
                - JWT Bearer token authentication for internal services
                - API key authentication for external clients
                - Kong API Gateway integration
                - Rate limiting and CORS protection
                """)
            .version("2.0.0")
            .contact(new Contact()
                .name("TradeMaster Development Team")
                .email("dev@trademaster.com")
                .url("https://trademaster.com"))
            .license(new License()
                .name("TradeMaster Proprietary License")
                .url("https://trademaster.com/license"));
    }

    private SecurityScheme bearerTokenSchema() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT Bearer token authentication for internal service communication");
    }

    private SecurityScheme apiKeySchema() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-API-Key")
            .description("API key authentication for external clients via Kong Gateway");
    }
}