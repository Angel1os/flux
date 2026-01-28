package com.angellos.market.data.service.config;

import com.angellos.market.data.service.interceptor.WebSocketAuthInterceptor;
import com.angellos.market.data.service.interceptor.WebSocketHandshakeInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for Market Data Service.
 * Configures STOMP over WebSocket for real-time price streaming.
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor,
                          WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.authInterceptor = authInterceptor;
        this.handshakeInterceptor = handshakeInterceptor;
        log.info("WebSocketConfig initialized with auth interceptor: {}, handshake interceptor: {}", 
                authInterceptor != null, handshakeInterceptor != null);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for broadcasting prices
        config.enableSimpleBroker("/topic", "/queue");
        // Set prefix for messages sent from client to server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering WebSocket endpoint: /ws/prices with handshake interceptor");
        
        // Register endpoint with SockJS for browser compatibility
        registry.addEndpoint("/ws/prices")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*") // Configure properly in production
                .withSockJS();

        // Native WebSocket endpoint (no SockJS) - useful for Postman / CLI websocket clients
        // Use: ws://localhost:8082/ws/prices-native?token=JWT
        registry.addEndpoint("/ws/prices-native")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
        
        log.info("WebSocket endpoint registered with SockJS support");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register channel interceptor for additional message-level validation
        registration.interceptors(authInterceptor);
    }
}
