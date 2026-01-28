package com.angellos.market.data.service.service.impl;

import com.angellos.market.data.service.domain.dto.PriceDTO;
import com.angellos.market.data.service.mapper.PriceMapper;
import com.angellos.market.data.service.repository.PriceRepository;
import com.angellos.market.data.service.service.PriceService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceServiceImpl implements PriceService {

    private final PriceRepository priceRepository;
    private final PriceMapper priceMapper;

    @Override
    @CircuitBreaker(name = "priceService", fallbackMethod = "getCurrentPriceFallback")
    public Mono<PriceDTO> getCurrentPrice(String symbol) {
        log.info("Fetching current price for symbol: {}", symbol);
        return priceRepository.findFirstBySymbolOrderByTimestampDesc(symbol)
                .map(priceMapper::toDTO)
                .switchIfEmpty(Mono.error(new RuntimeException("Price not found for symbol: " + symbol)))
                .doOnError(error -> log.error("Error fetching current price for symbol {}: {}", symbol, error.getMessage()));
    }

    private Mono<PriceDTO> getCurrentPriceFallback(String symbol, Exception e) {
        log.warn("Circuit breaker fallback for symbol: {}. Error: {}", symbol, e.getMessage());
        return Mono.just(PriceDTO.builder()
                .symbol(symbol)
                .price(java.math.BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .source("FALLBACK")
                .build());
    }

    @Override
    public Flux<PriceDTO> getPriceHistory(String symbol, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching price history for symbol: {} from {} to {}", symbol, startDate, endDate);
        return priceRepository.findBySymbolAndTimestampBetween(symbol, startDate, endDate)
                .map(priceMapper::toDTO)
                .doOnError(error -> log.error("Error fetching price history for symbol {}: {}",
                        symbol, error.getMessage()));
    }

    @Override
    public Flux<String> getAllSymbols() {
        log.info("Fetching all available symbols");
        return priceRepository.findAllSymbols()
                .doOnError(error -> log.error("Error fetching all symbols: {}", error.getMessage()));
    }

    @Override
    public Flux<PriceDTO> getAllLatestPrices() {
        log.info("Fetching latest prices for all symbols");
        return priceRepository.findLatestPricesForAllSymbols()
                .map(priceMapper::toDTO)
                .doOnError(error -> log.error("Error fetching latest prices: {}", error.getMessage()));
    }

    @Override
    public Flux<PriceDTO> streamPrices(String symbol) {
        log.info("Starting price stream for symbol: {}", symbol);
        // Stream prices every 1 second by polling the database
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(seq -> getCurrentPrice(symbol)
                        .onErrorResume(error -> {
                            log.warn("Error in price stream for symbol {}: {}", symbol, error.getMessage());
                            return Mono.empty(); // Skip errors and continue streaming
                        }))
                .doOnError(error -> log.error("Error in price stream: {}", error.getMessage()))
                .doOnCancel(() -> log.info("Price stream cancelled for symbol: {}", symbol))
                .doOnComplete(() -> log.info("Price stream completed for symbol: {}", symbol));
    }
}
