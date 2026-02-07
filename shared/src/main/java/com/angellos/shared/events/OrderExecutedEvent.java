package com.angellos.shared.events;

import com.angellos.shared.enums.OrderType;
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
    private OrderType orderType;  // BUY or SELL
    private BigDecimal executionPrice;
    private BigDecimal executionQuantity;
    private BigDecimal totalValue;
    private Instant timestamp;
    
    // Event metadata
    private String eventType = "ORDER_EXECUTED";
    private String serviceName = "trading-service";
}
