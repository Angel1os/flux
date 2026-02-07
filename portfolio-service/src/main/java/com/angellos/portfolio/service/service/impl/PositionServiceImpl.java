package com.angellos.portfolio.service.service.impl;

import com.angellos.portfolio.service.domain.dto.PositionDTO;
import com.angellos.portfolio.service.domain.model.Portfolio;
import com.angellos.portfolio.service.domain.model.Position;
import com.angellos.portfolio.service.mapper.PositionMapper;
import com.angellos.portfolio.service.repository.PortfolioRepository;
import com.angellos.portfolio.service.repository.PositionRepository;
import com.angellos.portfolio.service.service.MarketDataService;
import com.angellos.portfolio.service.service.PositionService;
import com.angellos.shared.record.Response;
import com.angellos.shared.utility.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.angellos.shared.enums.StatusCode.*;
import static com.angellos.shared.utility.AppUtils.getResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionServiceImpl implements PositionService {

    private final PositionRepository positionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionMapper positionMapper;
    private final MarketDataService marketDataService;

    @Override
    public ResponseEntity<Response> getPositions(UUID portfolioId, UUID userId) {
        log.info("Fetching positions for portfolio: {} for user: {}", portfolioId, userId);

        try {
            // Verify portfolio belongs to user
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            // Update current prices
            recalculatePortfolioValue(portfolioId);

            List<Position> positions = positionRepository.findByPortfolioId(portfolioId);
            List<PositionDTO> dtos = positions.stream()
                    .map(positionMapper::toDTO)
                    .collect(Collectors.toList());

            Response response = getResponse("Positions retrieved successfully", HttpStatus.OK, SUCCESS, dtos);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching positions: {}", e.getMessage(), e);
            Response response = getResponse("Error fetching positions: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    public ResponseEntity<Response> getPosition(UUID portfolioId, UUID userId, String symbol) {
        log.info("Fetching position: {} for portfolio: {} for user: {}", symbol, portfolioId, userId);

        try {
            // Verify portfolio belongs to user
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            Position position = positionRepository.findByPortfolioIdAndSymbol(portfolioId, symbol)
                    .orElse(null);

            if (position == null) {
                Response response = getResponse("Position not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            // Update current price
            updatePositionCurrentPrice(symbol, position.getCurrentPrice());

            PositionDTO dto = positionMapper.toDTO(position);
            Response response = getResponse("Position retrieved successfully", HttpStatus.OK, SUCCESS, dto);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching position: {}", e.getMessage(), e);
            Response response = getResponse("Error fetching position: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    @Transactional
    public void updatePositionCurrentPrice(String symbol, BigDecimal currentPrice) {
        if (currentPrice == null) {
            return;
        }

        // Update positions by symbol - get all positions with this symbol
        // Note: This is a simplified implementation. In production, you might want to
        // add a method to PositionRepository to find by symbol across all portfolios
        log.debug("Updating current price for symbol: {} to {}", symbol, currentPrice);
        // Actual update happens in recalculatePortfolioValue
    }

    @Override
    @Transactional
    public void recalculatePortfolioValue(UUID portfolioId) {
        log.debug("Recalculating portfolio value for: {}", portfolioId);

        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
        if (portfolio == null) {
            return;
        }

        List<Position> positions = positionRepository.findByPortfolioId(portfolioId);
        BigDecimal totalUnrealizedPnL = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = portfolio.getCashBalance();

        for (Position position : positions) {
            if (position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Get current price from Market Data Service
            BigDecimal currentPrice = marketDataService.getCurrentPrice(position.getSymbol());
            if (currentPrice != null) {
                position.setCurrentPrice(currentPrice);
                position.setCurrentValue(currentPrice.multiply(position.getQuantity()));

                // Calculate unrealized P&L
                BigDecimal unrealizedPnL = position.getCurrentValue()
                        .subtract(position.getTotalCost());
                position.setUnrealizedPnL(unrealizedPnL);

                // Calculate unrealized P&L percentage
                if (position.getTotalCost().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pnlPercent = unrealizedPnL
                            .divide(position.getTotalCost(), 6, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    position.setUnrealizedPnLPercent(pnlPercent);
                }

                totalUnrealizedPnL = totalUnrealizedPnL.add(unrealizedPnL);
                totalCurrentValue = totalCurrentValue.add(position.getCurrentValue());

                positionRepository.save(position);
            }
        }

        // Update portfolio
        portfolio.setUnrealizedPnL(totalUnrealizedPnL);
        portfolio.setTotalValue(totalCurrentValue);
        portfolioRepository.save(portfolio);

        log.debug("Portfolio value recalculated: totalValue={}, unrealizedPnL={}", 
                totalCurrentValue, totalUnrealizedPnL);
    }
}
