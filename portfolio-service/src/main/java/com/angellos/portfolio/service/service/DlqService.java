package com.angellos.portfolio.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.angellos.portfolio.service.config.KafkaTopicsConfig.TOPIC_PORTFOLIO_EVENTS_DLQ;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendToDlq(String originalTopic, String key, Object value, String errorMessage, String stackTrace, int retryAttempts) {
        log.warn("Sending message to DLQ. Original Topic: {}, Key: {}, Error: {}", originalTopic, key, errorMessage);

        try {
            Map<String, Object> dlqRecord = new HashMap<>();
            dlqRecord.put("originalTopic", originalTopic);
            dlqRecord.put("originalKey", key);
            dlqRecord.put("originalValue", value);
            dlqRecord.put("errorMessage", errorMessage);
            dlqRecord.put("stackTrace", stackTrace);
            dlqRecord.put("timestamp", Instant.now().toString());
            dlqRecord.put("retryAttempts", retryAttempts);

            kafkaTemplate.send(TOPIC_PORTFOLIO_EVENTS_DLQ, key, dlqRecord)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully sent message to DLQ. Topic: {}, Partition: {}, Offset: {}",
                                    TOPIC_PORTFOLIO_EVENTS_DLQ, result.getRecordMetadata().partition(), 
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send message to DLQ. Topic: {}, Key: {}, Error: {}",
                                    TOPIC_PORTFOLIO_EVENTS_DLQ, key, ex.getMessage(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Critical error: Failed to send message to DLQ itself. Original Topic: {}, Key: {}, Error: {}",
                    originalTopic, key, e.getMessage(), e);
        }
    }
}
