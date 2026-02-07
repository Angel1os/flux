package com.angellos.portfolio.service.service.impl;

import com.angellos.portfolio.service.domain.dto.PortfolioDTO;
import com.angellos.portfolio.service.domain.dto.PortfolioSummaryDTO;
import com.angellos.portfolio.service.domain.dto.PositionDTO;
import com.angellos.portfolio.service.domain.model.Portfolio;
import com.angellos.portfolio.service.mapper.PortfolioMapper;
import com.angellos.portfolio.service.mapper.PositionMapper;
import com.angellos.portfolio.service.repository.PortfolioRepository;
import com.angellos.portfolio.service.repository.PositionRepository;
import com.angellos.portfolio.service.service.PortfolioService;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.angellos.shared.enums.StatusCode.*;
import static com.angellos.shared.utility.AppUtils.getResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PortfolioMapper portfolioMapper;
    private final PositionMapper positionMapper;
    private final PositionService positionService;

    @Override
    @Transactional
    public ResponseEntity<Response> createPortfolio(UUID userId, String name) {
        log.info("Creating portfolio for user: {}, name: {}", userId, name);

        try {
            // Check if portfolio with same name exists
            if (portfolioRepository.existsByUserIdAndName(userId, name)) {
                Response response = getResponse("Portfolio with name '" + name + "' already exists", 
                        HttpStatus.CONFLICT, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            Portfolio portfolio = Portfolio.builder()
                    .userId(userId)
                    .name(name)
                    .cashBalance(BigDecimal.ZERO)
                    .totalValue(BigDecimal.ZERO)
                    .realizedPnL(BigDecimal.ZERO)
                    .unrealizedPnL(BigDecimal.ZERO)
                    .build();

            portfolio = portfolioRepository.save(portfolio);
            log.info("Portfolio created: {}", portfolio.getId());

            PortfolioDTO dto = portfolioMapper.toDTO(portfolio);
            Response response = getResponse("Portfolio created successfully", HttpStatus.CREATED, SUCCESS, dto);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error creating portfolio: {}", e.getMessage(), e);
            Response response = getResponse("Error creating portfolio: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    public ResponseEntity<Response> getPortfolio(UUID portfolioId, UUID userId) {
        log.info("Fetching portfolio: {} for user: {}", portfolioId, userId);

        try {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            // Recalculate values with current prices
            positionService.recalculatePortfolioValue(portfolioId);
            portfolio = portfolioRepository.findById(portfolioId).orElse(portfolio);

            PortfolioDTO dto = portfolioMapper.toDTO(portfolio);
            Response response = getResponse("Portfolio retrieved successfully", HttpStatus.OK, SUCCESS, dto);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching portfolio: {}", e.getMessage(), e);
            Response response = getResponse("Error fetching portfolio: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    public ResponseEntity<Response> getAllPortfolios(UUID userId) {
        log.info("Fetching all portfolios for user: {}", userId);

        try {
            List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
            List<PortfolioDTO> dtos = portfolios.stream()
                    .map(portfolioMapper::toDTO)
                    .collect(Collectors.toList());

            Response response = getResponse("Portfolios retrieved successfully", HttpStatus.OK, SUCCESS, dtos);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching portfolios: {}", e.getMessage(), e);
            Response response = getResponse("Error fetching portfolios: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Response> updatePortfolio(UUID portfolioId, UUID userId, String name) {
        log.info("Updating portfolio: {} for user: {}", portfolioId, userId);

        try {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            // Check if new name conflicts with existing portfolio
            if (!portfolio.getName().equals(name) && portfolioRepository.existsByUserIdAndName(userId, name)) {
                Response response = getResponse("Portfolio with name '" + name + "' already exists", 
                        HttpStatus.CONFLICT, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            portfolio.setName(name);
            portfolio = portfolioRepository.save(portfolio);

            PortfolioDTO dto = portfolioMapper.toDTO(portfolio);
            Response response = getResponse("Portfolio updated successfully", HttpStatus.OK, SUCCESS, dto);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error updating portfolio: {}", e.getMessage(), e);
            Response response = getResponse("Error updating portfolio: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Response> deletePortfolio(UUID portfolioId, UUID userId) {
        log.info("Deleting portfolio: {} for user: {}", portfolioId, userId);

        try {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            // Delete all positions
            positionRepository.deleteByPortfolioId(portfolioId);
            
            // Delete portfolio
            portfolioRepository.delete(portfolio);

            Response response = getResponse("Portfolio deleted successfully", HttpStatus.OK, SUCCESS, null);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error deleting portfolio: {}", e.getMessage(), e);
            Response response = getResponse("Error deleting portfolio: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    public ResponseEntity<Response> getPortfolioSummary(UUID portfolioId, UUID userId) {
        log.info("Fetching portfolio summary: {} for user: {}", portfolioId, userId);

        try {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            // Recalculate values
            positionService.recalculatePortfolioValue(portfolioId);
            portfolio = portfolioRepository.findById(portfolioId).orElse(portfolio);

            // Get positions
            List<PositionDTO> positions = positionRepository.findByPortfolioId(portfolioId).stream()
                    .map(positionMapper::toDTO)
                    .collect(Collectors.toList());

            // Calculate summary
            BigDecimal totalInvested = positions.stream()
                    .map(PositionDTO::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCurrentValue = positions.stream()
                    .map(p -> p.getCurrentValue() != null ? p.getCurrentValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(portfolio.getCashBalance());

            BigDecimal totalPnL = portfolio.getRealizedPnL().add(portfolio.getUnrealizedPnL());
            BigDecimal totalPnLPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                    ? totalPnL.divide(totalInvested, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            PortfolioSummaryDTO summary = PortfolioSummaryDTO.builder()
                    .portfolio(portfolioMapper.toDTO(portfolio))
                    .positions(positions)
                    .totalInvested(totalInvested)
                    .totalCurrentValue(totalCurrentValue)
                    .totalPnL(totalPnL)
                    .totalPnLPercent(totalPnLPercent)
                    .positionCount(positions.size())
                    .build();

            Response response = getResponse("Portfolio summary retrieved successfully", HttpStatus.OK, SUCCESS, summary);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching portfolio summary: {}", e.getMessage(), e);
            Response response = getResponse("Error fetching portfolio summary: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Response> depositCash(UUID portfolioId, UUID userId, BigDecimal amount) {
        log.info("Depositing cash: {} to portfolio: {} for user: {}", amount, portfolioId, userId);

        try {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            portfolio.setCashBalance(portfolio.getCashBalance().add(amount));
            portfolio.setTotalValue(portfolio.getTotalValue().add(amount));
            portfolio = portfolioRepository.save(portfolio);

            PortfolioDTO dto = portfolioMapper.toDTO(portfolio);
            Response response = getResponse("Cash deposited successfully", HttpStatus.OK, SUCCESS, dto);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error depositing cash: {}", e.getMessage(), e);
            Response response = getResponse("Error depositing cash: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Response> withdrawCash(UUID portfolioId, UUID userId, BigDecimal amount) {
        log.info("Withdrawing cash: {} from portfolio: {} for user: {}", amount, portfolioId, userId);

        try {
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            if (portfolio.getCashBalance().compareTo(amount) < 0) {
                Response response = getResponse("Insufficient cash balance", HttpStatus.BAD_REQUEST, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            portfolio.setCashBalance(portfolio.getCashBalance().subtract(amount));
            portfolio.setTotalValue(portfolio.getTotalValue().subtract(amount));
            portfolio = portfolioRepository.save(portfolio);

            PortfolioDTO dto = portfolioMapper.toDTO(portfolio);
            Response response = getResponse("Cash withdrawn successfully", HttpStatus.OK, SUCCESS, dto);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error withdrawing cash: {}", e.getMessage(), e);
            Response response = getResponse("Error withdrawing cash: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }
}
