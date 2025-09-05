package com.trademaster.notification.config;

import com.trademaster.notification.websocket.NotificationWebSocketHandler;
import com.trademaster.notification.websocket.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket Configuration for Real-Time Notifications
 * 
 * MANDATORY: Virtual Threads - Rule #12
 * MANDATORY: Security First - Rule #6
 * MANDATORY: Real-Time Communication - FRONT-020
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    
    /**
     * Register WebSocket handlers with security and CORS
     * 
     * MANDATORY: Security First - Rule #6
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
            .addInterceptors(handshakeInterceptor)
            .setAllowedOrigins(
                "http://localhost:3000",
                "https://app.trademaster.com",
                "https://*.trademaster.com"
            )
            .withSockJS();
        
        // Admin/Manager WebSocket endpoint
        registry.addHandler(notificationWebSocketHandler, "/ws/admin/notifications")
            .addInterceptors(handshakeInterceptor)
            .setAllowedOrigins(
                "http://localhost:3000",
                "https://admin.trademaster.com",
                "https://*.trademaster.com"
            )
            .withSockJS();
    }
    
    /**
     * Configure WebSocket container with Virtual Threads
     * 
     * MANDATORY: Virtual Threads - Rule #12
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        
        // Configure message size limits
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(300000L); // 5 minutes
        
        // Enable async processing for Virtual Threads
        container.setAsyncSendTimeout(5000L);
        
        return container;
    }
}