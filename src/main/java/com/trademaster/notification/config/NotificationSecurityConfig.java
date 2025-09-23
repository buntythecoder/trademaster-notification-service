package com.trademaster.notification.config;

import com.trademaster.notification.security.ServiceApiKeyFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration for Notification Service
 * 
 * MANDATORY: Security First - Rule #6
 * MANDATORY: Zero Trust Security - Rule #6
 * MANDATORY: Spring Security Configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class NotificationSecurityConfig {

    private final ServiceApiKeyFilter serviceApiKeyFilter;
    
    /**
     * Security Filter Chain with Zero Trust approach
     * 
     * MANDATORY: Zero Trust Security - Rule #6
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS Configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Session Management - Stateless for microservices
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization Rules - Kong API key authentication for internal endpoints
            .authorizeHttpRequests(authz -> authz
                // Public endpoints for health monitoring
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Internal API endpoints - handled by ServiceApiKeyFilter
                .requestMatchers("/api/internal/**").permitAll()

                // All other requests must be authenticated via JWT
                .anyRequest().authenticated()
            )
            
            // Add ServiceApiKeyFilter before JWT filter
            .addFilterBefore(serviceApiKeyFilter, UsernamePasswordAuthenticationFilter.class)

            // OAuth2 Resource Server Configuration
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                )
            )
            
            // Security Headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            )
            
            .build();
    }
    
    /**
     * JWT Decoder Configuration
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // In production, this would use actual JWT issuer URL
        return NimbusJwtDecoder
            .withJwkSetUri("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
            .build();
    }
    
    /**
     * CORS Configuration
     * 
     * MANDATORY: Security Configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins - should be externalized
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "https://app.trademaster.com",
            "https://*.trademaster.com"
        ));
        
        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-User-ID",
            "X-Correlation-ID",
            "X-Request-ID",
            "Cache-Control"
        ));
        
        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-Request-ID",
            "X-Correlation-ID"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Max age
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        
        return source;
    }
    
    /**
     * Security Event Listener for Audit
     */
    @Bean
    public SecurityEventListener securityEventListener() {
        return new SecurityEventListener();
    }
    
    /**
     * Custom Security Event Listener
     */
    public static class SecurityEventListener {
        // Implementation would handle security events for audit
        // Events: authentication, authorization failures, etc.
    }
}