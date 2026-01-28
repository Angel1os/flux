package com.angellos.trading.service.events;

import com.angellos.shared.enums.OrderSide;
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
public class OrderUpdatedEvent implements Serializable {
    
    private UUID orderId;
    private UUID userId;
    private String symbol;
    private OrderType type;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private Instant timestamp;
    
    // Event metadata
    private String eventType = "ORDER_UPDATED";
    private String serviceName = "trading-service";
}
