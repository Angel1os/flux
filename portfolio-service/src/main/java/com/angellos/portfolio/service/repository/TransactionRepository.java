package com.angellos.portfolio.service.repository;

import com.angellos.portfolio.service.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByPortfolioId(UUID portfolioId);
    Page<Transaction> findByPortfolioIdOrderByTimestampDesc(UUID portfolioId, Pageable pageable);
    Optional<Transaction> findByOrderId(UUID orderId);
    List<Transaction> findByPortfolioIdAndTimestampBetween(
            UUID portfolioId, LocalDateTime startDate, LocalDateTime endDate);
    List<Transaction> findByPortfolioIdAndType(UUID portfolioId, com.angellos.portfolio.service.domain.enums.TransactionType type);
}
