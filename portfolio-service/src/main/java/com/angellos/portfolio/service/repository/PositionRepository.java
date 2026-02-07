package com.angellos.portfolio.service.repository;

import com.angellos.portfolio.service.domain.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {

    List<Position> findByPortfolioId(UUID portfolioId);

    Optional<Position> findByPortfolioIdAndSymbol(UUID portfolioId, String symbol);

    List<Position> findByPortfolioIdAndQuantityGreaterThan(UUID portfolioId, BigDecimal zero);

    void deleteByPortfolioId(UUID portfolioId);

}
