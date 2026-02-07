package com.angellos.portfolio.service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryDTO {
    private PortfolioDTO portfolio;
    private List<PositionDTO> positions;
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalPnL;
    private BigDecimal totalPnLPercent;
    private Integer positionCount;
}
