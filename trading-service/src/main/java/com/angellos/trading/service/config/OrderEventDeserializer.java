package com.angellos.trading.service.config;

import com.angellos.trading.service.events.OrderCancelledEvent;
import com.angellos.trading.service.events.OrderCreatedEvent;
import com.angellos.trading.service.events.OrderExecutedEvent;
import com.angellos.trading.service.events.OrderUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Custom deserializer for order events using Jackson ObjectMapper.
 * Replaces deprecated JsonDeserializer.
 */
@Slf4j
public class OrderEventDeserializer implements Deserializer<Object> {

    private final ObjectMapper objectMapper;
    private static final String EVENT_TYPE_FIELD = "eventType";

    public OrderEventDeserializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Configuration if needed
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            // Read as a generic map to get the event type
            Map<String, Object> eventMap = objectMapper.readValue(data,
                    new TypeReference<Map<String, Object>>() {});

            String eventType = (String) eventMap.get(EVENT_TYPE_FIELD);

            // If eventType is null, infer from the data structure
            if (eventType == null || eventType.isEmpty()) {
                if (eventMap.containsKey("reason")) {
                    eventType = "ORDER_CANCELLED";
                } else if (eventMap.containsKey("executionPrice") || eventMap.containsKey("executionQuantity")) {
                    eventType = "ORDER_EXECUTED";
                } else {
                    eventType = "ORDER_CREATED";
                }
            }

            // Deserialize to the appropriate event class based on eventType
            return switch (eventType) {
                case "ORDER_CREATED" -> objectMapper.readValue(data, OrderCreatedEvent.class);
                case "ORDER_UPDATED" -> objectMapper.readValue(data, OrderUpdatedEvent.class);
                case "ORDER_CANCELLED" -> objectMapper.readValue(data, OrderCancelledEvent.class);
                case "ORDER_EXECUTED" -> objectMapper.readValue(data, OrderExecutedEvent.class);
                default -> {
                    log.warn("Unknown event type: {}, attempting to deserialize as OrderCreatedEvent", eventType);
                    yield objectMapper.readValue(data, OrderCreatedEvent.class);
                }
            };
        } catch (Exception e) {
            log.error("Failed to deserialize order event from topic: {}. Error: {}", topic, e.getMessage(), e);
            log.error("Event data (first 200 chars): {}", new String(data, 0, Math.min(200, data.length)));
            throw new RuntimeException("Failed to deserialize order event: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // Cleanup if needed
    }
}
