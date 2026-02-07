package com.angellos.portfolio.service.listener;

import com.angellos.portfolio.service.controller.PortfolioWebSocketController;
import com.angellos.portfolio.service.domain.enums.TransactionType;
import com.angellos.portfolio.service.domain.model.Portfolio;
import com.angellos.portfolio.service.domain.model.Position;
import com.angellos.portfolio.service.repository.PortfolioRepository;
import com.angellos.portfolio.service.repository.PositionRepository;
import com.angellos.portfolio.service.service.DlqService;
import com.angellos.portfolio.service.service.TransactionService;
import com.angellos.shared.enums.OrderType;
import com.angellos.shared.events.OrderCancelledEvent;
import com.angellos.shared.events.OrderExecutedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.angellos.portfolio.service.config.KafkaTopicsConfig.TOPIC_ORDER_EVENTS;

/**
 * Event listener for order events from Trading Service.
 * Consumes OrderExecutedEvent and OrderCancelledEvent to update portfolios.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TransactionService transactionService;
    private final PortfolioWebSocketController webSocketController;
    private final DlqService dlqService;

    // Track retry attempts per message
    private final ConcurrentHashMap<String, AtomicInteger> retryAttempts = new ConcurrentHashMap<>();
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @PostConstruct
    public void init() {
        log.info("OrderEventListener initialized and ready to consume from topic: {}, groupId: portfolio-service", 
                TOPIC_ORDER_EVENTS);
    }

    @KafkaListener(topics = TOPIC_ORDER_EVENTS, groupId = "portfolio-service", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvent(
            @Payload ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        String messageIdentifier = String.format("%s-%d-%d", record.topic(), record.partition(), record.offset());
        int currentAttempts = retryAttempts.computeIfAbsent(messageIdentifier, k -> new AtomicInteger(0)).incrementAndGet();

        try {
            log.debug("Received order event: key={}, partition={}, offset={}, attempts={}",
                    record.key(), record.partition(), record.offset(), currentAttempts);

            Object value = record.value();
            
            if (value instanceof OrderExecutedEvent orderExecutedEvent) {
                handleOrderExecuted(orderExecutedEvent);
            } else if (value instanceof OrderCancelledEvent orderCancelledEvent) {
                handleOrderCancelled(orderCancelledEvent);
            } else if (value instanceof Map) {
                // Handle deserialized Map
                Map<String, Object> eventMap = (Map<String, Object>) value;
                String eventType = (String) eventMap.getOrDefault("eventType", "");
                
                if ("ORDER_EXECUTED".equals(eventType)) {
                    handleOrderExecutedFromMap(eventMap);
                } else if ("ORDER_CANCELLED".equals(eventType)) {
                    handleOrderCancelledFromMap(eventMap);
                } else {
                    log.warn("Unknown event type: {}. Sending to DLQ. Message ID: {}", eventType, messageIdentifier);
                    dlqService.sendToDlq(record.topic(), record.key(), record.value(), 
                            "Unknown event type: " + eventType, "N/A", currentAttempts);
                    acknowledgment.acknowledge();
                    retryAttempts.remove(messageIdentifier);
                    return;
                }
            } else {
                log.warn("Unknown event type: {}. Sending to DLQ. Message ID: {}", 
                        value != null ? value.getClass().getName() : "NULL", messageIdentifier);
                dlqService.sendToDlq(record.topic(), record.key(), record.value(), 
                        "Unknown event type", "N/A", currentAttempts);
                acknowledgment.acknowledge();
                retryAttempts.remove(messageIdentifier);
                return;
            }

            acknowledgment.acknowledge();
            retryAttempts.remove(messageIdentifier);
            log.info("Successfully processed event. Message ID: {}", messageIdentifier);

        } catch (Exception e) {
            log.error("Error processing order event. Message ID: {}, Attempts: {}. Error: {}",
                    messageIdentifier, currentAttempts, e.getMessage(), e);

            if (currentAttempts >= MAX_RETRY_ATTEMPTS) {
                log.error("Max retry attempts ({}) reached for message ID: {}. Sending to DLQ.",
                        MAX_RETRY_ATTEMPTS, messageIdentifier);
                dlqService.sendToDlq(record.topic(), record.key(), record.value(), 
                        e.getMessage(), getStackTrace(e), currentAttempts);
                acknowledgment.acknowledge();
                retryAttempts.remove(messageIdentifier);
            } else {
                log.warn("Retrying message ID: {}. Current attempts: {}", messageIdentifier, currentAttempts);
                // Do NOT acknowledge - Kafka will redeliver
            }
        }
    }

    @Transactional
    protected void handleOrderExecuted(OrderExecutedEvent event) {
        log.info("Processing OrderExecutedEvent - OrderId: {}, UserId: {}, Symbol: {}, Price: {}, Quantity: {}",
                event.getOrderId(), event.getUserId(), event.getSymbol(),
                event.getExecutionPrice(), event.getExecutionQuantity());

        try {
            // Get order type (BUY/SELL) from event
            OrderType orderType = event.getOrderType();
            if (orderType == null) {
                log.warn("Order type is null in event for order: {}. Skipping portfolio update.", event.getOrderId());
                return;
            }

            // Determine transaction type
            TransactionType transactionType = orderType == OrderType.BUY 
                    ? TransactionType.BUY 
                    : TransactionType.SELL;

            // Calculate amounts
            BigDecimal totalAmount = event.getTotalValue() != null 
                    ? event.getTotalValue() 
                    : event.getExecutionPrice().multiply(event.getExecutionQuantity());
            BigDecimal fees = BigDecimal.ZERO; // TODO: Get fees from order or calculate

            // Refetch portfolio with latest version to avoid optimistic locking conflicts
            Portfolio portfolio = getOrCreateDefaultPortfolio(event.getUserId());
            Portfolio finalPortfolio = portfolio;
            portfolio = portfolioRepository.findById(portfolio.getId())
                    .orElseThrow(() -> new IllegalStateException("Portfolio not found: " + finalPortfolio.getId()));

            // Update position (may update portfolio.realizedPnL for SELL)
            BigDecimal realizedPnL = updatePosition(portfolio, event.getSymbol(), event.getExecutionPrice(), 
                    event.getExecutionQuantity(), transactionType);

            // Update portfolio realized P&L if this was a sell
            if (realizedPnL != null && realizedPnL.compareTo(BigDecimal.ZERO) != 0) {
                portfolio.setRealizedPnL(portfolio.getRealizedPnL().add(realizedPnL));
            }

            // Update portfolio cash balance
            if (transactionType == TransactionType.BUY) {
                portfolio.setCashBalance(portfolio.getCashBalance().subtract(totalAmount.add(fees)));
            } else {
                portfolio.setCashBalance(portfolio.getCashBalance().add(totalAmount.subtract(fees)));
            }

            // Recalculate portfolio value
            BigDecimal positionsValue = positionRepository.findByPortfolioId(portfolio.getId()).stream()
                    .map(p -> p.getCurrentValue() != null ? p.getCurrentValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            portfolio.setTotalValue(portfolio.getCashBalance().add(positionsValue));

            // Save portfolio once with all updates
            portfolio = portfolioRepository.save(portfolio);

            // Create transaction record
            transactionService.createTransaction(
                    portfolio.getId(),
                    event.getOrderId(),
                    event.getSymbol(),
                    transactionType,
                    event.getExecutionQuantity(),
                    event.getExecutionPrice(),
                    totalAmount,
                    fees
            );

            // Send WebSocket update
            webSocketController.sendPortfolioUpdate(portfolio.getId(), portfolio);

            log.info("Successfully processed OrderExecutedEvent for order: {}", event.getOrderId());

        } catch (org.hibernate.StaleObjectStateException e) {
            log.warn("Optimistic locking conflict for portfolio. This may happen with concurrent updates. " +
                    "Event will be retried. OrderId: {}, Error: {}", event.getOrderId(), e.getMessage());
            throw e; // Re-throw to trigger retry logic
        } catch (Exception e) {
            log.error("Error processing OrderExecutedEvent: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger retry logic
        }
    }

    @Transactional
    protected void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Processing OrderCancelledEvent - OrderId: {}, UserId: {}, Reason: {}",
                event.getOrderId(), event.getUserId(), event.getReason());

        // For cancelled orders, we typically don't need to update portfolio
        // unless the order was already partially executed
        // This is a simplified implementation
        log.info("Order cancelled - no portfolio update needed for order: {}", event.getOrderId());
    }

    private void handleOrderExecutedFromMap(Map<String, Object> eventMap) {
        // Convert Map to OrderExecutedEvent
        OrderExecutedEvent event = OrderExecutedEvent.builder()
                .orderId(UUID.fromString(eventMap.get("orderId").toString()))
                .userId(UUID.fromString(eventMap.get("userId").toString()))
                .symbol((String) eventMap.get("symbol"))
                .orderType(eventMap.get("orderType") != null 
                        ? OrderType.valueOf(eventMap.get("orderType").toString()) 
                        : null)
                .executionPrice(new BigDecimal(eventMap.get("executionPrice").toString()))
                .executionQuantity(new BigDecimal(eventMap.get("executionQuantity").toString()))
                .totalValue(eventMap.get("totalValue") != null 
                        ? new BigDecimal(eventMap.get("totalValue").toString()) 
                        : null)
                .build();

        handleOrderExecuted(event);
    }

    private void handleOrderCancelledFromMap(Map<String, Object> eventMap) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(UUID.fromString(eventMap.get("orderId").toString()))
                .userId(UUID.fromString(eventMap.get("userId").toString()))
                .reason((String) eventMap.get("reason"))
                .build();

        handleOrderCancelled(event);
    }

    private Portfolio getOrCreateDefaultPortfolio(UUID userId) {
        // Get user's first portfolio or create a default one
        var portfolios = portfolioRepository.findByUserId(userId);
        
        if (portfolios.isEmpty()) {
            Portfolio portfolio = Portfolio.builder()
                    .userId(userId)
                    .name("Default Portfolio")
                    .cashBalance(BigDecimal.ZERO)
                    .totalValue(BigDecimal.ZERO)
                    .realizedPnL(BigDecimal.ZERO)
                    .unrealizedPnL(BigDecimal.ZERO)
                    .build();
            portfolio = portfolioRepository.save(portfolio);
            log.info("Created default portfolio for user: {}", userId);
            return portfolio;
        }

        return portfolios.get(0); // Return first portfolio
    }

    /**
     * Updates position and returns realized P&L if this was a sell transaction.
     * Returns null for buy transactions (no realized P&L).
     */
    protected BigDecimal updatePosition(Portfolio portfolio, String symbol, BigDecimal price,
                                  BigDecimal quantity, TransactionType type) {
        Position position = positionRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
                .orElse(null);

        if (type == TransactionType.BUY) {
            // Buy: Add to position
            if (position == null) {
                // Create new position
                position = Position.builder()
                        .portfolio(portfolio)
                        .symbol(symbol)
                        .quantity(quantity)
                        .averagePrice(price)
                        .totalCost(price.multiply(quantity))
                        .currentPrice(price)
                        .currentValue(price.multiply(quantity))
                        .unrealizedPnL(BigDecimal.ZERO)
                        .unrealizedPnLPercent(BigDecimal.ZERO)
                        .build();
            } else {
                // Update existing position
                BigDecimal newQuantity = position.getQuantity().add(quantity);
                BigDecimal newTotalCost = position.getTotalCost().add(price.multiply(quantity));
                BigDecimal newAveragePrice = newTotalCost.divide(newQuantity, 8, RoundingMode.HALF_UP);

                position.setQuantity(newQuantity);
                position.setAveragePrice(newAveragePrice);
                position.setTotalCost(newTotalCost);
                position.setCurrentPrice(price);
                position.setCurrentValue(price.multiply(newQuantity));
            }
            positionRepository.save(position);
            log.info("Position updated: symbol={}, quantity={}, averagePrice={}", 
                    symbol, position.getQuantity(), position.getAveragePrice());
            return null; // No realized P&L for buy
        } else {
            // Sell: Subtract from position
            if (position == null || position.getQuantity().compareTo(quantity) < 0) {
                throw new IllegalArgumentException("Insufficient position quantity for sell order");
            }

            BigDecimal newQuantity = position.getQuantity().subtract(quantity);
            
            // Calculate realized P&L
            BigDecimal realizedPnL = price.subtract(position.getAveragePrice())
                    .multiply(quantity);

            if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
                // Position fully closed
                positionRepository.delete(position);
                log.info("Position fully closed: {}", symbol);
            } else {
                // Update position
                position.setQuantity(newQuantity);
                position.setTotalCost(position.getAveragePrice().multiply(newQuantity));
                position.setCurrentPrice(price);
                position.setCurrentValue(price.multiply(newQuantity));
                positionRepository.save(position);
            }
            
            log.info("Position updated (sell): symbol={}, quantity={}, realizedPnL={}", 
                    symbol, newQuantity, realizedPnL);
            return realizedPnL; // Return realized P&L for sell
        }
    }


    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
