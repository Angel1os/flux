package com.angellos.portfolio.service.service;

import com.angellos.portfolio.service.domain.dto.TransactionDTO;
import com.angellos.portfolio.service.domain.enums.TransactionType;
import com.angellos.shared.record.Response;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    ResponseEntity<Response> getTransactions(UUID portfolioId, UUID userId, Pageable pageable);
    ResponseEntity<Response> getTransactionsByDateRange(UUID portfolioId, UUID userId, 
                                                       LocalDateTime startDate, LocalDateTime endDate);
    TransactionDTO createTransaction(UUID portfolioId, UUID orderId, String symbol,
                                     TransactionType type,
                                     BigDecimal quantity, BigDecimal price,
                                     BigDecimal totalAmount, BigDecimal fees);
}
