package com.angellos.api.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * Global filter to extract user ID from JWT token and add it as a header.
 * This helps with rate limiting by user.
 * 
 * Priority:
 * 1. Use X-User-Id header if already present
 * 2. Extract 'sub' claim from JWT token
 * 3. Fallback to IP address (handled by RateLimiterConfig)
 */
@Component
@Slf4j
public class JwtExtractionFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Check if X-User-Id header already exists (use it)
        String existingUserId = request.getHeaders().getFirst("X-User-Id");
        if (StringUtils.hasText(existingUserId)) {
            log.debug("X-User-Id header already present: {}", existingUserId);
            return chain.filter(exchange);
        }
        
        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                // Decode JWT and extract 'sub' claim
                String userId = extractUserIdFromJwt(token);
                
                if (StringUtils.hasText(userId)) {
                    log.debug("Extracted user ID from JWT: {}", userId);
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .build();
                    
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                } else {
                    log.debug("No 'sub' claim found in JWT token");
                }
            } catch (Exception e) {
                log.debug("Failed to extract user ID from JWT token: {}", e.getMessage());
            }
        }
        
        // No user ID extracted - RateLimiterConfig will fallback to IP address
        return chain.filter(exchange);
    }

    /**
     * Extracts the 'sub' (subject) claim from a JWT token.
     * JWT format: header.payload.signature
     * We decode the payload (base64) and parse JSON to get 'sub'.
     */
    private String extractUserIdFromJwt(String token) {
        try {
            // Split JWT into parts
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.debug("Invalid JWT format: expected 3 parts, got {}", parts.length);
                return null;
            }
            
            // Decode payload (second part)
            String payload = parts[1];
            
            // Add padding if needed (Base64 requires padding)
            int padding = 4 - (payload.length() % 4);
            if (padding != 4) {
                payload = payload + "=".repeat(padding);
            }
            
            // Decode base64
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);
            
            // Parse JSON and extract 'sub' claim
            JsonNode jsonNode = objectMapper.readTree(decodedPayload);
            JsonNode subNode = jsonNode.get("sub");
            
            if (subNode != null && subNode.isTextual()) {
                return subNode.asText();
            }
            
            log.debug("No 'sub' claim found in JWT payload");
            return null;
        } catch (Exception e) {
            log.debug("Error parsing JWT token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public int getOrder() {
        return -100; // High priority - run early in the filter chain
    }
}
