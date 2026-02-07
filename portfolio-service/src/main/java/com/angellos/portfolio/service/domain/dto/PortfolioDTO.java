package com.angellos.portfolio.service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDTO {
    private UUID id;
    private UUID userId;
    private String name;
    private BigDecimal cashBalance;
    private BigDecimal totalValue;
    private BigDecimal realizedPnL;
    private BigDecimal unrealizedPnL;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
