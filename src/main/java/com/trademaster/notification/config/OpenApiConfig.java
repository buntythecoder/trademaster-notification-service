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
 * OpenAPI 3.0 Configuration for Notification Service API Documentation
 * 
 * MANDATORY: API Documentation - Production Readiness
 * MANDATORY: Security Documentation - Rule #6
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8084}")
    private String serverPort;
    
    @Value("${spring.application.name:notification-service}")
    private String applicationName;
    
    /**
     * Configure OpenAPI documentation
     */
    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Development Server"),
                new Server()
                    .url("https://api-dev.trademaster.com")
                    .description("Development Environment"),
                new Server()
                    .url("https://api.trademaster.com")
                    .description("Production Environment")
            ))
            .components(securityComponents())
            .security(List.of(
                new SecurityRequirement().addList("bearer-jwt"),
                new SecurityRequirement().addList("api-key")
            ));
    }
    
    private Info apiInfo() {
        return new Info()
            .title("TradeMaster Notification Service API")
            .description("""
                # TradeMaster Notification Service
                
                ## Overview
                The Notification Service provides comprehensive multi-channel notification capabilities 
                for the TradeMaster trading platform, including:
                
                - **Multi-Channel Support**: Email, SMS, Push, In-App notifications
                - **Template Management**: Dynamic template creation and versioning
                - **User Preferences**: Granular notification control per user
                - **Real-Time Delivery**: WebSocket-based instant notifications
                - **Analytics & Reporting**: Comprehensive notification metrics
                
                ## Authentication
                This API uses JWT Bearer tokens for authentication. Include your token in the 
                `Authorization` header as `Bearer <token>`.
                
                ## Rate Limiting
                API calls are rate-limited per user:
                - **Standard users**: 100 requests/minute
                - **Premium users**: 1000 requests/minute
                - **Admin users**: 10000 requests/minute
                
                ## WebSocket Support
                Real-time notifications are available via WebSocket at `/ws/notifications`.
                
                ## Error Handling
                All errors follow a consistent format with appropriate HTTP status codes.
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("TradeMaster API Support")
                .email("api-support@trademaster.com")
                .url("https://docs.trademaster.com"))
            .license(new License()
                .name("TradeMaster API License")
                .url("https://trademaster.com/api-license"));
    }
    
    private Components securityComponents() {
        return new Components()
            .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer token authentication")
            )
            .addSecuritySchemes("api-key", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API Key authentication for service-to-service calls")
            );
    }
}