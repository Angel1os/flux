package com.angellos.trading.service.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Intercepts WebSocket handshake to extract and validate JWT token from query parameters or headers.
 * This validates authentication at the connection level before the WebSocket is established.
 */
@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public WebSocketHandshakeInterceptor(JwtDecoder jwtDecoder, 
                                        JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        log.info("WebSocketHandshakeInterceptor initialized");
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        String uri = request.getURI().toString();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        log.info("=== WebSocket Handshake Interceptor Called ===");
        log.info("Method: {}, URI: {}", method, uri);
        log.info("Request class: {}", request.getClass().getName());
        
        // Allow SockJS info requests (they don't have tokens)
        // SockJS makes these requests: /info, /websocket, /xhr, /xhr_streaming, etc.
        // But note: /websocket is also the native WebSocket transport, so we need to check for token
        if (uri.contains("/info") || uri.contains("/xhr") || uri.contains("/xhr_streaming") ||
            uri.contains("/xhr_send") || uri.contains("/jsonp")) {
            log.info("Allowing SockJS metadata request: {}", uri);
            return true;
        }
        
        // For WebSocket transport, extract token and store it
        // Validation will happen at channel level (STOMP CONNECT message)
        // This allows SockJS to establish connection, then we validate
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // Extract token from query parameter (e.g., ?token=xxx)
            String token = servletRequest.getServletRequest().getParameter("token");
            log.info("Token from query param: {}", token != null && !token.isEmpty() ? "present (length: " + token.length() + ")" : "missing");
            
            // Or from Authorization header
            if (!StringUtils.hasText(token)) {
                String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
                log.info("Authorization header: {}", authHeader != null ? "present" : "missing");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            
            // If token is present, try to validate and store it
            // If validation fails, still allow connection - channel interceptor will reject CONNECT
            if (StringUtils.hasText(token)) {
                try {
                    log.info("Pre-validating JWT token at handshake...");
                    // Validate JWT
                    Jwt jwt = jwtDecoder.decode(token);
                    log.info("JWT decoded successfully, subject: {}", jwt.getSubject());
                    
                    // Convert to Spring Security Authentication
                    Authentication auth = jwtAuthenticationConverter.convert(jwt);
                    log.info("Authentication created, name: {}, authorities: {}", auth.getName(), auth.getAuthorities());
                    
                    // Store authentication in attributes for later use in the session
                    attributes.put("user", auth);
                    attributes.put("token", token); // Also store raw token
                    attributes.put("uri", uri); // Store URI for token extraction if needed
                    
                    log.info("WebSocket handshake pre-authenticated user: {}", auth.getName());
                } catch (Exception e) {
                    log.warn("WebSocket handshake token validation failed (will validate at channel level): {}", e.getMessage());
                    // Store token and URI anyway - channel interceptor will handle validation
                    attributes.put("token", token);
                    attributes.put("uri", uri);
                }
            } else {
                log.warn("No token provided in WebSocket handshake. URI: {}. Will validate at channel level.", uri);
                // Store URI anyway in case token is in query string but not extracted properly
                attributes.put("uri", uri);
            }
        }
        
        // Always allow handshake - validation happens at channel level on STOMP CONNECT
        log.info("Allowing WebSocket handshake (validation deferred to channel interceptor)");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Cleanup if needed
        if (exception != null) {
            log.error("WebSocket handshake error: {}", exception.getMessage());
        }
    }
}
