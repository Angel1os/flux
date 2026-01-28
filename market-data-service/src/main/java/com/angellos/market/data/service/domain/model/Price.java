package com.angellos.market.data.service.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("prices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Price {

    @Id
    private UUID id;

    @Column("symbol")
    private String symbol;

    @Column("price")
    private BigDecimal price;

    @Column("volume")
    private Long volume;

    @Column("change_percent")
    private BigDecimal changePercent;

    @Column("high_24h")
    private BigDecimal high24h;

    @Column("low_24h")
    private BigDecimal low24h;

    @Column("timestamp")
    private LocalDateTime timestamp;

    @Column("source")
    private String source; // e.g., "BINANCE", "COINBASE", "AGGREGATED"

}
