package com.angellos.market.data.service.processor;

import com.angellos.market.data.service.config.GenericJsonSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Kafka Streams processor for real-time price aggregation.
 * Processes price events from Kafka topics and performs aggregations:
 * - Average price per symbol (time windows)
 * - Price change calculations
 * - Volume aggregations
 */
@Component
@Slf4j
public class PriceStreamProcessor {

    @Value("${spring.kafka.streams.application-id}")
    private String applicationId;

    /**
     * Process price streams from Kafka.
     * Reads from input topic, performs aggregations, and writes to output topics.
     */
    @Bean
    public KStream<String, Object> processPriceStream(StreamsBuilder streamsBuilder) {
        log.info("Configuring Kafka Streams processor for price aggregation");

        // Input topic for price events
        String inputTopic = "price-events";
        
        // Output topics
        String aggregatedPricesTopic = "price-aggregated";
        String priceChangesTopic = "price-changes";

        // Create KStream from input topic
        // Using GenericJsonSerde for value deserialization
        KStream<String, Object> priceStream = streamsBuilder.stream(
                inputTopic,
                Consumed.with(Serdes.String(), new GenericJsonSerde())
        );

        // Process price events
        priceStream
                .filter((key, value) -> {
                    // Filter valid price events
                    if (value instanceof Map) {
                        Map<String, Object> priceMap = (Map<String, Object>) value;
                        return priceMap.containsKey("symbol") && priceMap.containsKey("price");
                    }
                    return false;
                })
                .mapValues(value -> {
                    // Transform to ensure consistent structure
                    if (value instanceof Map) {
                        Map<String, Object> priceMap = (Map<String, Object>) value;
                        // Add processing timestamp
                        priceMap.put("processedAt", System.currentTimeMillis());
                        return priceMap;
                    }
                    return value;
                })
                .peek((key, value) -> {
                    if (value instanceof Map) {
                        Map<String, Object> priceMap = (Map<String, Object>) value;
                        log.debug("Processing price event for symbol: {}, price: {}", 
                                priceMap.get("symbol"), priceMap.get("price"));
                    }
                });

        // Group by symbol for aggregations
        KGroupedStream<String, Object> groupedBySymbol = priceStream
                .map((key, value) -> {
                    if (value instanceof Map) {
                        Map<String, Object> priceMap = (Map<String, Object>) value;
                        String symbol = (String) priceMap.get("symbol");
                        return new KeyValue<>(symbol, value);
                    }
                    return new KeyValue<>(key, value);
                })
                .groupByKey(Grouped.with(Serdes.String(), new GenericJsonSerde()));

        // Calculate average price over 1-minute tumbling windows
        KTable<Windowed<String>, Object> averagePrices = groupedBySymbol
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                .aggregate(
                        () -> (Object) Map.<String, Object>of(
                                "totalPrice", BigDecimal.ZERO,
                                "count", 0L,
                                "totalVolume", 0L
                        ),
                        (key, value, aggregate) -> {
                            if (value instanceof Map) {
                                Map<String, Object> priceMap = (Map<String, Object>) value;
                                Map<String, Object> agg = (Map<String, Object>) aggregate;
                                
                                BigDecimal price = priceMap.get("price") != null 
                                        ? new BigDecimal(priceMap.get("price").toString())
                                        : BigDecimal.ZERO;
                                Long volume = priceMap.get("volume") != null
                                        ? ((Number) priceMap.get("volume")).longValue()
                                        : 0L;
                                
                                // Safely convert totalPrice from aggregate (might be Integer, Double, BigDecimal, etc.)
                                BigDecimal totalPrice = convertToBigDecimal(agg.get("totalPrice")).add(price);
                                Long count = ((Number) agg.get("count")).longValue() + 1;
                                Long totalVolume = ((Number) agg.get("totalVolume")).longValue() + volume;
                                
                                BigDecimal avgPrice = totalPrice.divide(BigDecimal.valueOf(count), 2, 
                                        java.math.RoundingMode.HALF_UP);
                                
                                return (Object) Map.of(
                                        "symbol", key,
                                        "averagePrice", avgPrice,
                                        "count", count,
                                        "totalVolume", totalVolume,
                                        "windowStart", System.currentTimeMillis() - 60000,
                                        "windowEnd", System.currentTimeMillis()
                                );
                            }
                            return aggregate;
                        },
                        Materialized.with(Serdes.String(), new GenericJsonSerde())
                );

        // Write aggregated prices to output topic
        averagePrices
                .toStream()
                .map((windowedKey, value) -> {
                    String key = windowedKey.key();
                    return new KeyValue<>(key, value);
                })
                .to(aggregatedPricesTopic, Produced.with(Serdes.String(), new GenericJsonSerde()));

        // Calculate price changes (compare current with previous)
        KTable<String, Object> priceChanges = groupedBySymbol
                .aggregate(
                        () -> (Object) Map.<String, Object>of(
                                "previousPrice", BigDecimal.ZERO,
                                "currentPrice", BigDecimal.ZERO,
                                "change", BigDecimal.ZERO,
                                "changePercent", BigDecimal.ZERO
                        ),
                        (key, value, aggregate) -> {
                            if (value instanceof Map) {
                                Map<String, Object> priceMap = (Map<String, Object>) value;
                                Map<String, Object> agg = (Map<String, Object>) aggregate;
                                
                                BigDecimal currentPrice = priceMap.get("price") != null
                                        ? new BigDecimal(priceMap.get("price").toString())
                                        : BigDecimal.ZERO;
                                
                                // Safely convert previousPrice from aggregate (might be Integer, Double, BigDecimal, etc.)
                                BigDecimal previousPrice = convertToBigDecimal(agg.get("previousPrice"));
                                
                                BigDecimal change = currentPrice.subtract(previousPrice);
                                BigDecimal changePercent = previousPrice.compareTo(BigDecimal.ZERO) > 0
                                        ? change.divide(previousPrice, 4, java.math.RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                        : BigDecimal.ZERO;
                                
                                return (Object) Map.of(
                                        "symbol", key,
                                        "previousPrice", previousPrice,
                                        "currentPrice", currentPrice,
                                        "change", change,
                                        "changePercent", changePercent,
                                        "timestamp", System.currentTimeMillis()
                                );
                            }
                            return aggregate;
                        },
                        Materialized.with(Serdes.String(), new GenericJsonSerde())
                );

        // Write price changes to output topic
        priceChanges
                .toStream()
                                .filter((key, value) -> {
                    // Only emit if there's a significant change
                    if (value instanceof Map) {
                        Map<String, Object> changeMap = (Map<String, Object>) value;
                        BigDecimal changePercent = convertToBigDecimal(changeMap.get("changePercent"));
                        return changePercent.abs().compareTo(BigDecimal.valueOf(0.01)) > 0; // > 0.01%
                    }
                    return false;
                })
                .to(priceChangesTopic, Produced.with(Serdes.String(), new GenericJsonSerde()));

        log.info("Kafka Streams processor configured - Input: {}, Outputs: {}, {}", 
                inputTopic, aggregatedPricesTopic, priceChangesTopic);

        return priceStream;
    }

    /**
     * Safely convert an Object to BigDecimal.
     * Handles Integer, Long, Double, String, and BigDecimal types.
     */
    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse BigDecimal from string: {}", value);
                return BigDecimal.ZERO;
            }
        }
        // Fallback: convert to string then parse
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to convert to BigDecimal: {} (type: {})", value, value.getClass().getName());
            return BigDecimal.ZERO;
        }
    }
}
