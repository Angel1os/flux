package com.angellos.market.data.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL configuration for Market Data Service.
 * Configures GraphQL schema and type resolvers.
 */
@Configuration
@Slf4j
public class GraphQLConfig {

    /**
     * Configures GraphQL runtime wiring.
     * This allows custom scalar types, directives, and type resolvers.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> {
            log.info("GraphQL runtime wiring configured");
            // Add custom scalar types, directives, etc. here if needed
            // For now, using default Spring GraphQL configuration
        };
    }
}
