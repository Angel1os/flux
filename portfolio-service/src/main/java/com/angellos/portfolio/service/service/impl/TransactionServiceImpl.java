package com.angellos.portfolio.service.service.impl;

import com.angellos.portfolio.service.domain.dto.TransactionDTO;
import com.angellos.portfolio.service.domain.enums.TransactionStatus;
import com.angellos.portfolio.service.domain.enums.TransactionType;
import com.angellos.portfolio.service.domain.model.Portfolio;
import com.angellos.portfolio.service.domain.model.Transaction;
import com.angellos.portfolio.service.mapper.TransactionMapper;
import com.angellos.portfolio.service.repository.PortfolioRepository;
import com.angellos.portfolio.service.repository.TransactionRepository;
import com.angellos.portfolio.service.service.TransactionService;
import com.angellos.shared.record.Response;
import com.angellos.shared.utility.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.angellos.shared.enums.StatusCode.*;
import static com.angellos.shared.utility.AppUtils.getResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public ResponseEntity<Response> getTransactions(UUID portfolioId, UUID userId, Pageable pageable) {
        log.info("Fetching transactions for portfolio: {} for user: {}", portfolioId, userId);

        try {
            // Verify portfolio belongs to user
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            Page<Transaction> transactions = transactionRepository
                    .findByPortfolioIdOrderByTimestampDesc(portfolioId, pageable);

            var dtos = transactions.getContent().stream()
                    .map(transactionMapper::toDTO)
                    .collect(Collectors.toList());

            Response response = getResponse("Transactions retrieved successfully", HttpStatus.OK, SUCCESS, dtos);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching transactions: {}", e.getMessage(), e);
            Response response = getResponse("Error fetching transactions: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    public ResponseEntity<Response> getTransactionsByDateRange(UUID portfolioId, UUID userId, 
                                                               LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching transactions for portfolio: {} for user: {} from {} to {}", 
                portfolioId, userId, startDate, endDate);

        try {
            // Verify portfolio belongs to user
            Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                    .orElse(null);

            if (portfolio == null) {
                Response response = getResponse("Portfolio not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }

            var transactions = transactionRepository.findByPortfolioIdAndTimestampBetween(
                    portfolioId, startDate, endDate);

            var dtos = transactions.stream()
                    .map(transactionMapper::toDTO)
                    .collect(Collectors.toList());

            Response response = getResponse("Transactions retrieved successfully", HttpStatus.OK, SUCCESS, dtos);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching transactions: {}", e.getMessage(), e);
            Response response = getResponse("Error fetching transactions: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    @Transactional
    public TransactionDTO createTransaction(UUID portfolioId, UUID orderId, String symbol, 
                                           TransactionType type, java.math.BigDecimal quantity, 
                                           java.math.BigDecimal price, java.math.BigDecimal totalAmount, 
                                           java.math.BigDecimal fees) {
        log.info("Creating transaction: portfolioId={}, orderId={}, symbol={}, type={}", 
                portfolioId, orderId, symbol, type);

        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
        if (portfolio == null) {
            throw new IllegalArgumentException("Portfolio not found: " + portfolioId);
        }

        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .orderId(orderId)
                .symbol(symbol)
                .type(type)
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .fees(fees)
                .status(TransactionStatus.COMPLETED)
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created: {}", transaction.getId());

        return transactionMapper.toDTO(transaction);
    }
}
