package com.angellos.portfolio.service.service;

import com.angellos.portfolio.service.domain.dto.PositionDTO;
import com.angellos.shared.record.Response;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PositionService {

    ResponseEntity<Response> getPositions(UUID portfolioId, UUID userId);

    ResponseEntity<Response> getPosition(UUID portfolioId, UUID userId, String symbol);

    void updatePositionCurrentPrice(String symbol, BigDecimal currentPrice);

    void recalculatePortfolioValue(UUID portfolioId);

}
