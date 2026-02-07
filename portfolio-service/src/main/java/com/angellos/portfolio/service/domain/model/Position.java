package com.angellos.portfolio.service.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "positions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"portfolio_id", "symbol"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal averagePrice;

    @Column(name = "current_price", precision = 19, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "total_cost", nullable = false, precision = 19, scale = 8)
    private BigDecimal totalCost;

    @Column(name = "current_value", precision = 19, scale = 8)
    private BigDecimal currentValue;

    @Column(name = "unrealized_pnl", precision = 19, scale = 8)
    @Builder.Default
    private BigDecimal unrealizedPnL = BigDecimal.ZERO;

    @Column(name = "unrealized_pnl_percent", precision = 10, scale = 6)
    private BigDecimal unrealizedPnLPercent;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Version
    private Long version;
}
