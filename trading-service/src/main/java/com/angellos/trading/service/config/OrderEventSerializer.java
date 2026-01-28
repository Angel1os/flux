package com.angellos.trading.service.config;

import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Custom Kafka serializer for order events.
 * Uses Jackson ObjectMapper directly instead of deprecated JsonSerializer.
 */
public class OrderEventSerializer implements Serializer<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No additional configuration needed
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order event to JSON", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
