package com.angellos.market.data.service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDTO {
    private String symbol;
    private BigDecimal price;
    private Long volume;
    private BigDecimal changePercent;
    private BigDecimal high24h;
    private BigDecimal low24h;
    private LocalDateTime timestamp;
    private String source;

}
