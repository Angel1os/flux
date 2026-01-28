package com.angellos.market.data.service.service;

import com.angellos.market.data.service.domain.dto.PriceEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simulates price data for testing purposes.
 * Generates realistic price movements using random walk algorithm.
 * In production, this would be replaced by real market data providers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceSimulatorService {

    private final PriceProducerService priceProducerService;
    private final Random random = new Random();
    
    // Track current prices and 24h stats for each symbol
    private final Map<String, PriceState> priceStates = new HashMap<>();
    
    // Symbols to simulate
    private static final String[] SYMBOLS = {"BTC", "ETH", "AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", "NVDA"};

    /**
     * Initialize price states on startup
     */
    @Scheduled(initialDelay = 2000, fixedDelay = Long.MAX_VALUE)
    public void initializePrices() {
        log.info("Initializing price simulator for {} symbols", SYMBOLS.length);
        for (String symbol : SYMBOLS) {
            BigDecimal basePrice = getBasePrice(symbol);
            priceStates.put(symbol, new PriceState(
                    basePrice,
                    basePrice.multiply(BigDecimal.valueOf(1.05)), // high24h
                    basePrice.multiply(BigDecimal.valueOf(0.95)), // low24h
                    basePrice
            ));
        }
        log.info("Price simulator initialized");
    }

    /**
     * Generate and publish price updates every 2 seconds
     */
//    @Scheduled(fixedRate = 2000) // Every 2 seconds
    public void generatePriceUpdates() {
        if (priceStates.isEmpty()) {
            return; // Wait for initialization
        }

        for (String symbol : SYMBOLS) {
            PriceState state = priceStates.get(symbol);
            if (state == null) {
                continue;
            }

            // Random walk: price change between -2% and +2%
            BigDecimal changePercent = BigDecimal.valueOf(random.nextDouble() * 4 - 2)
                    .setScale(4, RoundingMode.HALF_UP);
            
            BigDecimal changeAmount = state.currentPrice
                    .multiply(changePercent)
                    .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            
            BigDecimal newPrice = state.currentPrice.add(changeAmount)
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Ensure price doesn't go negative
            if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
                newPrice = state.currentPrice; // Keep current price if it would go negative
            }

            // Update 24h high/low
            BigDecimal newHigh24h = newPrice.max(state.high24h);
            BigDecimal newLow24h = newPrice.min(state.low24h);

            // Calculate volume (random between 1000 and 100000)
            Long volume = 1000L + random.nextInt(99000);

            // Create price event
            PriceEventDTO priceEvent = PriceEventDTO.builder()
                    .symbol(symbol)
                    .price(newPrice)
                    .volume(volume)
                    .changePercent(changePercent)
                    .high24h(newHigh24h)
                    .low24h(newLow24h)
                    .timestamp(LocalDateTime.now())
                    .source("SIMULATOR")
                    .build();

            // Publish to Kafka
            priceProducerService.publishPriceEvent(priceEvent);

            // Update state
            priceStates.put(symbol, new PriceState(
                    newPrice,
                    newHigh24h,
                    newLow24h,
                    state.previousPrice
            ));
        }
    }

    /**
     * Get base price for a symbol (for initialization)
     */
    private BigDecimal getBasePrice(String symbol) {
        return switch (symbol) {
            case "BTC" -> BigDecimal.valueOf(45000);
            case "ETH" -> BigDecimal.valueOf(2500);
            case "AAPL" -> BigDecimal.valueOf(180);
            case "GOOGL" -> BigDecimal.valueOf(140);
            case "MSFT" -> BigDecimal.valueOf(380);
            case "TSLA" -> BigDecimal.valueOf(250);
            case "AMZN" -> BigDecimal.valueOf(150);
            case "NVDA" -> BigDecimal.valueOf(500);
            default -> BigDecimal.valueOf(100);
        };
    }

    /**
     * Internal state tracking for price simulation
     */
    private static class PriceState {
        BigDecimal currentPrice;
        BigDecimal high24h;
        BigDecimal low24h;
        BigDecimal previousPrice;

        PriceState(BigDecimal currentPrice, BigDecimal high24h, BigDecimal low24h, BigDecimal previousPrice) {
            this.currentPrice = currentPrice;
            this.high24h = high24h;
            this.low24h = low24h;
            this.previousPrice = previousPrice;
        }
    }
}
