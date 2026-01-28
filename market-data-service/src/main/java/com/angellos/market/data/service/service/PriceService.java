package com.angellos.market.data.service.service;

import com.angellos.market.data.service.domain.dto.PriceDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface PriceService {

    // Get current price for a symbol
    Mono<PriceDTO> getCurrentPrice(String symbol);

    // Get price history for a symbol
    Flux<PriceDTO> getPriceHistory(String symbol, LocalDateTime startDate, LocalDateTime endDate);

    // Get all available symbols
    Flux<String> getAllSymbols();

    // Get latest prices for all symbols
    Flux<PriceDTO> getAllLatestPrices();

    // Stream real-time prices (for WebSocket)
    Flux<PriceDTO> streamPrices(String symbol);

}
