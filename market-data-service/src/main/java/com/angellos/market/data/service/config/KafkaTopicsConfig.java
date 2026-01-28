package com.angellos.market.data.service.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates required Kafka topics on startup (if they don't already exist).
 *
 * This avoids manual topic creation via CLI/Offset Explorer.
 */
@Configuration
public class KafkaTopicsConfig {

    public static final String TOPIC_PRICE_EVENTS = "price-events";
    public static final String TOPIC_PRICE_AGGREGATED = "price-aggregated";
    public static final String TOPIC_PRICE_CHANGES = "price-changes";

    // Local dev defaults (tweak anytime)
    private static final int DEFAULT_PARTITIONS = 3;
    private static final short DEFAULT_REPLICATION_FACTOR = 1;

    @Bean
    public KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic priceEventsTopic() {
        return new NewTopic(TOPIC_PRICE_EVENTS, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR);
    }

    @Bean
    public NewTopic priceAggregatedTopic() {
        return new NewTopic(TOPIC_PRICE_AGGREGATED, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR);
    }

    @Bean
    public NewTopic priceChangesTopic() {
        return new NewTopic(TOPIC_PRICE_CHANGES, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR);
    }
}

