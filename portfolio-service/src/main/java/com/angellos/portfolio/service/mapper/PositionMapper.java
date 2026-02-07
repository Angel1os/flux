package com.angellos.portfolio.service.mapper;

import com.angellos.portfolio.service.domain.dto.PositionDTO;
import com.angellos.portfolio.service.domain.model.Position;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {

    public PositionDTO toDTO(Position position) {
        if (position == null) {
            return null;
        }

        return PositionDTO.builder()
                .id(position.getId())
                .portfolioId(position.getPortfolio() != null ? position.getPortfolio().getId() : null)
                .symbol(position.getSymbol())
                .quantity(position.getQuantity())
                .averagePrice(position.getAveragePrice())
                .currentPrice(position.getCurrentPrice())
                .totalCost(position.getTotalCost())
                .currentValue(position.getCurrentValue())
                .unrealizedPnL(position.getUnrealizedPnL())
                .unrealizedPnLPercent(position.getUnrealizedPnLPercent())
                .lastUpdated(position.getLastUpdated())
                .build();
    }
}
