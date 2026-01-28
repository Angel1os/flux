package com.angellos.trading.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis configuration for CQRS read models.
 * Configures Redis template and enables Spring Data Redis repositories.
 * Uses custom JSON serializer instead of deprecated GenericJackson2JsonRedisSerializer.
 */
@Configuration
@EnableRedisRepositories(basePackages = "com.angellos.trading.service.repository")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use custom JSON serializer
        template.setValueSerializer(createJsonRedisSerializer());
        template.setHashValueSerializer(createJsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Creates a custom JSON Redis serializer using ObjectMapper.
     * This replaces the deprecated GenericJackson2JsonRedisSerializer.
     */
    private RedisSerializer<Object> createJsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        return new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object value) {
                if (value == null) {
                    return new byte[0];
                }
                try {
                    return objectMapper.writeValueAsBytes(value);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize value to JSON", e);
                }
            }

            @Override
            public Object deserialize(byte[] bytes) {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                try {
                    return objectMapper.readValue(bytes, Object.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize value from JSON", e);
                }
            }
        };
    }
}
