# Dead Letter Queue (DLQ) Implementation Guide

## Overview

The Market Data Service now includes a **Dead Letter Queue (DLQ)** system for handling failed Kafka messages. Messages that fail processing after maximum retry attempts are routed to a DLQ topic for analysis and manual reprocessing.

---

## How It Works

### 1. **Retry Logic**
- Messages are retried up to **3 times** (configurable via `MAX_RETRY_ATTEMPTS`)
- Retry attempts are tracked per message using `topic-partition-offset` as the key
- Each retry is logged with attempt number

### 2. **Failure Handling**

**Immediate DLQ (No Retry)**:
- **Invalid data format**: Unknown message types, null values, malformed data
- **Data validation errors**: `IllegalArgumentException` (e.g., invalid symbol, null required fields)

**Retry Then DLQ**:
- **Database errors**: Connection failures, transaction errors, constraint violations
- **Processing errors**: Any other exceptions during message processing
- After 3 failed attempts → message sent to DLQ

### 3. **DLQ Message Format**

Messages sent to DLQ include:
```json
{
  "originalTopic": "price-events",
  "originalPartition": 0,
  "originalOffset": 12345,
  "originalKey": "BTC",
  "originalValue": { /* original price event */ },
  "originalTimestamp": 1234567890,
  "errorMessage": "Max retry attempts (3) exceeded",
  "errorType": "java.sql.SQLException",
  "errorStackTrace": "...",
  "failedAt": "2026-01-28T20:00:00",
  "consumerGroup": "price-events-consumer"
}
```

---

## Components

### 1. **DlqService** (`com.angellos.market.data.service.service.DlqService`)
- Handles routing failed messages to DLQ topic
- Enriches messages with error metadata
- Logs DLQ operations

### 2. **PriceEventListener** (Updated)
- Tracks retry attempts per message
- Implements retry logic (max 3 attempts)
- Routes to DLQ after max retries
- Handles different error types appropriately

### 3. **KafkaTopicsConfig** (Updated)
- Creates `price-events-dlq` topic on startup
- Topic configuration: 3 partitions, replication factor 1 (dev)

---

## Configuration

### Application Properties (`application.yml`)
```yaml
spring:
  kafka:
    consumer:
      max-retry-attempts: 3  # Maximum retry attempts before DLQ
      enable-auto-commit: false  # Manual acknowledgment for retry control
```

### Code Constants
```java
private static final int MAX_RETRY_ATTEMPTS = 3;  // In PriceEventListener
```

---

## DLQ Topic

- **Topic Name**: `price-events-dlq`
- **Partitions**: 3
- **Replication Factor**: 1 (dev) / 3 (production)
- **Retention**: Default (or configure as needed)

---

## Monitoring & Operations

### 1. **Check DLQ Messages**
```bash
# Using kafka-console-consumer
kafka-console-consumer --bootstrap-server localhost:19092 \
  --topic price-events-dlq \
  --from-beginning
```

### 2. **DLQ Message Analysis**
- Check `errorMessage` and `errorType` to understand failures
- Review `originalValue` to see the original message
- Check `failedAt` timestamp to identify failure patterns

### 3. **Reprocessing DLQ Messages**
1. **Manual Reprocessing**: Extract `originalValue` from DLQ messages and republish to `price-events`
2. **Automated Reprocessing**: Create a separate consumer that reads from DLQ and republishes (after fixing root cause)

### 4. **Alerting** (Recommended)
- Monitor DLQ topic size
- Alert when DLQ message count exceeds threshold
- Alert on specific error types (e.g., database connection failures)

---

## Error Scenarios

### Scenario 1: Database Connection Failure
1. Message received → Attempt 1: DB connection fails
2. Message not acknowledged → Kafka redelivers
3. Attempt 2: DB connection fails
4. Message not acknowledged → Kafka redelivers
5. Attempt 3: DB connection fails
6. **Max retries exceeded** → Message sent to DLQ
7. Message acknowledged (to prevent infinite retries)

### Scenario 2: Invalid Data Format
1. Message received → Invalid JSON structure
2. **Immediate DLQ** (no retry - data won't improve)
3. Message acknowledged

### Scenario 3: Successful Processing After Retry
1. Message received → Attempt 1: Temporary DB error
2. Message not acknowledged → Kafka redelivers
3. Attempt 2: **Success** → Price saved, WebSocket update sent
4. Retry counter cleared, message acknowledged

---

## Best Practices

1. **Monitor DLQ Regularly**: Check DLQ topic size and error patterns
2. **Fix Root Causes**: Don't just reprocess - fix the underlying issue
3. **Set Retention**: Configure DLQ topic retention based on your needs (e.g., 7 days)
4. **Alerting**: Set up alerts for DLQ message count and specific error types
5. **Documentation**: Document common DLQ scenarios and resolution steps

---

## Testing DLQ

### Test 1: Simulate Database Failure
1. Stop PostgreSQL
2. Send price events to Kafka
3. Verify messages are retried 3 times
4. Verify messages appear in `price-events-dlq` topic

### Test 2: Simulate Invalid Data
1. Send malformed JSON to `price-events` topic
2. Verify message goes directly to DLQ (no retries)
3. Check DLQ message contains error details

### Test 3: Verify Successful Retry
1. Temporarily stop PostgreSQL
2. Send price event
3. Restart PostgreSQL before 3 retries complete
4. Verify message is processed successfully on retry

---

## Future Enhancements

1. **DLQ Dashboard**: Web UI to view and reprocess DLQ messages
2. **Automatic Reprocessing**: Scheduled job to retry DLQ messages after fixes
3. **Error Classification**: Categorize errors (transient vs permanent) for better handling
4. **Metrics**: Expose DLQ metrics (message count, error types) via Actuator/Prometheus

---

## Summary

✅ **DLQ Topic**: `price-events-dlq` created automatically  
✅ **Retry Logic**: 3 attempts before DLQ  
✅ **Error Handling**: Different strategies for different error types  
✅ **Metadata**: Rich error information in DLQ messages  
✅ **Monitoring**: Logs and DLQ topic for analysis  

The DLQ system ensures that failed messages are not lost and can be analyzed and reprocessed after fixing root causes.
