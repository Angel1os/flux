package com.angellos.market.data.service.listener;

import com.angellos.market.data.service.controller.PriceWebSocketController;
import com.angellos.market.data.service.domain.dto.PriceDTO;
import com.angellos.market.data.service.domain.dto.PriceEventDTO;
import com.angellos.market.data.service.domain.model.Price;
import com.angellos.market.data.service.mapper.PriceMapper;
import com.angellos.market.data.service.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

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

    private final PriceRepository priceRepository;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final PriceMapper priceMapper;
    private final PriceWebSocketController webSocketController;

    @KafkaListener(
            topics = "price-events",
            groupId = "market-data-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePriceEvent(
            @Payload ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("Received price event: key={}, partition={}, offset={}", 
                    record.key(), record.partition(), record.offset());

            // Extract the actual value (could be Map or PriceEventDTO)
            Object value = record.value();
            PriceEventDTO priceEvent;

            if (value instanceof Map) {
                // Deserialize from Map (from GenericJsonSerde)
                priceEvent = mapToPriceEvent((Map<String, Object>) value);
            } else if (value instanceof PriceEventDTO) {
                priceEvent = (PriceEventDTO) value;
            } else {
                log.warn("Unknown price event type: {}", value.getClass().getName());
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }

            if (priceEvent == null || priceEvent.getSymbol() == null) {
                log.warn("Invalid price event: {}", priceEvent);
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
                
                // Acknowledge message only after successful save
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
            } else {
                log.error("Failed to save price to database - price was null after block");
                // Don't acknowledge on failure to allow retry
            }

        } catch (Exception e) {
            log.error("Error processing price event: {}", e.getMessage(), e);
            // Don't acknowledge on exception - let Kafka retry or move to DLQ
            // Only acknowledge if it's a data validation error that won't be fixed by retry
            if (acknowledgment != null && e instanceof IllegalArgumentException) {
                // Acknowledge only for data validation errors (bad data format)
                log.warn("Acknowledging message with invalid data to avoid infinite retries");
                acknowledgment.acknowledge();
            }
            // Otherwise, let the exception propagate or handle via DLQ
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
