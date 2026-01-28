package com.angellos.market.data.service.controller;

import com.angellos.market.data.service.domain.dto.PriceDTO;
import com.angellos.market.data.service.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API controller for Market Data Service.
 * Provides reactive endpoints for price data using WebFlux.
 */
@RestController
@RequestMapping("/api/v1/market-data/prices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Price Endpoints", description = "REST API endpoints for market price data")
public class PriceController {

    private final PriceService priceService;

    @Operation(
            description = "Get the current (latest) price for a specific symbol",
            summary = "Get current price by symbol"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Price retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PriceDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Price not found for symbol",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/{symbol}/current")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Mono<ResponseEntity<PriceDTO>> getCurrentPrice(
            @Parameter(description = "Trading symbol (e.g., BTC, AAPL, GOOGL)", required = true, example = "BTC")
            @PathVariable String symbol) {
        log.info("Fetching current price for symbol: {}", symbol);
        return priceService.getCurrentPrice(symbol)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error fetching current price for symbol {}: {}", symbol, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                });
    }

    @Operation(
            description = "Get price history for a symbol within a date range",
            summary = "Get price history by symbol and date range"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Price history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PriceDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date range",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/{symbol}/history")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Flux<PriceDTO> getPriceHistory(
            @Parameter(description = "Trading symbol", required = true, example = "BTC")
            @PathVariable String symbol,
            @Parameter(description = "Start date (ISO format)", required = true, example = "2026-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true, example = "2026-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching price history for symbol: {} from {} to {}", symbol, startDate, endDate);
        return priceService.getPriceHistory(symbol, startDate, endDate)
                .doOnError(error -> log.error("Error fetching price history: {}", error.getMessage()));
    }

    @Operation(
            description = "Get all available trading symbols",
            summary = "Get all symbols"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Symbols retrieved successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping(value = "/symbols", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Mono<ResponseEntity<List<String>>> getAllSymbols() {
        log.info("Fetching all available symbols");
        return priceService.getAllSymbols()
                .collectList()
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("Error fetching symbols: {}", error.getMessage()));
    }

    @Operation(
            description = "Get the latest price for all available symbols",
            summary = "Get latest prices for all symbols"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Latest prices retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PriceDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public Flux<PriceDTO> getAllLatestPrices() {
        log.info("Fetching latest prices for all symbols");
        return priceService.getAllLatestPrices()
                .doOnError(error -> log.error("Error fetching latest prices: {}", error.getMessage()));
    }
}
