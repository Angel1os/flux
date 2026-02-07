package com.angellos.portfolio.service.service;

import com.angellos.portfolio.service.domain.dto.PortfolioDTO;
import com.angellos.portfolio.service.domain.dto.PortfolioSummaryDTO;
import com.angellos.shared.record.Response;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PortfolioService {

    ResponseEntity<Response> createPortfolio(UUID userId, String name);

    ResponseEntity<Response> getPortfolio(UUID portfolioId, UUID userId);

    ResponseEntity<Response> getAllPortfolios(UUID userId);

    ResponseEntity<Response> updatePortfolio(UUID portfolioId, UUID userId, String name);

    ResponseEntity<Response> deletePortfolio(UUID portfolioId, UUID userId);

    ResponseEntity<Response> getPortfolioSummary(UUID portfolioId, UUID userId);

    ResponseEntity<Response> depositCash(UUID portfolioId, UUID userId, BigDecimal amount);

    ResponseEntity<Response> withdrawCash(UUID portfolioId, UUID userId, BigDecimal amount);

}
