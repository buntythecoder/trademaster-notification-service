package com.trademaster.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Kong API Gateway Configuration
 *
 * Configures Kong Gateway integration for API management, rate limiting,
 * authentication, and monitoring. Implements TradeMaster standards for
 * API gateway patterns.
 *
 * Features:
 * - Service and route configuration
 * - Plugin management for security and monitoring
 * - Rate limiting and CORS configuration
 * - API key authentication
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "kong.gateway")
@Data
public class KongConfiguration {

    private boolean enabled = true;
    private String adminUrl = "http://localhost:8001";
    private String proxyUrl = "http://localhost:8000";

    private ServiceConfig service;
    private RouteConfig route;
    private List<PluginConfig> plugins;

    @Data
    public static class ServiceConfig {
        private String name;
        private String url;
        private String path;
        private int connectTimeout = 60000;
        private int writeTimeout = 60000;
        private int readTimeout = 60000;
        private int retries = 5;
    }

    @Data
    public static class RouteConfig {
        private String name;
        private List<String> paths;
        private List<String> methods;
        private boolean stripPath = true;
        private boolean preserveHost = false;
    }

    @Data
    public static class PluginConfig {
        private String name;
        private Map<String, Object> config;
        private boolean enabled = true;
    }
}