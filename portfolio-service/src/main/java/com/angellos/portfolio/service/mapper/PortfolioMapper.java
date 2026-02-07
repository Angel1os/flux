package com.angellos.portfolio.service.mapper;

import com.angellos.portfolio.service.domain.dto.PortfolioDTO;
import com.angellos.portfolio.service.domain.model.Portfolio;
import org.springframework.stereotype.Component;

@Component
public class PortfolioMapper {

    public PortfolioDTO toDTO(Portfolio portfolio) {
        if (portfolio == null) {
            return null;
        }

        return PortfolioDTO.builder()
                .id(portfolio.getId())
                .userId(portfolio.getUserId())
                .name(portfolio.getName())
                .cashBalance(portfolio.getCashBalance())
                .totalValue(portfolio.getTotalValue())
                .realizedPnL(portfolio.getRealizedPnL())
                .unrealizedPnL(portfolio.getUnrealizedPnL())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .build();
    }

    public Portfolio toEntity(PortfolioDTO dto) {
        if (dto == null) {
            return null;
        }

        return Portfolio.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .name(dto.getName())
                .cashBalance(dto.getCashBalance())
                .totalValue(dto.getTotalValue())
                .realizedPnL(dto.getRealizedPnL())
                .unrealizedPnL(dto.getUnrealizedPnL())
                .build();
    }
}
