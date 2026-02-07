package com.angellos.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent implements Serializable {
    
    private UUID orderId;
    private UUID userId;
    private String reason;
    private Instant timestamp;
    
    // Event metadata
    private String eventType = "ORDER_CANCELLED";
    private String serviceName = "trading-service";
}
