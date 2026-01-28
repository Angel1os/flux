package com.angellos.market.data.service.repository;

import com.angellos.market.data.service.domain.model.Price;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface PriceRepository extends ReactiveCrudRepository<Price, UUID> {

    // Find latest price for a symbol
    Mono<Price> findFirstBySymbolOrderByTimestampDesc(String symbol);

    // Find prices by symbol in date range
    @Query("SELECT * FROM prices WHERE symbol = :symbol " +
            "AND timestamp >= :startDate AND timestamp <= :endDate " +
            "ORDER BY timestamp DESC")
    Flux<Price> findBySymbolAndTimestampBetween(
            String symbol,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Find all symbols
    @Query("SELECT DISTINCT symbol FROM prices")
    Flux<String> findAllSymbols();

    // Find latest prices for all symbols
    @Query("SELECT DISTINCT ON (symbol) * FROM prices " +
            "ORDER BY symbol, timestamp DESC")
    Flux<Price> findLatestPricesForAllSymbols();

}
