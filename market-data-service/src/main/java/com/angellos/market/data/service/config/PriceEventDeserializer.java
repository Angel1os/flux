package com.angellos.market.data.service.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Custom deserializer for price events using Jackson ObjectMapper.
 * Replaces deprecated JsonDeserializer.
 * Deserializes to Map<String, Object> for flexible handling.
 */
@Slf4j
public class PriceEventDeserializer implements Deserializer<Object> {

    private final ObjectMapper objectMapper;

    public PriceEventDeserializer() {
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
            // Deserialize to Map<String, Object> for flexible handling
            // The listener will convert it to PriceEventDTO
            return objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize price event from topic: {}. Error: {}", topic, e.getMessage(), e);
            log.error("Event data (first 200 chars): {}", new String(data, 0, Math.min(200, data.length)));
            throw new RuntimeException("Failed to deserialize price event: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // Cleanup if needed
    }
}
