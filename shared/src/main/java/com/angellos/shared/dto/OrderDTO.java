package com.angellos.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private UUID id;
    private String symbol;
    private String type; // BUY, SELL
    private String status; // PENDING, EXECUTED, etc.
    private String side; // MARKET, LIMIT, STOP
    private BigDecimal quantity;
    private BigDecimal limitPrice;
}
