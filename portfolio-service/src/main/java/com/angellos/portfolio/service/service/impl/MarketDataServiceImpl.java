package com.angellos.portfolio.service.service.impl;

import com.angellos.portfolio.service.service.MarketDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataServiceImpl implements MarketDataService {

    private static final String CURRENT_PRICE_URL_TEMPLATE = "/api/v1/market-data/prices/{symbol}/current";

    @Value("${market.data.service.url}")
    private String marketDataServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;


    @Override
    @CircuitBreaker(name = "marketDataService", fallbackMethod = "getCurrentPriceFallback")
    public BigDecimal getCurrentPrice(String symbol) {
        try {
            String url = marketDataServiceUrl + CURRENT_PRICE_URL_TEMPLATE.replace("{symbol}", symbol);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                // Try to find price in response - could be in "data" or directly in root
                JsonNode priceNode = jsonNode.get("data");
                if (priceNode == null) {
                    priceNode = jsonNode;
                }
                
                if (priceNode != null && priceNode.has("price")) {
                    return new BigDecimal(priceNode.get("price").asText());
                }
            }

            log.warn("Could not fetch current price for symbol: {}", symbol);
            return null;
        } catch (Exception e) {
            log.error("Error fetching current price for symbol {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    public BigDecimal getCurrentPriceFallback(String symbol, Exception e) {
        log.warn("Circuit breaker opened for Market Data Service. Using fallback for symbol: {}", symbol);
        return null; // Return null, position will keep last known price
    }
}
