package com.angellos.trading.service.service.impl;

import com.angellos.shared.dto.OrderRequest;
import com.angellos.shared.enums.OrderSide;
import com.angellos.shared.enums.OrderStatus;
import com.angellos.shared.record.Response;
import com.angellos.trading.service.domain.model.Order;
import com.angellos.shared.events.OrderExecutedEvent;
import com.angellos.trading.service.events.OrderCancelledEvent;
import com.angellos.trading.service.events.OrderCreatedEvent;
import com.angellos.trading.service.events.OrderUpdatedEvent;
import com.angellos.trading.service.repository.OrderRepository;
import com.angellos.trading.service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.angellos.shared.enums.StatusCode.*;
import static com.angellos.shared.utility.AppConstants.*;
import static com.angellos.shared.utility.AppUtils.*;
import static com.angellos.shared.utility.SecurityUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @Override
    @Transactional
    public ResponseEntity<Response> createOrder(OrderRequest orderRequest) {
        log.info("Creating order for symbol: {}, type: {}",
                orderRequest.getSymbol(), orderRequest.getType());
        Response response;

        try {
            validateOrder(orderRequest);

            UUID userId = getCurrentUserId();

            Order order = Order.builder()
                    .symbol(orderRequest.getSymbol())
                    .type(orderRequest.getType())
                    .side(orderRequest.getSide())
                    .quantity(orderRequest.getQuantity())
                    .limitPrice(orderRequest.getLimitPrice())
                    .status(OrderStatus.PENDING)
                    .createdBy(userId)
                    .build();

            order = orderRepository.save(order);
            log.info("Order created successfully with id: {} by user: {}", order.getId(), userId);

            publishOrderCreatedEvent(order);

            response = getResponse(SAVED_MESSAGE, HttpStatus.CREATED, SUCCESS, order);
            return response.toResponseEntity();
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating order: {}", e.getMessage());
            response = getResponse("Validation error: " + e.getMessage(), HttpStatus.BAD_REQUEST, CLIENT_ERROR, null);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            response = getResponse("Error creating order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Response> updateOrder(UUID id, OrderRequest orderRequest) {
        log.info("Updating order: {}", id);
        Response response;

        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

            if (order.getStatus() == OrderStatus.EXECUTED || order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot update order with status: " + order.getStatus());
            }

            validateOrder(orderRequest);

            UUID userId = getCurrentUserId();
            if (userId != null) {
                order.setUpdatedBy(userId);
            }

            order.setSymbol(orderRequest.getSymbol());
            order.setType(orderRequest.getType());
            order.setSide(orderRequest.getSide());
            order.setQuantity(orderRequest.getQuantity());
            order.setLimitPrice(orderRequest.getLimitPrice());

            order = orderRepository.save(order);
            log.info("Order updated successfully: {} by user: {}", id, userId);

            publishOrderUpdatedEvent(order);

            response = getResponse(UPDATED_MESSAGE, HttpStatus.OK, SUCCESS, order);
            return response.toResponseEntity();
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating order: {}", e.getMessage());
            response = getResponse("Validation error: " + e.getMessage(), HttpStatus.BAD_REQUEST, CLIENT_ERROR, null);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error updating order {}: {}", id, e.getMessage(), e);
            response = getResponse("Error updating order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Response> cancelOrder(UUID id) {
        log.info("Cancelling order: {}", id);
        Response response;

        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

            if (order.getStatus() == OrderStatus.EXECUTED) {
                throw new IllegalArgumentException("Cannot cancel executed order");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Order is already cancelled");
            }

            order.setStatus(OrderStatus.CANCELLED);
            order = orderRepository.save(order);
            log.info("Order cancelled successfully: {}", id);

            publishOrderCancelledEvent(order, "User cancelled order");

            response = getResponse("Order cancelled successfully", HttpStatus.OK, SUCCESS, order);
            return response.toResponseEntity();
        } catch (IllegalArgumentException e) {
            log.warn("Validation error cancelling order: {}", e.getMessage());
            response = getResponse("Validation error: " + e.getMessage(), HttpStatus.BAD_REQUEST, CLIENT_ERROR, null);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error cancelling order {}: {}", id, e.getMessage(), e);
            response = getResponse("Error cancelling order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }


    @Override
    @Transactional
    public ResponseEntity<Response> executeOrder(UUID id, BigDecimal executionPrice, BigDecimal executionQuantity) {
        log.info("Executing order: {} with price: {}, quantity: {}", id, executionPrice, executionQuantity);
        Response response;

        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot execute cancelled order");
            }
            if (order.getStatus() == OrderStatus.EXECUTED &&
                    order.getExecutedQuantity() != null &&
                    order.getExecutedQuantity().compareTo(order.getQuantity()) >= 0) {
                throw new IllegalArgumentException("Order is already fully executed");
            }


            if (executionPrice == null || executionPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Execution price must be greater than zero");
            }
            if (executionQuantity == null || executionQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Execution quantity must be greater than zero");
            }
            if (executionQuantity.compareTo(order.getQuantity()) > 0) {
                throw new IllegalArgumentException("Execution quantity cannot exceed order quantity");
            }

            /**
             * Calculating remaining quantity
             */
            BigDecimal currentExecutedQty = order.getExecutedQuantity() != null ? order.getExecutedQuantity() : BigDecimal.ZERO;
            BigDecimal newExecutedQty = currentExecutedQty.add(executionQuantity);
            BigDecimal remainingQty = order.getQuantity().subtract(newExecutedQty);

            order.setExecutedPrice(executionPrice);
            order.setExecutedQuantity(newExecutedQty);

            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                order.setStatus(OrderStatus.EXECUTED);
                log.info("Order fully executed: {}", id);
            } else {
                order.setStatus(OrderStatus.PARTIALLY_EXECUTED);
                log.info("Order partially executed: {} (remaining: {})", id, remainingQty);
            }

            order = orderRepository.save(order);
            log.info("Order execution saved: {}", id);

            publishOrderExecutedEvent(order, executionPrice, executionQuantity);

            response = getResponse("Order executed successfully", HttpStatus.OK, SUCCESS, order);
            return response.toResponseEntity();
        } catch (IllegalArgumentException e) {
            log.warn("Validation error executing order: {}", e.getMessage());
            response = getResponse("Validation error: " + e.getMessage(), HttpStatus.BAD_REQUEST, CLIENT_ERROR, null);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error executing order {}: {}", id, e.getMessage(), e);
            response = getResponse("Error executing order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }


    private void validateOrder(OrderRequest orderRequest) {
        if (orderRequest.getQuantity() == null || orderRequest.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (orderRequest.getSymbol() == null || orderRequest.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        /**
         * Validating limit price for LIMIT orders
         */
        if (orderRequest.getSide() == OrderSide.LIMIT) {
            if (orderRequest.getLimitPrice() == null || orderRequest.getLimitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Limit price is required for LIMIT orders");
            }
        }
    }



    /**
     * Publishes OrderCreatedEvent to Kafka
     */
    private void publishOrderCreatedEvent(Order order) {
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getCreatedBy())
                    .symbol(order.getSymbol())
                    .type(order.getType())
                    .side(order.getSide())
                    .quantity(order.getQuantity())
                    .limitPrice(order.getLimitPrice())
                    .timestamp(Instant.now())
                    .eventType("ORDER_CREATED")
                    .build();

            kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId().toString(), event);
            log.info("Published OrderCreatedEvent for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent for order {}: {}", order.getId(), e.getMessage(), e);
            // Don't throw exception - event publishing failure shouldn't break the transaction
        }
    }

    /**
     * Publishes OrderUpdatedEvent to Kafka
     */
    private void publishOrderUpdatedEvent(Order order) {
        try {
            OrderUpdatedEvent event = OrderUpdatedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getCreatedBy())
                    .symbol(order.getSymbol())
                    .type(order.getType())
                    .side(order.getSide())
                    .quantity(order.getQuantity())
                    .limitPrice(order.getLimitPrice())
                    .timestamp(Instant.now())
                    .eventType("ORDER_UPDATED")
                    .build();

            kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId().toString(), event);
            log.info("Published OrderUpdatedEvent for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to publish OrderUpdatedEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes OrderCancelledEvent to Kafka
     */
    private void publishOrderCancelledEvent(Order order, String reason) {
        try {
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getCreatedBy())
                    .reason(reason)
                    .timestamp(Instant.now())
                    .eventType("ORDER_CANCELLED")
                    .build();

            kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId().toString(), event);
            log.info("Published OrderCancelledEvent for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to publish OrderCancelledEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes OrderExecutedEvent to Kafka
     * This method can be called when an order is executed (e.g., by an order matching engine)
     */
    private void publishOrderExecutedEvent(Order order, BigDecimal executionPrice, BigDecimal executionQuantity) {
        try {
            BigDecimal totalValue = executionPrice.multiply(executionQuantity);

            OrderExecutedEvent event = OrderExecutedEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getCreatedBy())
                    .symbol(order.getSymbol())
                    .orderType(order.getType())  // Include orderType (BUY/SELL) in the event
                    .executionPrice(executionPrice)
                    .executionQuantity(executionQuantity)
                    .totalValue(totalValue)
                    .timestamp(Instant.now())
                    .eventType("ORDER_EXECUTED")
                    .build();

            kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getId().toString(), event);
            log.info("Published OrderExecutedEvent for order: {} with type: {}", order.getId(), order.getType());
        } catch (Exception e) {
            log.error("Failed to publish OrderExecutedEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

}
