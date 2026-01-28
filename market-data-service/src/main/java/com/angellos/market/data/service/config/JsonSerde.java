package com.angellos.market.data.service.config;

import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Custom JSON Serde for Kafka Streams.
 * Replaces deprecated org.springframework.kafka.support.serializer.JsonSerde.
 * Uses Jackson ObjectMapper for serialization/deserialization.
 */
@Slf4j
public class JsonSerde<T> implements Serde<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public JsonSerde(Class<T> type) {
        this.type = type;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Serializer<T> serializer() {
        return new JsonSerializer<>();
    }

    @Override
    public Deserializer<T> deserializer() {
        return new JsonDeserializer<>(type);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Configuration if needed
    }

    @Override
    public void close() {
        // Cleanup if needed
    }

    /**
     * Custom JSON Serializer
     */
    private class JsonSerializer<T> implements Serializer<T> {
        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            // No additional configuration needed
        }

        @Override
        public byte[] serialize(String topic, T data) {
            if (data == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (Exception e) {
                log.error("Failed to serialize to JSON for topic: {}. Error: {}", topic, e.getMessage(), e);
                throw new RuntimeException("Failed to serialize to JSON", e);
            }
        }

        @Override
        public void close() {
            // No resources to close
        }
    }

    /**
     * Custom JSON Deserializer
     */
    private class JsonDeserializer<T> implements Deserializer<T> {
        private final Class<T> targetType;

        public JsonDeserializer(Class<T> targetType) {
            this.targetType = targetType;
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            // Configuration if needed
        }

        @Override
        public T deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                return objectMapper.readValue(data, targetType);
            } catch (Exception e) {
                log.error("Failed to deserialize from JSON for topic: {}. Error: {}", topic, e.getMessage(), e);
                throw new RuntimeException("Failed to deserialize from JSON", e);
            }
        }

        @Override
        public void close() {
            // Cleanup if needed
        }
    }
}
