package com.angellos.market.data.service.service;

import com.angellos.market.data.service.domain.dto.PriceEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing price events to Kafka.
 * This is the single source of truth for emitting price events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String PRICE_EVENTS_TOPIC = "price-events";

    /**
     * Publish a price event to Kafka.
     * The event will be consumed by:
     * 1. Kafka Streams (for aggregation)
     * 2. PriceEventListener (for saving to DB and WebSocket updates)
     */
    public void publishPriceEvent(PriceEventDTO priceEvent) {
        try {
            String key = priceEvent.getSymbol(); // Use symbol as key for partitioning
            kafkaTemplate.send(PRICE_EVENTS_TOPIC, key, priceEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Published price event for symbol {}: price={}", 
                                    priceEvent.getSymbol(), priceEvent.getPrice());
                        } else {
                            log.error("Failed to publish price event for symbol {}: {}", 
                                    priceEvent.getSymbol(), ex.getMessage(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing price event to Kafka: {}", e.getMessage(), e);
        }
    }
}
