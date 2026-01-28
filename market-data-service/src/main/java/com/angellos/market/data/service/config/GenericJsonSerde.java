package com.angellos.market.data.service.config;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Generic JSON Serde for Kafka Streams that can handle any Object type.
 * Replaces deprecated org.springframework.kafka.support.serializer.JsonSerde.
 * Uses Jackson ObjectMapper for serialization/deserialization.
 * 
 * This is useful when you need to deserialize to Map<String, Object> or generic types.
 */
@Slf4j
public class GenericJsonSerde implements Serde<Object> {

    private final ObjectMapper objectMapper;

    public GenericJsonSerde() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Serializer<Object> serializer() {
        return new GenericJsonSerializer();
    }

    @Override
    public Deserializer<Object> deserializer() {
        return new GenericJsonDeserializer();
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
     * Generic JSON Serializer
     */
    private class GenericJsonSerializer implements Serializer<Object> {
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
     * Generic JSON Deserializer - deserializes to Map<String, Object>
     */
    private class GenericJsonDeserializer implements Deserializer<Object> {
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
                // Deserialize to Map<String, Object> for generic handling
                return objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
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
