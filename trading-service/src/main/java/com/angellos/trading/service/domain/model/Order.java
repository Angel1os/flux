package com.angellos.trading.service.domain.model;

import com.angellos.shared.enums.OrderSide;
import com.angellos.shared.enums.OrderStatus;
import com.angellos.shared.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal limitPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal executedPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal executedQuantity;

    @Version
    private Long version;

    @Column
    private UUID updatedBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
