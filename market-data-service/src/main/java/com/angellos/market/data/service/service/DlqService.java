package com.angellos.market.data.service.service;

import com.angellos.market.data.service.config.KafkaTopicsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Dead Letter Queue (DLQ) service for handling failed Kafka messages.
 * Routes messages that fail processing after retries to a DLQ topic for analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DlqService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send a failed message to the DLQ topic with error metadata.
     *
     * @param originalRecord The original Kafka consumer record that failed
     * @param errorMessage   Error message describing the failure
     * @param exception      The exception that caused the failure (can be null)
     */
    public void sendToDlq(ConsumerRecord<String, Object> originalRecord, 
                         String errorMessage, 
                         Throwable exception) {
        try {
            // Create DLQ payload with original message + error metadata
            Map<String, Object> dlqPayload = new HashMap<>();
            
            // Original message data
            dlqPayload.put("originalTopic", originalRecord.topic());
            dlqPayload.put("originalPartition", originalRecord.partition());
            dlqPayload.put("originalOffset", originalRecord.offset());
            dlqPayload.put("originalKey", originalRecord.key());
            dlqPayload.put("originalValue", originalRecord.value());
            dlqPayload.put("originalTimestamp", originalRecord.timestamp());
            
            // Error metadata
            dlqPayload.put("errorMessage", errorMessage);
            dlqPayload.put("errorType", exception != null ? exception.getClass().getName() : "Unknown");
            dlqPayload.put("errorStackTrace", exception != null ? getStackTrace(exception) : null);
            dlqPayload.put("failedAt", LocalDateTime.now().toString());
            dlqPayload.put("consumerGroup", originalRecord.topic() + "-consumer");
            
            // Send to DLQ topic
            String dlqKey = originalRecord.key() != null 
                    ? originalRecord.key() + "-dlq-" + System.currentTimeMillis()
                    : "dlq-" + System.currentTimeMillis();
            
            kafkaTemplate.send(KafkaTopicsConfig.TOPIC_PRICE_EVENTS_DLQ, dlqKey, dlqPayload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send message to DLQ topic: {}", ex.getMessage(), ex);
                        } else {
                            log.warn("Message sent to DLQ: topic={}, partition={}, offset={}, error={}", 
                                    originalRecord.topic(), 
                                    originalRecord.partition(), 
                                    originalRecord.offset(), 
                                    errorMessage);
                        }
                    });
                    
        } catch (Exception e) {
            log.error("Critical error while sending to DLQ: {}", e.getMessage(), e);
            // If DLQ itself fails, log it but don't throw - we don't want to crash the consumer
        }
    }

    /**
     * Get stack trace as string (limited to first 1000 chars to avoid huge payloads)
     */
    private String getStackTrace(Throwable exception) {
        if (exception == null) {
            return null;
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString();
        // Limit stack trace size to avoid huge DLQ messages
        return stackTrace.length() > 1000 ? stackTrace.substring(0, 1000) + "... (truncated)" : stackTrace;
    }
}
