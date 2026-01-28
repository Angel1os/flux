package com.angellos.market.data.service.controller;

import com.angellos.market.data.service.domain.dto.PriceDTO;
import com.angellos.market.data.service.service.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * GraphQL controller for Market Data Service.
 * Provides GraphQL queries and subscriptions for price data.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PriceGraphQLController {

    private final PriceService priceService;

    @QueryMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Mono<PriceDTO> currentPrice(@Argument String symbol) {
        log.info("GraphQL query: currentPrice for symbol: {}", symbol);
        return priceService.getCurrentPrice(symbol)
                .doOnError(error -> log.error("Error in GraphQL currentPrice query: {}", error.getMessage()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Flux<PriceDTO> priceHistory(
            @Argument String symbol,
            @Argument LocalDateTime startDate,
            @Argument LocalDateTime endDate) {
        log.info("GraphQL query: priceHistory for symbol: {} from {} to {}", symbol, startDate, endDate);
        return priceService.getPriceHistory(symbol, startDate, endDate)
                .doOnError(error -> log.error("Error in GraphQL priceHistory query: {}", error.getMessage()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Flux<String> symbols() {
        log.info("GraphQL query: symbols");
        return priceService.getAllSymbols()
                .doOnError(error -> log.error("Error in GraphQL symbols query: {}", error.getMessage()));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Flux<PriceDTO> latestPrices() {
        log.info("GraphQL query: latestPrices");
        return priceService.getAllLatestPrices()
                .doOnError(error -> log.error("Error in GraphQL latestPrices query: {}", error.getMessage()));
    }

    @SubscriptionMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Flux<PriceDTO> priceStream(@Argument String symbol) {
        log.info("GraphQL subscription: priceStream for symbol: {}", symbol);
        return priceService.streamPrices(symbol)
                .doOnError(error -> log.error("Error in GraphQL priceStream subscription: {}", error.getMessage()))
                .doOnCancel(() -> log.info("GraphQL priceStream subscription cancelled for symbol: {}", symbol));
    }
}
