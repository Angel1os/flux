package com.angellos.portfolio.service.domain.dto;

import com.angellos.portfolio.service.domain.enums.TransactionStatus;
import com.angellos.portfolio.service.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private UUID id;
    private UUID portfolioId;
    private UUID orderId;
    private String symbol;
    private TransactionType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private BigDecimal fees;
    private TransactionStatus status;
    private LocalDateTime timestamp;
}
