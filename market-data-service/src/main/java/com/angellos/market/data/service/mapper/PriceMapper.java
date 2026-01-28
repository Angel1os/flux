package com.angellos.market.data.service.mapper;

import com.angellos.market.data.service.domain.dto.PriceDTO;
import com.angellos.market.data.service.domain.dto.PriceStreamDTO;
import com.angellos.market.data.service.domain.model.Price;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper to convert between Price entity, PriceDTO, and PriceStreamDTO.
 * Handles conversions for reactive operations and WebSocket streaming.
 */
@Component
public class PriceMapper {

    /**
     * Convert Price entity to PriceDTO
     */
    public PriceDTO toDTO(Price price) {
        if (price == null) {
            return null;
        }

        return PriceDTO.builder()
                .symbol(price.getSymbol())
                .price(price.getPrice())
                .volume(price.getVolume())
                .changePercent(price.getChangePercent())
                .high24h(price.getHigh24h())
                .low24h(price.getLow24h())
                .timestamp(price.getTimestamp())
                .source(price.getSource())
                .build();
    }

    /**
     * Convert PriceDTO to Price entity (for saving to database)
     */
    public Price toEntity(PriceDTO priceDTO) {
        if (priceDTO == null) {
            return null;
        }

        return Price.builder()
                .symbol(priceDTO.getSymbol())
                .price(priceDTO.getPrice())
                .volume(priceDTO.getVolume())
                .changePercent(priceDTO.getChangePercent())
                .high24h(priceDTO.getHigh24h())
                .low24h(priceDTO.getLow24h())
                .timestamp(priceDTO.getTimestamp() != null ? priceDTO.getTimestamp() : LocalDateTime.now())
                .source(priceDTO.getSource())
                .build();
    }

    /**
     * Convert PriceDTO to PriceStreamDTO (for WebSocket streaming)
     * Only includes essential fields for real-time updates
     */
    public PriceStreamDTO toStreamDTO(PriceDTO priceDTO) {
        if (priceDTO == null) {
            return null;
        }

        return PriceStreamDTO.builder()
                .symbol(priceDTO.getSymbol())
                .price(priceDTO.getPrice())
                .changePercent(priceDTO.getChangePercent())
                .timestamp(priceDTO.getTimestamp() != null
                        ? priceDTO.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()
                        : Instant.now())
                .build();
    }

    /**
     * Convert Price entity directly to PriceStreamDTO (for WebSocket)
     */
    public PriceStreamDTO toStreamDTO(Price price) {
        if (price == null) {
            return null;
        }

        return PriceStreamDTO.builder()
                .symbol(price.getSymbol())
                .price(price.getPrice())
                .changePercent(price.getChangePercent())
                .timestamp(price.getTimestamp() != null
                        ? price.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()
                        : Instant.now())
                .build();
    }

    /**
     * Convert PriceStreamDTO to PriceDTO
     */
    public PriceDTO toDTO(PriceStreamDTO streamDTO) {
        if (streamDTO == null) {
            return null;
        }

        return PriceDTO.builder()
                .symbol(streamDTO.getSymbol())
                .price(streamDTO.getPrice())
                .changePercent(streamDTO.getChangePercent())
                .timestamp(streamDTO.getTimestamp() != null
                        ? LocalDateTime.ofInstant(streamDTO.getTimestamp(), ZoneId.systemDefault())
                        : LocalDateTime.now())
                .build();
    }

    /**
     * Update existing Price entity with data from PriceDTO
     */
    public void updateEntity(Price existing, PriceDTO priceDTO) {
        if (existing == null || priceDTO == null) {
            return;
        }

        if (priceDTO.getPrice() != null) {
            existing.setPrice(priceDTO.getPrice());
        }
        if (priceDTO.getVolume() != null) {
            existing.setVolume(priceDTO.getVolume());
        }
        if (priceDTO.getChangePercent() != null) {
            existing.setChangePercent(priceDTO.getChangePercent());
        }
        if (priceDTO.getHigh24h() != null) {
            existing.setHigh24h(priceDTO.getHigh24h());
        }
        if (priceDTO.getLow24h() != null) {
            existing.setLow24h(priceDTO.getLow24h());
        }
        if (priceDTO.getSource() != null) {
            existing.setSource(priceDTO.getSource());
        }
        existing.setTimestamp(priceDTO.getTimestamp() != null ? priceDTO.getTimestamp() : LocalDateTime.now());
    }

    /**
     * Convert Map (from Kafka message) to PriceDTO
     * Useful when consuming price events from Kafka
     */
    public PriceDTO toDTO(java.util.Map<String, Object> priceMap) {
        if (priceMap == null || priceMap.isEmpty()) {
            return null;
        }

        return PriceDTO.builder()
                .symbol((String) priceMap.get("symbol"))
                .price(priceMap.get("price") != null
                        ? new java.math.BigDecimal(priceMap.get("price").toString())
                        : null)
                .volume(priceMap.get("volume") != null
                        ? ((Number) priceMap.get("volume")).longValue()
                        : null)
                .changePercent(priceMap.get("changePercent") != null
                        ? new java.math.BigDecimal(priceMap.get("changePercent").toString())
                        : null)
                .high24h(priceMap.get("high24h") != null
                        ? new java.math.BigDecimal(priceMap.get("high24h").toString())
                        : null)
                .low24h(priceMap.get("low24h") != null
                        ? new java.math.BigDecimal(priceMap.get("low24h").toString())
                        : null)
                .source((String) priceMap.get("source"))
                .timestamp(parseTimestamp(priceMap.get("timestamp")))
                .build();
    }

    /**
     * Helper method to parse timestamp from various formats
     */
    private LocalDateTime parseTimestamp(Object timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now();
        }

        try {
            if (timestamp instanceof Instant) {
                return LocalDateTime.ofInstant((Instant) timestamp, ZoneId.systemDefault());
            } else if (timestamp instanceof LocalDateTime) {
                return (LocalDateTime) timestamp;
            } else if (timestamp instanceof String) {
                return LocalDateTime.parse((String) timestamp);
            } else if (timestamp instanceof Number) {
                return LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(((Number) timestamp).longValue()),
                        ZoneId.systemDefault()
                );
            }
        } catch (Exception e) {
            // Log and return current time as fallback
        }

        return LocalDateTime.now();
    }
}
