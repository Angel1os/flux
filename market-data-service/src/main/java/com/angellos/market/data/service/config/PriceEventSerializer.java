package com.angellos.market.data.service.config;

import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Custom Kafka serializer for price events.
 * Uses Jackson ObjectMapper directly instead of deprecated JsonSerializer.
 */
public class PriceEventSerializer implements Serializer<Object> {

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
            throw new RuntimeException("Failed to serialize price event to JSON", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
