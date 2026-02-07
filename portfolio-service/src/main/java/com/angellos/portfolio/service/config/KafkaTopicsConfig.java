package com.angellos.portfolio.service.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicsConfig {

    public static final String TOPIC_ORDER_EVENTS = "order-events";
    public static final String TOPIC_PORTFOLIO_EVENTS = "portfolio-events";
    public static final String TOPIC_PORTFOLIO_EVENTS_DLQ = "portfolio-events-dlq";

    private static final int DEFAULT_PARTITIONS = 3;
    private static final short DEFAULT_REPLICATION_FACTOR = 1;

    @Bean
    public KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic portfolioEventsDlqTopic() {
        return new NewTopic(TOPIC_PORTFOLIO_EVENTS_DLQ, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR);
    }
}
