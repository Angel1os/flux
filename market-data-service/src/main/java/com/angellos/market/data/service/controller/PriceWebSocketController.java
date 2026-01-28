package com.angellos.market.data.service.controller;

import com.angellos.market.data.service.domain.dto.PriceDTO;
import com.angellos.market.data.service.domain.dto.PriceStreamDTO;
import com.angellos.market.data.service.mapper.PriceMapper;
import com.angellos.market.data.service.service.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * WebSocket controller for real-time price streaming.
 * Uses STOMP over WebSocket for broadcasting price updates.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PriceWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PriceService priceService;
    private final PriceMapper priceMapper;

    /**
     * Send price update to subscribers of a specific symbol
     */
    public void sendPriceUpdate(String symbol, PriceDTO price) {
        PriceStreamDTO streamDTO = priceMapper.toStreamDTO(price);
        messagingTemplate.convertAndSend("/topic/prices/" + symbol, streamDTO);
        log.debug("Sent price update for symbol {}: {}", symbol, streamDTO.getPrice());
    }

    /**
     * Broadcast price update to all subscribers
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void broadcastPriceUpdate(PriceDTO price) {
        PriceStreamDTO streamDTO = priceMapper.toStreamDTO(price);
        messagingTemplate.convertAndSend("/topic/prices/all", streamDTO);
        log.debug("Broadcasted price update for symbol: {}", price.getSymbol());
    }

    /**
     * Handle client subscription request for a specific symbol
     * Client sends message to /app/prices/subscribe with payload: {"symbol": "BTC"}
     */
    @MessageMapping("/prices/subscribe")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public void subscribeToPrice(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        String username = auth != null ? auth.getName() : "anonymous";
        String symbol = payload != null ? payload.get("symbol") : null;
        
        if (symbol == null || symbol.isEmpty()) {
            log.warn("Invalid subscription request: symbol is required");
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/prices/error",
                    Map.of("error", "Symbol is required")
            );
            return;
        }
        
        log.info("User {} subscribed to price updates for symbol: {}", username, symbol);
        
        // Start streaming prices for this symbol (this runs in background)
        priceService.streamPrices(symbol)
                .doOnNext(price -> sendPriceUpdate(symbol, price))
                .doOnError(error -> log.error("Error in price stream for symbol {}: {}", symbol, error.getMessage()))
                .doOnCancel(() -> log.info("Price stream cancelled for symbol: {}", symbol))
                .subscribe(); // Start the reactive stream
        
        // Send confirmation
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/prices/subscribed",
                Map.of("symbol", symbol, "status", "subscribed")
        );
    }

    /**
     * Handle client subscription request for all symbols
     * Client sends message to /app/prices/subscribe/all
     */
    @MessageMapping("/prices/subscribe/all")
    @PreAuthorize("hasRole('ADMIN')")
    public void subscribeToAllPrices(SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        String username = auth != null ? auth.getName() : "anonymous";
        
        log.info("User {} subscribed to all price updates", username);
        
        // Stream all latest prices periodically (this runs in background)
        Flux.interval(Duration.ofSeconds(1))
                .flatMap(seq -> priceService.getAllLatestPrices())
                .doOnNext(this::broadcastPriceUpdate)
                .doOnError(error -> log.error("Error in all prices stream: {}", error.getMessage()))
                .doOnCancel(() -> log.info("All prices stream cancelled"))
                .subscribe(); // Start the reactive stream
        
        // Send confirmation
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/prices/subscribed",
                Map.of("status", "subscribed", "scope", "all")
        );
    }
}
