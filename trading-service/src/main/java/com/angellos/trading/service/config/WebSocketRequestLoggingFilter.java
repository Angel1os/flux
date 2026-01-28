package com.angellos.trading.service.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
// import org.springframework.stereotype.Component; // Uncomment when enabling the filter

import java.io.IOException;
import java.util.Enumeration;

/**
 * Filter to log all WebSocket-related requests for debugging.
 */
// @Component
@Order(1)
@Slf4j
public class WebSocketRequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getRequestURI();
            
            if (path != null && path.startsWith("/ws/")) {
                log.info("=== WebSocket Request Filter ===");
                log.info("Method: {}", httpRequest.getMethod());
                log.info("URI: {}", httpRequest.getRequestURI());
                log.info("Query String: {}", httpRequest.getQueryString());
                log.info("Headers:");
                Enumeration<String> headerNames = httpRequest.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    log.info("  {}: {}", headerName, httpRequest.getHeader(headerName));
                }
                
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                
                try {
                    chain.doFilter(request, response);
                    log.info("Response Status: {}", httpResponse.getStatus());
                } catch (Exception e) {
                    log.error("Filter chain error: {}", e.getMessage(), e);
                    throw e;
                }
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
