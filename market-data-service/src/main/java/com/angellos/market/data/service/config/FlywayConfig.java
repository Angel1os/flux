package com.angellos.market.data.service.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway configuration to ensure migrations run on startup.
 * This ensures the database schema is created before R2DBC tries to use it.
 * 
 * Note: The database 'market_data_db' must exist before Flyway can run migrations.
 * Create it manually: CREATE DATABASE market_data_db;
 */
@Configuration
@Slf4j
public class FlywayConfig {

    @Value("${spring.flyway.url}")
    private String flywayUrl;

    @Value("${spring.flyway.user}")
    private String flywayUser;

    @Value("${spring.flyway.password}")
    private String flywayPassword;

    @Value("${spring.flyway.locations}")
    private String flywayLocations;

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        log.info("Initializing Flyway migrations - URL: {}, User: {}, Locations: {}", 
                flywayUrl, flywayUser, flywayLocations);
        
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(flywayUrl, flywayUser, flywayPassword)
                    .locations(flywayLocations)
                    .baselineOnMigrate(true)
                    .load();

            MigrateResult migrationsApplied = flyway.migrate();
            log.info("Flyway migrations completed successfully - Applied {} migration(s)", migrationsApplied);
            
            return flyway;
        } catch (Exception e) {
            log.error("Flyway migration failed! Error: {}", e.getMessage(), e);
            log.error("Please ensure:");
            log.error("1. PostgreSQL is running");
            log.error("2. Database 'market_data_db' exists (CREATE DATABASE market_data_db;)");
            log.error("3. User '{}' has permissions on the database", flywayUser);
            throw new RuntimeException("Flyway migration failed", e);
        }
    }
}
