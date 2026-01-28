package com.angellos.trading.service.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Intercepts WebSocket messages to authenticate using JWT tokens.
 * Extracts token from STOMP headers or query parameters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = new JwtAuthenticationConverter();
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("=== WebSocket CONNECT Message Received ===");
            
            // Check if authentication was already set during handshake (stored in session attributes)
            // The handshake interceptor stores it as "user" in attributes
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Object userObj = sessionAttributes.get("user");
                if (userObj instanceof Authentication) {
                    Authentication existingAuth = (Authentication) userObj;
                    if (existingAuth.isAuthenticated()) {
                        log.info("WebSocket CONNECT: Using authentication from handshake for user: {}", existingAuth.getName());
                        accessor.setUser(existingAuth);
                        return message; // Already authenticated, proceed
                    }
                }
                
                // Also check for token stored in attributes
                Object tokenObj = sessionAttributes.get("token");
                if (tokenObj instanceof String) {
                    String token = (String) tokenObj;
                    log.info("Found token in session attributes, validating...");
                    try {
                        Jwt jwt = jwtDecoder.decode(token);
                        Authentication auth = jwtAuthenticationConverter.convert(jwt);
                        accessor.setUser(auth);
                        sessionAttributes.put("user", auth); // Update with auth object
                        log.info("WebSocket CONNECT: Authenticated user from stored token: {}", auth.getName());
                        return message;
                    } catch (Exception e) {
                        log.error("Failed to validate token from session attributes: {}", e.getMessage());
                        throw new SecurityException("Invalid JWT token");
                    }
                }
            }

            // If not authenticated from handshake, try to extract token from multiple sources
            String token = extractToken(accessor, sessionAttributes);
            log.info("Token extracted: {}", token != null && !token.isEmpty() ? "present (length: " + token.length() + ")" : "missing");

            if (StringUtils.hasText(token)) {
                try {
                    log.info("WebSocket CONNECT: Validating token from STOMP headers");
                    // Decode and validate JWT
                    Jwt jwt = jwtDecoder.decode(token);

                    // Convert to Spring Security Authentication
                    Authentication auth = jwtAuthenticationConverter.convert(jwt);

                    // Set authentication in session
                    accessor.setUser(auth);

                    log.info("WebSocket authenticated user: {}", auth.getName());
                } catch (Exception e) {
                    log.error("WebSocket authentication failed: {}", e.getMessage());
                    // Reject connection - you can throw exception or set error
                    throw new SecurityException("Invalid JWT token");
                }
            } else {
                log.warn("No JWT token provided in WebSocket CONNECT message and no handshake authentication found");
                throw new SecurityException("Authentication required");
            }
        }

        return message;
    }

    /**
     * Extracts JWT token from multiple sources.
     * Priority: 
     * 1. Session attributes (from handshake interceptor)
     * 2. STOMP Authorization header
     * 3. STOMP native header "token"
     * 4. WebSocket session URI query parameter
     */
    private String extractToken(StompHeaderAccessor accessor, Map<String, Object> sessionAttributes) {
        // 1. Check session attributes first (stored by handshake interceptor)
        if (sessionAttributes != null) {
            Object tokenObj = sessionAttributes.get("token");
            if (tokenObj instanceof String && StringUtils.hasText((String) tokenObj)) {
                log.info("Token found in session attributes");
                return (String) tokenObj;
            }
        }
        
        // 2. Try Authorization header from STOMP headers (Bearer token)
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                log.info("Token found in STOMP Authorization header");
                return authHeader.substring(7);
            }
        }

        // 3. Try token from STOMP native headers
        String token = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(token)) {
            log.info("Token found in STOMP native headers");
            return token;
        }
        
        // 4. Try to extract from WebSocket session URI (query parameter)
        // The session ID can be used to get the session, but we need a different approach
        // Actually, the query parameters should have been stored during handshake
        // But if handshake interceptor wasn't called, we can try to parse from session attributes
        
        // Check if there's a session URI stored
        if (sessionAttributes != null) {
            Object uriObj = sessionAttributes.get("uri");
            if (uriObj instanceof String) {
                try {
                    URI uri = new URI((String) uriObj);
                    String query = uri.getQuery();
                    if (StringUtils.hasText(query)) {
                        // Parse query string for token parameter
                        String[] params = query.split("&");
                        for (String param : params) {
                            String[] keyValue = param.split("=", 2);
                            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                                log.info("Token found in session URI query parameter");
                                return keyValue[1];
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse URI from session attributes: {}", e.getMessage());
                }
            }
        }

        log.warn("No token found in any source");
        return null;
    }

}
