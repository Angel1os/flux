package com.angellos.market.data.service.listener;

import com.angellos.market.data.service.controller.PriceWebSocketController;
import com.angellos.market.data.service.domain.dto.PriceDTO;
import com.angellos.market.data.service.domain.dto.PriceEventDTO;
import com.angellos.market.data.service.domain.model.Price;
import com.angellos.market.data.service.mapper.PriceMapper;
import com.angellos.market.data.service.service.DlqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka consumer for price events.
 * Consumes from 'price-events' topic and:
 * 1. Saves prices to PostgreSQL database
 * 2. Sends WebSocket updates to subscribed clients
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PriceEventListener {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final PriceMapper priceMapper;
    private final PriceWebSocketController webSocketController;
    private final DlqService dlqService;

    // Track retry attempts per message (key: topic-partition-offset)
    private final Map<String, AtomicInteger> retryAttempts = new ConcurrentHashMap<>();
    
    // Maximum retry attempts before sending to DLQ
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @KafkaListener(
            topics = "price-events",
            groupId = "market-data-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePriceEvent(
            @Payload ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {
        
        String messageId = String.format("%s-%d-%d", record.topic(), record.partition(), record.offset());
        AtomicInteger attempts = retryAttempts.computeIfAbsent(messageId, k -> new AtomicInteger(0));
        int currentAttempt = attempts.incrementAndGet();
        
        try {
            log.debug("Processing price event (attempt {}/{}): key={}, partition={}, offset={}", 
                    currentAttempt, MAX_RETRY_ATTEMPTS, record.key(), record.partition(), record.offset());

            // Extract the actual value (could be Map or PriceEventDTO)
            Object value = record.value();
            PriceEventDTO priceEvent;

            if (value instanceof Map) {
                // Deserialize from Map (from GenericJsonSerde)
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) value;
                priceEvent = mapToPriceEvent(valueMap);
            } else if (value instanceof PriceEventDTO) {
                priceEvent = (PriceEventDTO) value;
            } else {
                String errorMsg = String.format("Unknown price event type: %s", value != null ? value.getClass().getName() : "null");
                log.warn(errorMsg);
                // Invalid data format - send to DLQ immediately (no retry)
                dlqService.sendToDlq(record, errorMsg, null);
                retryAttempts.remove(messageId);
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }

            if (priceEvent == null || priceEvent.getSymbol() == null) {
                String errorMsg = String.format("Invalid price event: %s", priceEvent);
                log.warn(errorMsg);
                // Invalid data - send to DLQ immediately (no retry)
                dlqService.sendToDlq(record, errorMsg, null);
                retryAttempts.remove(messageId);
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }

            // Convert PriceEventDTO to Price entity
            Price price = Price.builder()
                    .id(UUID.randomUUID())
                    .symbol(priceEvent.getSymbol())
                    .price(priceEvent.getPrice())
                    .volume(priceEvent.getVolume())
                    .changePercent(priceEvent.getChangePercent())
                    .high24h(priceEvent.getHigh24h())
                    .low24h(priceEvent.getLow24h())
                    .timestamp(priceEvent.getTimestamp() != null 
                            ? priceEvent.getTimestamp() 
                            : java.time.LocalDateTime.now())
                    .source(priceEvent.getSource())
                    .build();

            // Save to PostgreSQL using R2dbcEntityTemplate for explicit transaction management
            // Block with timeout to ensure the save completes and commits
            Price savedPrice = r2dbcEntityTemplate.insert(price)
                    .doOnSuccess(saved -> {
                        log.info("Saved price to database: symbol={}, price={}, id={}", 
                                saved.getSymbol(), saved.getPrice(), saved.getId());
                    })
                    .doOnError(error -> {
                        log.error("Error saving price to database: {}", error.getMessage(), error);
                    })
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                            .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
                    .block(Duration.ofSeconds(5)); // Block with timeout to wait for save to complete

            if (savedPrice != null) {
                log.info("Price successfully persisted: symbol={}, id={}", 
                        savedPrice.getSymbol(), savedPrice.getId());
                
                // Convert to DTO for WebSocket
                PriceDTO priceDTO = priceMapper.toDTO(savedPrice);
                
                // Send WebSocket update
                webSocketController.sendPriceUpdate(priceEvent.getSymbol(), priceDTO);
                
                // Success - clear retry counter and acknowledge
                retryAttempts.remove(messageId);
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
            } else {
                String errorMsg = "Failed to save price to database - price was null after block";
                log.error(errorMsg);
                handleFailure(record, acknowledgment, messageId, currentAttempt, 
                        new RuntimeException(errorMsg));
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
            log.error("Error processing price event (attempt {}/{}): {}", 
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

    /**
     * Convert Map (from Kafka) to PriceEventDTO
     */
    private PriceEventDTO mapToPriceEvent(Map<String, Object> map) {
        try {
            return PriceEventDTO.builder()
                    .symbol((String) map.get("symbol"))
                    .price(map.get("price") != null 
                            ? new java.math.BigDecimal(map.get("price").toString()) 
                            : null)
                    .volume(map.get("volume") != null 
                            ? ((Number) map.get("volume")).longValue() 
                            : null)
                    .changePercent(map.get("changePercent") != null 
                            ? new java.math.BigDecimal(map.get("changePercent").toString()) 
                            : null)
                    .high24h(map.get("high24h") != null 
                            ? new java.math.BigDecimal(map.get("high24h").toString()) 
                            : null)
                    .low24h(map.get("low24h") != null 
                            ? new java.math.BigDecimal(map.get("low24h").toString()) 
                            : null)
                    .timestamp(parseTimestamp(map.get("timestamp")))
                    .source((String) map.get("source"))
                    .build();
        } catch (Exception e) {
            log.error("Error converting Map to PriceEventDTO: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse timestamp from various formats
     */
    private java.time.LocalDateTime parseTimestamp(Object timestamp) {
        if (timestamp == null) {
            return java.time.LocalDateTime.now();
        }
        try {
            if (timestamp instanceof String) {
                return java.time.LocalDateTime.parse((String) timestamp);
            } else if (timestamp instanceof java.time.LocalDateTime) {
                return (java.time.LocalDateTime) timestamp;
            } else if (timestamp instanceof java.time.Instant) {
                return java.time.LocalDateTime.ofInstant(
                        (java.time.Instant) timestamp,
                        java.time.ZoneId.systemDefault()
                );
            }
        } catch (Exception e) {
            log.debug("Failed to parse timestamp: {}", e.getMessage());
        }
        return java.time.LocalDateTime.now();
    }
}
