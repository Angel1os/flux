package com.angellos.trading.service.domain.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read model for orders optimized for Redis caching.
 * This is the CQRS read model - separate from the write model (Order entity).
 * 
 * Key differences:
 * - Stored in Redis for fast reads
 * - Denormalized for query optimization
 * - Updated via events (eventual consistency)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("OrderReadModel")
public class OrderReadModel implements Serializable {

    @Id
    private UUID id;

    @Indexed
    private UUID userId;

    @Indexed
    private String symbol;

    private String type; // BUY, SELL

    @Indexed
    private String status; // PENDING, EXECUTED, etc.

    private String side; // MARKET, LIMIT, STOP

    private BigDecimal quantity;

    private BigDecimal limitPrice;

    private BigDecimal executedPrice;

    private BigDecimal executedQuantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
