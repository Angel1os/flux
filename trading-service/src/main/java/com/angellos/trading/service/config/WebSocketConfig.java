package com.angellos.trading.service.config;

import com.angellos.trading.service.interceptor.WebSocketAuthInterceptor;
import com.angellos.trading.service.interceptor.WebSocketHandshakeInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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
        log.info("WebSocketConfig initialized with handshake interceptor: {}", handshakeInterceptor != null);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering WebSocket endpoint: /ws/orders with handshake interceptor");
        
        // Register endpoint with SockJS for browser compatibility
        // SockJS provides fallback transports for browsers that don't support WebSocket
        registry.addEndpoint("/ws/orders")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*") // Configure properly in production
                .withSockJS();
        
        log.info("WebSocket endpoint registered with SockJS support");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register channel interceptor for additional message-level validation
        registration.interceptors(authInterceptor);
    }
}
