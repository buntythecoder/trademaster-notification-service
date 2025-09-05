package com.trademaster.notification.config;

import com.trademaster.notification.logging.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration for Notification Service
 * 
 * MANDATORY: Web Configuration - TradeMaster Standards
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Structured Logging - Rule #15
 * 
 * Configures web interceptors, CORS, and security settings
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    /**
     * Register interceptors for request/response processing
     * 
     * MANDATORY: Structured Logging - Rule #15
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/actuator/**",
                "/ops/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico",
                "/error"
            );
    }

    /**
     * Configure CORS for cross-origin requests
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(
                "https://*.trademaster.com",
                "https://localhost:*",
                "https://127.0.0.1:*"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Correlation-ID",
                "X-User-ID",
                "Accept",
                "Origin"
            )
            .exposedHeaders(
                "X-Correlation-ID",
                "X-Total-Count",
                "X-Page-Count"
            )
            .allowCredentials(true)
            .maxAge(3600);
    }
}