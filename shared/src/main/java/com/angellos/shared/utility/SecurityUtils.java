package com.angellos.shared.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

/**
 * Utility class for extracting user information from JWT tokens.
 * This class can be used across all microservices that use OAuth2 Resource Server.
 */
@Slf4j
public class SecurityUtils {

    private static final String SUB_CLAIM = "sub";
    private static final String USER_ID_CLAIM = "userId";
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String EMAIL_CLAIM = "email";

    /**
     * Gets the current authenticated user's ID from the JWT token.
     * Falls back to 'sub' claim if 'userId' claim is not present.
     *
     * @return User ID as UUID, or null if not authenticated or claim not found
     */
    public static UUID getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
                log.debug("No JWT authentication found in security context");
                return null;
            }

            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();

            // Try to get userId claim first (custom claim)
            Object userIdClaim = jwt.getClaim(USER_ID_CLAIM);
            if (userIdClaim != null) {
                return UUID.fromString(userIdClaim.toString());
            }

            // Fall back to 'sub' claim (standard OIDC claim containing user ID)
            String sub = jwt.getClaimAsString(SUB_CLAIM);
            if (sub != null && !sub.isEmpty()) {
                return UUID.fromString(sub);
            }

            log.warn("No user ID found in JWT token claims");
            return null;
        } catch (Exception e) {
            log.error("Error extracting user ID from JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the current authenticated user's username from the JWT token.
     *
     * @return Username, or null if not authenticated or claim not found
     */
    public static String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
                return null;
            }

            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();

            // Try preferred_username first
            String username = jwt.getClaimAsString(PREFERRED_USERNAME_CLAIM);
            if (username != null && !username.isEmpty()) {
                return username;
            }

            // Fall back to 'sub' claim
            return jwt.getClaimAsString(SUB_CLAIM);
        } catch (Exception e) {
            log.error("Error extracting username from JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the current authenticated user's email from the JWT token.
     *
     * @return Email, or null if not authenticated or claim not found
     */
    public static String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
                return null;
            }

            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();

            return jwt.getClaimAsString(EMAIL_CLAIM);
        } catch (Exception e) {
            log.error("Error extracting email from JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the raw JWT token from the security context.
     *
     * @return JWT token, or null if not authenticated
     */
    public static Jwt getCurrentJwt() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
                return null;
            }

            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            return jwtAuth.getToken();
        } catch (Exception e) {
            log.error("Error extracting JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null 
                && authentication.isAuthenticated()
                && authentication instanceof JwtAuthenticationToken;
    }
}
