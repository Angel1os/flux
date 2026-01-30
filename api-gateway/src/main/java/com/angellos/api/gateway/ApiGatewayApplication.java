package com.angellos.api.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Service
 * 
 * Features:
 * - Centralized routing to Trading Service and Market Data Service
 * - Load balancing (using Spring Cloud LoadBalancer)
 * - Rate limiting (Redis-based)
 * - Circuit breaker (Resilience4j)
 * - JWT token forwarding
 * - CORS configuration
 * - Single entry point for all microservices
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
