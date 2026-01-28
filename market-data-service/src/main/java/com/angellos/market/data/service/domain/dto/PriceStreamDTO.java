package com.angellos.market.data.service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceStreamDTO {
    private String symbol;
    private BigDecimal price;
    private BigDecimal changePercent;
    private Instant timestamp;
}
