package com.angellos.market.data.service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for price events published to Kafka topic 'price-events'.
 * This is the canonical format for all price updates in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceEventDTO {
    private String symbol;
    private BigDecimal price;
    private Long volume;
    private BigDecimal changePercent;
    private BigDecimal high24h;
    private BigDecimal low24h;
    private LocalDateTime timestamp;
    private String source; // e.g., "SIMULATOR", "BINANCE", "COINBASE", "AGGREGATED"
}
