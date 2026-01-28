package com.angellos.shared.dto;

import com.angellos.shared.enums.OrderSide;
import com.angellos.shared.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotNull(message = "Order type is required")
    private OrderType type;
    
    @NotNull(message = "Order side is required")
    private OrderSide side;
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;
    
    private BigDecimal limitPrice;
}
