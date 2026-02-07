package com.angellos.portfolio.service.config;

import com.angellos.shared.events.OrderCancelledEvent;
import com.angellos.shared.events.OrderExecutedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

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
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Ensure enums are deserialized from their string names
        this.objectMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, false);
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
                case "ORDER_EXECUTED" -> objectMapper.readValue(data, OrderExecutedEvent.class);
                case "ORDER_CANCELLED" -> objectMapper.readValue(data, OrderCancelledEvent.class);
                default -> {
                    log.warn("Unknown event type: {}, attempting to deserialize as OrderExecutedEvent", eventType);
                    yield objectMapper.readValue(data, OrderExecutedEvent.class);
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
