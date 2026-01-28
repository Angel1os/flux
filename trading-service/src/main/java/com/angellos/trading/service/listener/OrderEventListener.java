package com.angellos.trading.service.listener;

import com.angellos.trading.service.controller.OrderWebSocketController;
import com.angellos.trading.service.domain.readmodel.OrderReadModel;
import com.angellos.trading.service.events.OrderCancelledEvent;
import com.angellos.trading.service.events.OrderCreatedEvent;
import com.angellos.trading.service.events.OrderExecutedEvent;
import com.angellos.trading.service.events.OrderUpdatedEvent;
import com.angellos.trading.service.mapper.OrderReadModelMapper;
import com.angellos.trading.service.repository.OrderReadRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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

    @PostConstruct
    public void init() {
        log.info("OrderEventListener initialized and ready to consume from topic: order-events, groupId: trading-service");
    }

    @KafkaListener(topics = "order-events", groupId = "trading-service", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvent(
            @Payload Object event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment
    ) {
        try {
            if (event == null) {
                log.error("Event is NULL - deserialization may have failed!");
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }

            // Extract value from ConsumerRecord if needed
            Object actualEvent = event;
            if (event instanceof ConsumerRecord) {
                @SuppressWarnings("unchecked")
                ConsumerRecord<String, Object> record = (ConsumerRecord<String, Object>) event;
                actualEvent = record.value();
            }

            // Route to appropriate handler based on event type
            if (actualEvent instanceof OrderCreatedEvent orderCreatedEvent) {
                handleOrderCreated(orderCreatedEvent);
            } else if (actualEvent instanceof OrderUpdatedEvent orderUpdatedEvent) {
                handleOrderUpdated(orderUpdatedEvent);
            } else if (actualEvent instanceof OrderCancelledEvent orderCancelledEvent) {
                handleOrderCancelled(orderCancelledEvent);
            } else if (actualEvent instanceof OrderExecutedEvent orderExecutedEvent) {
                handleOrderExecuted(orderExecutedEvent);
            } else {
                log.warn("Unknown event type: {}", actualEvent != null ? actualEvent.getClass().getName() : "NULL");
            }

            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            // In production, you might want to send to a dead letter queue
            // For now, we'll acknowledge to prevent infinite retries
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
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
