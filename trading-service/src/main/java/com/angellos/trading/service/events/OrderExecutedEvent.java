package com.angellos.trading.service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderExecutedEvent implements Serializable {
    
    private UUID orderId;
    private UUID userId;
    private String symbol;
    private BigDecimal executionPrice;
    private BigDecimal executionQuantity;
    private BigDecimal totalValue;
    private Instant timestamp;
    
    // Event metadata
    private String eventType = "ORDER_EXECUTED";
    private String serviceName = "trading-service";
}
