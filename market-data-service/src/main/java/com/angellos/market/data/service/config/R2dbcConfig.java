package com.angellos.market.data.service.config;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * R2DBC configuration for reactive database access.
 * Configures PostgreSQL connection using R2DBC for non-blocking database operations.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.angellos.market.data.service.repository")
@Slf4j
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        // Parse R2DBC URL: r2dbc:postgresql://localhost:5432/market_data_db
        String url = r2dbcUrl.replace("r2dbc:postgresql://", "");
        String[] parts = url.split("/");
        String[] hostPort = parts[0].split(":");
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 5432;
        String database = parts.length > 1 ? parts[1] : "market_data_db";

        log.info("Configuring R2DBC connection - Host: {}, Port: {}, Database: {}", host, port, database);

        PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .build();

        return new PostgresqlConnectionFactory(configuration);
    }
}
