package com.angellos.market.data.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for Market Data Service.
 * Supports OAuth2 authentication with Keycloak (password grant and client credentials).
 * Configured for WebFlux (reactive) endpoints.
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        
        // Define scopes for password grant
        Scopes passwordScopes = new Scopes();
        passwordScopes.addString("openid", "OpenID Connect scope");
        passwordScopes.addString("profile", "User profile information");
        passwordScopes.addString("email", "User email address");
        
        // Define scopes for client credentials grant
        Scopes clientCredentialsScopes = new Scopes();
        clientCredentialsScopes.addString("openid", "OpenID Connect scope");
        
        return new OpenAPI()
                .info(new Info()
                        .title("Market Data Service API")
                        .description("The Market Data Service provides real-time and historical price data " +
                                "for trading symbols. Supports REST API (WebFlux), GraphQL queries/subscriptions, " +
                                "and WebSocket streaming for real-time price updates. " +
                                "Uses reactive programming with R2DBC and Kafka Streams for high-throughput processing. " +
                                "Requires OAuth2 authentication via Keycloak.")
                        .version("1.0")
                        .contact(new Contact()
                                .email("angellosprince@gmail.com")
                                .name("Developer: Prince Amofah")
                        )
                        .license(new License()
                                .name("Flux")
                        )
                )
                .addSecurityItem(new SecurityRequirement()
                        .addList("OAuth2")
                )
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("OAuth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth2 authentication with Keycloak. " +
                                        "Use 'Password' flow for username/password authentication or " +
                                        "'Client Credentials' for service-to-service authentication.")
                                .flows(new OAuthFlows()
                                        .password(new OAuthFlow()
                                                .tokenUrl(tokenUrl)
                                                .refreshUrl(tokenUrl)
                                                .scopes(passwordScopes)
                                        )
                                        .clientCredentials(new OAuthFlow()
                                                .tokenUrl(tokenUrl)
                                                .scopes(clientCredentialsScopes)
                                        )
                                )
                        )
                );
    }
}
