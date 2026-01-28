package com.angellos.trading.service.listener;

import com.angellos.trading.service.controller.OrderWebSocketController;
import com.angellos.trading.service.domain.readmodel.OrderReadModel;
import com.angellos.trading.service.events.OrderCancelledEvent;
import com.angellos.trading.service.events.OrderCreatedEvent;
import com.angellos.trading.service.events.OrderExecutedEvent;
import com.angellos.trading.service.events.OrderUpdatedEvent;
import com.angellos.trading.service.mapper.OrderReadModelMapper;
import com.angellos.trading.service.repository.OrderReadRepository;
import com.angellos.trading.service.service.DlqService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Event listener for order events from Kafka.
 * This listener can be used to:
 * - Update read models (CQRS)
 * - Send notifications
 * - Update analytics
 * - Trigger other business processes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderReadRepository orderReadRepository;
    private final OrderReadModelMapper orderReadModelMapper;
    private final OrderWebSocketController webSocketController;
    private final DlqService dlqService;

    // Track retry attempts per message (key: topic-partition-offset)
    private final Map<String, AtomicInteger> retryAttempts = new ConcurrentHashMap<>();

    // Maximum retry attempts before sending to DLQ
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @PostConstruct
    public void init() {
        log.info("OrderEventListener initialized and ready to consume from topic: order-events, groupId: trading-service");
    }

    @KafkaListener(topics = "order-events", groupId = "trading-service", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvent(
            @Payload ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {
        String messageId = String.format("%s-%d-%d", record.topic(), record.partition(), record.offset());
        AtomicInteger attempts = retryAttempts.computeIfAbsent(messageId, k -> new AtomicInteger(0));
        int currentAttempt = attempts.incrementAndGet();

        try {
            log.debug("Processing order event (attempt {}/{}): key={}, partition={}, offset={}",
                    currentAttempt, MAX_RETRY_ATTEMPTS, record.key(), record.partition(), record.offset());

            Object event = record.value();

            if (event == null) {
                String errorMsg = "Event is NULL - deserialization may have failed!";
                log.error(errorMsg);
                // Invalid data - send to DLQ immediately (no retry)
                dlqService.sendToDlq(record, errorMsg, null);
                retryAttempts.remove(messageId);
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }

            // Route to appropriate handler based on event type
            if (event instanceof OrderCreatedEvent orderCreatedEvent) {
                handleOrderCreated(orderCreatedEvent);
            } else if (event instanceof OrderUpdatedEvent orderUpdatedEvent) {
                handleOrderUpdated(orderUpdatedEvent);
            } else if (event instanceof OrderCancelledEvent orderCancelledEvent) {
                handleOrderCancelled(orderCancelledEvent);
            } else if (event instanceof OrderExecutedEvent orderExecutedEvent) {
                handleOrderExecuted(orderExecutedEvent);
            } else {
                String errorMsg = String.format("Unknown event type: %s", event != null ? event.getClass().getName() : "NULL");
                log.warn(errorMsg);
                // Unknown event type - send to DLQ immediately (no retry)
                dlqService.sendToDlq(record, errorMsg, null);
                retryAttempts.remove(messageId);
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }

            // Success - clear retry counter and acknowledge
            retryAttempts.remove(messageId);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (IllegalArgumentException e) {
            // Data validation error - won't be fixed by retry, send to DLQ immediately
            log.warn("Data validation error - sending to DLQ immediately: {}", e.getMessage());
            dlqService.sendToDlq(record, "Data validation error: " + e.getMessage(), e);
            retryAttempts.remove(messageId);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Error processing order event (attempt {}/{}): {}",
                    currentAttempt, MAX_RETRY_ATTEMPTS, e.getMessage(), e);
            handleFailure(record, acknowledgment, messageId, currentAttempt, e);
        }
    }

    /**
     * Handle processing failure with retry logic and DLQ routing.
     */
    private void handleFailure(ConsumerRecord<String, Object> record,
                               Acknowledgment acknowledgment,
                               String messageId,
                               int currentAttempt,
                               Throwable exception) {
        if (currentAttempt >= MAX_RETRY_ATTEMPTS) {
            // Max retries exceeded - send to DLQ
            String errorMsg = String.format("Max retry attempts (%d) exceeded for message", MAX_RETRY_ATTEMPTS);
            log.error("{} - sending to DLQ: topic={}, partition={}, offset={}",
                    errorMsg, record.topic(), record.partition(), record.offset());
            dlqService.sendToDlq(record, errorMsg, exception);
            retryAttempts.remove(messageId);
            // Acknowledge to prevent infinite retries
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } else {
            // Retry will happen automatically via Kafka's retry mechanism
            // Don't acknowledge - let Kafka redeliver the message
            log.warn("Message will be retried (attempt {}/{})", currentAttempt, MAX_RETRY_ATTEMPTS);
            // Don't acknowledge - let Kafka handle retry
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent - OrderId: {}, UserId: {}, Symbol: {}", 
                event.getOrderId(), event.getUserId(), event.getSymbol());
        
        try {
            // Update read model (Redis cache) - CQRS
            log.debug("Converting event to read model for order: {}", event.getOrderId());
            OrderReadModel readModel = orderReadModelMapper.toReadModel(event);
            log.debug("Read model created: {}", readModel);
            
            log.debug("Saving read model to Redis for order: {}", event.getOrderId());
            OrderReadModel saved = orderReadRepository.save(readModel);
            log.info("Successfully updated read model in Redis for order: {}, saved model: {}", 
                    event.getOrderId(), saved != null ? saved.getId() : "null");
        } catch (Exception e) {
            log.error("Failed to update read model for OrderCreatedEvent - OrderId: {}, Error: {}", 
                    event.getOrderId(), e.getMessage(), e);
            // Don't throw - log error but acknowledge message to prevent infinite retries
        }
        
        // TODO: Send notification to user
        // Push WebSocket update
        OrderReadModel readModel = orderReadModelMapper.toReadModel(event);
        webSocketController.sendOrderUpdateToUser(event.getUserId(), readModel);
        log.info("Sent WebSocket update for order creation: {}", event.getOrderId());
        // TODO: Update analytics
        // TODO: Trigger portfolio service update
    }

    private void handleOrderUpdated(OrderUpdatedEvent event) {
        log.info("Processing OrderUpdatedEvent - OrderId: {}, UserId: {}, Symbol: {}", 
                event.getOrderId(), event.getUserId(), event.getSymbol());
        
        try {
            // Update read model (Redis cache) - CQRS
            OrderReadModel existing = orderReadRepository.findById(event.getOrderId()).orElse(null);
            if (existing != null) {
                orderReadModelMapper.updateReadModel(existing, event);
                OrderReadModel updated = orderReadRepository.save(existing);
                log.info("Updated read model for order: {}", event.getOrderId());
                
                // Push WebSocket update
                webSocketController.sendOrderUpdateToUser(event.getUserId(), updated);
                log.info("Sent WebSocket update for order update: {}", event.getOrderId());
            } else {
                // If read model doesn't exist, create it
                OrderReadModel readModel = orderReadModelMapper.toReadModel(event);
                OrderReadModel saved = orderReadRepository.save(readModel);
                log.info("Created read model for order: {}", event.getOrderId());
                
                // Push WebSocket update
                // TODO: Send notification to user
                webSocketController.sendOrderUpdateToUser(event.getUserId(), saved);
                log.info("Sent WebSocket update for order update: {}", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to update read model for OrderUpdatedEvent: {}", e.getMessage(), e);
        }
        
        // TODO: Update analytics
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Processing OrderCancelledEvent - OrderId: {}, UserId: {}, Reason: {}", 
                event.getOrderId(), event.getUserId(), event.getReason());
        
        try {
            // Update read model (Redis cache) - CQRS
            OrderReadModel existing = orderReadRepository.findById(event.getOrderId()).orElse(null);
            if (existing != null) {
                orderReadModelMapper.updateReadModel(existing, event);
                OrderReadModel updated = orderReadRepository.save(existing);
                log.info("Updated read model for cancelled order: {}", event.getOrderId());
                
                // Push WebSocket update
                // TODO: Send notification to user
                webSocketController.sendOrderUpdateToUser(event.getUserId(), updated);
                log.info("Sent WebSocket update for order cancellation: {}", event.getOrderId());
            } else {
                // If read model doesn't exist, create it with cancellation status
                OrderReadModel readModel = orderReadModelMapper.toReadModel(event);
                OrderReadModel saved = orderReadRepository.save(readModel);
                log.info("Created read model for cancelled order: {}", event.getOrderId());
                
                // Push WebSocket update
                webSocketController.sendOrderUpdateToUser(event.getUserId(), saved);
                log.info("Sent WebSocket update for order cancellation: {}", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to update read model for OrderCancelledEvent: {}", e.getMessage(), e);
        }
        
        // TODO: Update analytics
        // TODO: Release any held resources
    }

    private void handleOrderExecuted(OrderExecutedEvent event) {
        log.info("Processing OrderExecutedEvent - OrderId: {}, UserId: {}, Symbol: {}, Price: {}, Quantity: {}", 
                event.getOrderId(), event.getUserId(), event.getSymbol(), 
                event.getExecutionPrice(), event.getExecutionQuantity());
        
        try {
            // Update read model (Redis cache) - CQRS
            OrderReadModel existing = orderReadRepository.findById(event.getOrderId()).orElse(null);
            OrderReadModel updated;
            if (existing != null) {
                orderReadModelMapper.updateReadModel(existing, event);
                updated = orderReadRepository.save(existing);
                log.info("Updated read model for executed order: {}", event.getOrderId());
            } else {
                // If read model doesn't exist, create it with execution details
                OrderReadModel readModel = orderReadModelMapper.toReadModel(event);
                updated = orderReadRepository.save(readModel);
                log.info("Created read model for executed order: {}", event.getOrderId());
            }
            
            // Push WebSocket update (after read model is updated)
            // TODO: Send notification to user
            webSocketController.sendOrderUpdateToUser(event.getUserId(), updated);
            log.info("Sent WebSocket update for order execution: {}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to update read model for OrderExecutedEvent: {}", e.getMessage(), e);
        }
        
        // TODO: Update analytics
        // TODO: Trigger portfolio service update
        // TODO: Trigger settlement process
    }
}
