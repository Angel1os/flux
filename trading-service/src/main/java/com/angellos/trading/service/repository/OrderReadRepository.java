package com.angellos.trading.service.repository;

import com.angellos.trading.service.domain.readmodel.OrderReadModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Redis repository for OrderReadModel (CQRS read side).
 * Provides fast read operations from Redis cache.
 */
@Repository
public interface OrderReadRepository extends CrudRepository<OrderReadModel, UUID> {

    /**
     * Find all orders for a specific user
     */
    List<OrderReadModel> findByUserId(UUID userId);

    /**
     * Find all orders for a specific symbol
     */
    List<OrderReadModel> findBySymbol(String symbol);

    /**
     * Find all orders by status
     */
    List<OrderReadModel> findByStatus(String status);

    /**
     * Find all orders for a user with a specific status
     */
    List<OrderReadModel> findByUserIdAndStatus(UUID userId, String status);

    /**
     * Find all orders for a user with a specific symbol
     */
    List<OrderReadModel> findByUserIdAndSymbol(UUID userId, String symbol);
}
