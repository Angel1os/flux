package com.angellos.market.data.service.config;

import lombok.extern.slf4j.Slf4j;
import graphql.scalars.ExtendedScalars;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

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
            // Custom scalar types
            // Schema uses: scalar LocalDateTime
            // We must serialize/parse java.time.LocalDateTime (not OffsetDateTime).
            wiringBuilder.scalar(ExtendedScalars.DateTime);

            GraphQLScalarType localDateTimeScalar = GraphQLScalarType.newScalar()
                    .name("LocalDateTime")
                    .description("ISO-8601 LocalDateTime (no offset), e.g. 2026-01-31T23:59:59")
                    .coercing(new Coercing<LocalDateTime, String>() {
                        @Override
                        public String serialize(Object dataFetcherResult) {
                            if (dataFetcherResult == null) {
                                return null;
                            }
                            if (dataFetcherResult instanceof LocalDateTime ldt) {
                                return ldt.toString();
                            }
                            throw new IllegalArgumentException(
                                    "Expected LocalDateTime but was " + dataFetcherResult.getClass().getName()
                            );
                        }

                        @Override
                        public LocalDateTime parseValue(Object input) {
                            if (input == null) {
                                return null;
                            }
                            if (input instanceof LocalDateTime ldt) {
                                return ldt;
                            }
                            if (input instanceof String s) {
                                try {
                                    return LocalDateTime.parse(s);
                                } catch (DateTimeParseException e) {
                                    throw new IllegalArgumentException("Invalid LocalDateTime: " + s, e);
                                }
                            }
                            throw new IllegalArgumentException(
                                    "Expected String/LocalDateTime but was " + input.getClass().getName()
                            );
                        }

                        @Override
                        public LocalDateTime parseLiteral(Object input) {
                            if (input instanceof StringValue sv) {
                                try {
                                    return LocalDateTime.parse(sv.getValue());
                                } catch (DateTimeParseException e) {
                                    throw new IllegalArgumentException("Invalid LocalDateTime: " + sv.getValue(), e);
                                }
                            }
                            return null;
                        }
                    })
                    .build();

            wiringBuilder.scalar(localDateTimeScalar);
        };
    }
}
