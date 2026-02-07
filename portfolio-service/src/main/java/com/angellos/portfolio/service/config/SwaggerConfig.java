package com.angellos.portfolio.service.config;

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

@Configuration
public class SwaggerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        
        Scopes passwordScopes = new Scopes();
        passwordScopes.addString("openid", "OpenID Connect scope");
        passwordScopes.addString("profile", "User profile information");
        passwordScopes.addString("email", "User email address");
        
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio Service API")
                        .description("The Portfolio Service tracks user portfolios, positions, and performance in real-time. " +
                                "Consumes order events from Trading Service and updates portfolios accordingly. " +
                                "Requires OAuth2 authentication via Keycloak. Use Password grant with your username and password.")
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
                                .description("OAuth2 authentication with Keycloak using Password grant. " +
                                        "Enter your username (e.g., trader1) and password to authenticate as a user.")
                                .flows(new OAuthFlows()
                                        .password(new OAuthFlow()
                                                .tokenUrl(tokenUrl)
                                                .refreshUrl(tokenUrl)
                                                .scopes(passwordScopes)
                                        )
                                )
                        )
                );
    }
}
