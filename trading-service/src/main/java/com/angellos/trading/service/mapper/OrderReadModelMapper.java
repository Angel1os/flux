package com.angellos.trading.service.mapper;

import com.angellos.trading.service.domain.model.Order;
import com.angellos.trading.service.domain.readmodel.OrderReadModel;
import com.angellos.shared.events.OrderExecutedEvent;
import com.angellos.trading.service.events.OrderCancelledEvent;
import com.angellos.trading.service.events.OrderCreatedEvent;
import com.angellos.trading.service.events.OrderUpdatedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper to convert between Order (write model) and OrderReadModel (read model).
 * Also converts events to read models for CQRS.
 */
@Component
public class OrderReadModelMapper {

    /**
     * Convert Order entity to OrderReadModel
     */
    public OrderReadModel toReadModel(Order order) {
        if (order == null) {
            return null;
        }

        return OrderReadModel.builder()
                .id(order.getId())
                .userId(order.getCreatedBy())
                .symbol(order.getSymbol())
                .type(order.getType() != null ? order.getType().name() : null)
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .side(order.getSide() != null ? order.getSide().name() : null)
                .quantity(order.getQuantity())
                .limitPrice(order.getLimitPrice())
                .executedPrice(order.getExecutedPrice())
                .executedQuantity(order.getExecutedQuantity())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Convert OrderCreatedEvent to OrderReadModel
     */
    public OrderReadModel toReadModel(OrderCreatedEvent event) {
        if (event == null) {
            return null;
        }

        return OrderReadModel.builder()
                .id(event.getOrderId())
                .userId(event.getUserId())
                .symbol(event.getSymbol())
                .type(event.getType() != null ? event.getType().name() : null)
                .status("PENDING") // New orders start as PENDING
                .side(event.getSide() != null ? event.getSide().name() : null)
                .quantity(event.getQuantity())
                .limitPrice(event.getLimitPrice())
                .createdAt(convertInstantToLocalDateTime(event.getTimestamp()))
                .build();
    }

    /**
     * Convert OrderUpdatedEvent to OrderReadModel (updates existing)
     */
    public OrderReadModel toReadModel(OrderUpdatedEvent event) {
        if (event == null) {
            return null;
        }

        return OrderReadModel.builder()
                .id(event.getOrderId())
                .userId(event.getUserId())
                .symbol(event.getSymbol())
                .type(event.getType() != null ? event.getType().name() : null)
                .status("PENDING") // Updated orders remain PENDING unless executed/cancelled
                .side(event.getSide() != null ? event.getSide().name() : null)
                .quantity(event.getQuantity())
                .limitPrice(event.getLimitPrice())
                .updatedAt(convertInstantToLocalDateTime(event.getTimestamp()))
                .build();
    }

    /**
     * Convert OrderCancelledEvent to OrderReadModel (updates status)
     */
    public OrderReadModel toReadModel(OrderCancelledEvent event) {
        if (event == null) {
            return null;
        }

        return OrderReadModel.builder()
                .id(event.getOrderId())
                .userId(event.getUserId())
                .status("CANCELLED")
                .updatedAt(convertInstantToLocalDateTime(event.getTimestamp()))
                .build();
    }

    /**
     * Convert OrderExecutedEvent to OrderReadModel (updates execution details)
     */
    public OrderReadModel toReadModel(OrderExecutedEvent event) {
        if (event == null) {
            return null;
        }

        return OrderReadModel.builder()
                .id(event.getOrderId())
                .userId(event.getUserId())
                .symbol(event.getSymbol())
                .status("EXECUTED")
                .executedPrice(event.getExecutionPrice())
                .executedQuantity(event.getExecutionQuantity())
                .updatedAt(convertInstantToLocalDateTime(event.getTimestamp()))
                .build();
    }

    /**
     * Update existing OrderReadModel with data from OrderUpdatedEvent
     */
    public void updateReadModel(OrderReadModel existing, OrderUpdatedEvent event) {
        if (existing == null || event == null) {
            return;
        }

        existing.setSymbol(event.getSymbol());
        existing.setType(event.getType() != null ? event.getType().name() : null);
        existing.setSide(event.getSide() != null ? event.getSide().name() : null);
        existing.setQuantity(event.getQuantity());
        existing.setLimitPrice(event.getLimitPrice());
        existing.setUpdatedAt(convertInstantToLocalDateTime(event.getTimestamp()));
    }

    /**
     * Update existing OrderReadModel with cancellation status
     */
    public void updateReadModel(OrderReadModel existing, OrderCancelledEvent event) {
        if (existing == null || event == null) {
            return;
        }

        existing.setStatus("CANCELLED");
        existing.setUpdatedAt(convertInstantToLocalDateTime(event.getTimestamp()));
    }

    /**
     * Update existing OrderReadModel with execution details
     */
    public void updateReadModel(OrderReadModel existing, OrderExecutedEvent event) {
        if (existing == null || event == null) {
            return;
        }

        existing.setStatus("EXECUTED");
        existing.setExecutedPrice(event.getExecutionPrice());
        existing.setExecutedQuantity(event.getExecutionQuantity());
        existing.setUpdatedAt(convertInstantToLocalDateTime(event.getTimestamp()));
    }

    private LocalDateTime convertInstantToLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
