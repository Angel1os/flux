# Trading Service - Implementation Guide
## First Service to Build for Trading Platform

---

## 🎯 Why Start Here?

1. **Core Business Logic** - Central to the platform
2. **Maximum Spring Boot 4 Features** - Can use all new features
3. **Foundation for Others** - Other services depend on it
4. **Learning Value** - Best practices in one place

---

## 📋 Service Overview

**Trading Service** handles:
- Order placement (buy/sell)
- Order validation
- Order execution
- Real-time order status updates
- Integration with market data
- Portfolio updates via events

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│         Trading Service                 │
│                                         │
│  ┌──────────────┐  ┌──────────────┐    │
│  │   REST API   │  │  WebSocket  │    │
│  │  (Orders)    │  │  (Live)     │    │
│  └──────┬───────┘  └──────┬──────┘    │
│         │                  │           │
│  ┌──────▼──────────────────▼──────┐   │
│  │     Order Service Layer         │   │
│  │  - Validation                  │   │
│  │  - Processing                  │   │
│  │  - State Management            │   │
│  └──────┬──────────────────┬──────┘   │
│         │                  │           │
│  ┌──────▼──────┐  ┌────────▼──────┐   │
│  │   Write DB  │  │   Read DB     │   │
│  │ (PostgreSQL)│  │   (Redis)     │   │
│  └──────┬──────┘  └────────┬──────┘   │
│         │                  │           │
│  ┌──────▼──────────────────▼──────┐   │
│  │      Event Publisher           │   │
│  │      (Kafka)                   │   │
│  └────────────────────────────────┘   │
│                                         │
│  ┌────────────────────────────────┐   │
│  │  Declarative HTTP Client        │   │
│  │  (Market Data Service)         │   │
│  └────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

---

## 🚀 Step-by-Step Implementation

### Phase 1: Project Setup (Day 1)

#### 1.1 Create Project Structure

```bash
mkdir trading-platform
cd trading-platform
mkdir trading-service
cd trading-service
```

#### 1.2 Initialize Maven Project

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.1</version>
        <relativePath/>
    </parent>
    
    <groupId>com.trading</groupId>
    <artifactId>trading-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>Trading Service</name>
    <description>Core trading service for order management</description>
    
    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2025.1.0</spring-cloud.version>
        <resilience4j.version>2.1.0</resilience4j.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        
        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        
        <!-- Resilience4j for Circuit Breakers -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>${resilience4j.version}</version>
        </dependency>
        
        <!-- Micrometer for Observability -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### Phase 2: Core Domain Models (Day 1-2)

#### 2.1 Create Domain Entities

**Order Entity (Write Model):**

```java
package com.trading.service.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false, length = 10)
    private String symbol;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type; // BUY, SELL
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // PENDING, EXECUTED, CANCELLED, REJECTED
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side; // MARKET, LIMIT, STOP
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal limitPrice;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal executedPrice;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal executedQuantity;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Version
    private Long version; // For optimistic locking
}

enum OrderType {
    BUY, SELL
}

enum OrderStatus {
    PENDING, EXECUTED, CANCELLED, REJECTED, PARTIALLY_EXECUTED
}

enum OrderSide {
    MARKET, LIMIT, STOP
}
```

**Order Read Model (Redis Cache):**

```java
package com.trading.service.domain.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReadModel implements Serializable {
    private UUID id;
    private UUID userId;
    private String symbol;
    private String type;
    private String status;
    private String side;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private BigDecimal executedPrice;
    private BigDecimal executedQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### Phase 3: Declarative HTTP Client (Day 2)

#### 3.1 Market Data Client Interface

**This is the NEW Spring Boot 4 feature!**

```java
package com.trading.service.client;

import com.trading.service.dto.PriceDTO;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange("http://market-data-service:8082/api/v1")
public interface MarketDataClient {
    
    @GetExchange("/prices/{symbol}")
    Mono<PriceDTO> getCurrentPrice(@PathVariable String symbol);
    
    @GetExchange("/prices/{symbol}/history")
    Mono<PriceHistoryDTO> getPriceHistory(
        @PathVariable String symbol,
        @RequestParam String from,
        @RequestParam String to
    );
}
```

#### 3.2 Configure HTTP Service Client

```java
package com.trading.service.config;

import com.trading.service.client.MarketDataClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {
    
    @Bean
    public MarketDataClient marketDataClient() {
        WebClient webClient = WebClient.builder()
            .baseUrl("http://market-data-service:8082/api/v1")
            .build();
        
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(webClient))
            .build();
        
        return factory.createClient(MarketDataClient.class);
    }
}
```

---

### Phase 4: Circuit Breaker Implementation (Day 2-3)

#### 4.1 Service with Circuit Breaker

```java
package com.trading.service.service;

import com.trading.service.client.MarketDataClient;
import com.trading.service.dto.PriceDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {
    
    private final MarketDataClient marketDataClient;
    
    @CircuitBreaker(name = "marketData", fallbackMethod = "getCachedPrice")
    @Retry(name = "marketData")
    public Mono<PriceDTO> getCurrentPrice(String symbol) {
        log.info("Fetching current price for symbol: {}", symbol);
        return marketDataClient.getCurrentPrice(symbol)
            .doOnError(error -> log.error("Error fetching price for {}: {}", symbol, error.getMessage()));
    }
    
    // Fallback method
    public Mono<PriceDTO> getCachedPrice(String symbol, Exception e) {
        log.warn("Circuit breaker opened for market data. Using cached price for: {}", symbol);
        // Return cached price from Redis or default value
        return Mono.just(PriceDTO.builder()
            .symbol(symbol)
            .price(BigDecimal.ZERO)
            .timestamp(LocalDateTime.now())
            .build());
    }
}
```

#### 4.2 Circuit Breaker Configuration

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      marketData:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
  retry:
    instances:
      marketData:
        maxAttempts: 3
        waitDuration: 1000
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

---

### Phase 5: Order Service with CQRS (Day 3-4)

#### 5.1 Order Service (Write Side)

```java
package com.trading.service.service;

import com.trading.service.domain.model.Order;
import com.trading.service.domain.model.OrderStatus;
import com.trading.service.dto.CreateOrderDTO;
import com.trading.service.events.OrderCreatedEvent;
import com.trading.service.events.OrderExecutedEvent;
import com.trading.service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final MarketDataService marketDataService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public Order createOrder(CreateOrderDTO dto) {
        // Validate order
        validateOrder(dto);
        
        // Create order entity
        Order order = Order.builder()
            .userId(dto.getUserId())
            .symbol(dto.getSymbol())
            .type(dto.getType())
            .side(dto.getSide())
            .quantity(dto.getQuantity())
            .limitPrice(dto.getLimitPrice())
            .status(OrderStatus.PENDING)
            .build();
        
        // Save to write database
        order = orderRepository.save(order);
        
        // Publish event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getId())
            .userId(order.getUserId())
            .symbol(order.getSymbol())
            .type(order.getType().name())
            .quantity(order.getQuantity())
            .timestamp(java.time.Instant.now())
            .build();
        
        kafkaTemplate.send("order-events", order.getId().toString(), event);
        
        log.info("Order created: {}", order.getId());
        return order;
    }
    
    @Transactional
    public void executeOrder(UUID orderId, BigDecimal executionPrice, BigDecimal executionQuantity) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.setExecutedPrice(executionPrice);
        order.setExecutedQuantity(executionQuantity);
        order.setStatus(OrderStatus.EXECUTED);
        
        orderRepository.save(order);
        
        // Publish execution event
        OrderExecutedEvent event = OrderExecutedEvent.builder()
            .orderId(orderId)
            .executionPrice(executionPrice)
            .executionQuantity(executionQuantity)
            .timestamp(java.time.Instant.now())
            .build();
        
        kafkaTemplate.send("order-events", orderId.toString(), event);
        
        log.info("Order executed: {}", orderId);
    }
    
    private void validateOrder(CreateOrderDTO dto) {
        // Validation logic
        if (dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        // Add more validation...
    }
}
```

#### 5.2 Order Query Service (Read Side)

```java
package com.trading.service.service;

import com.trading.service.domain.readmodel.OrderReadModel;
import com.trading.service.repository.OrderReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    
    private final OrderReadRepository orderReadRepository;
    
    @Cacheable(value = "orders", key = "#orderId")
    public OrderReadModel getOrder(UUID orderId) {
        return orderReadRepository.findById(orderId.toString())
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
    
    @Cacheable(value = "userOrders", key = "#userId")
    public List<OrderReadModel> getUserOrders(UUID userId) {
        return orderReadRepository.findByUserId(userId.toString());
    }
    
    public List<OrderReadModel> getOrdersBySymbol(String symbol) {
        return orderReadRepository.findBySymbol(symbol);
    }
}
```

---

### Phase 6: Virtual Threads Configuration (Day 4)

#### 6.1 Enable Virtual Threads

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true
```

#### 6.2 Configure Virtual Thread Executor

```java
package com.trading.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class VirtualThreadConfig {
    
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

#### 6.3 Use Virtual Threads for Async Operations

```java
@Service
@RequiredArgsConstructor
public class OrderProcessingService {
    
    @Async("virtualThreadExecutor")
    public CompletableFuture<Void> processOrderAsync(Order order) {
        // Process order on virtual thread
        // This won't block OS threads
        return CompletableFuture.runAsync(() -> {
            // Order processing logic
        });
    }
}
```

---

### Phase 7: WebSocket for Real-Time Updates (Day 4-5)

#### 7.1 WebSocket Configuration

```java
package com.trading.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/orders")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
```

#### 7.2 WebSocket Controller

```java
package com.trading.service.controller;

import com.trading.service.dto.OrderStatusUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void sendOrderUpdate(UUID orderId, OrderStatusUpdateDTO update) {
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, update);
        log.debug("Sent order update for order: {}", orderId);
    }
    
    public void sendUserOrderUpdate(UUID userId, OrderStatusUpdateDTO update) {
        messagingTemplate.convertAndSend("/queue/orders/user/" + userId, update);
    }
}
```

---

### Phase 8: Observability with Micrometer (Day 5)

#### 8.1 Custom Metrics

```java
package com.trading.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradingMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter ordersCreated;
    private final Counter ordersExecuted;
    private final Timer orderProcessingTime;
    
    public TradingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.ordersCreated = Counter.builder("trading.orders.created")
            .description("Total number of orders created")
            .register(meterRegistry);
        this.ordersExecuted = Counter.builder("trading.orders.executed")
            .description("Total number of orders executed")
            .register(meterRegistry);
        this.orderProcessingTime = Timer.builder("trading.orders.processing.time")
            .description("Order processing time")
            .register(meterRegistry);
    }
    
    public void incrementOrdersCreated() {
        ordersCreated.increment();
    }
    
    public void incrementOrdersExecuted() {
        ordersExecuted.increment();
    }
    
    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(meterRegistry);
    }
}
```

#### 8.2 Use Metrics in Service

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final TradingMetrics metrics;
    
    public Order createOrder(CreateOrderDTO dto) {
        Timer.Sample sample = metrics.startOrderProcessingTimer();
        try {
            // Create order logic
            Order order = // ... create order
            metrics.incrementOrdersCreated();
            return order;
        } finally {
            sample.stop(metrics.getOrderProcessingTime());
        }
    }
}
```

---

### Phase 9: Application Configuration (Day 5)

#### 9.1 application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: trading-service
  
  # Virtual Threads (Spring Boot 4)
  threads:
    virtual:
      enabled: true
  
  # Database
  datasource:
    url: jdbc:postgresql://localhost:5432/trading_db
    username: trading
    password: trading
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  # Redis
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
  
  # Kafka
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: trading-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

# Resilience4j
resilience4j:
  circuitbreaker:
    instances:
      marketData:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
  retry:
    instances:
      marketData:
        maxAttempts: 3
        waitDuration: 1000

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0

# Logging
logging:
  level:
    com.trading: DEBUG
    org.springframework.kafka: INFO
```

---

### Phase 10: REST Controller (Day 6)

```java
package com.trading.service.controller;

import com.trading.service.dto.CreateOrderDTO;
import com.trading.service.domain.model.Order;
import com.trading.service.service.OrderService;
import com.trading.service.service.OrderQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        Order order = orderService.createOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderReadModel> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderQueryService.getOrder(orderId));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderReadModel>> getUserOrders(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderQueryService.getUserOrders(userId));
    }
    
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<OrderReadModel>> getOrdersBySymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(orderQueryService.getOrdersBySymbol(symbol));
    }
}
```

---

## ✅ Checklist

- [ ] Day 1: Project setup, POM configuration
- [ ] Day 2: Domain models, Declarative HTTP client
- [ ] Day 3: Circuit breakers, Order service (write)
- [ ] Day 4: Order query service (read), Virtual threads
- [ ] Day 5: WebSocket, Observability
- [ ] Day 6: REST controllers, Testing
- [ ] Day 7: Integration testing, Documentation

---

## 🎯 Key Spring Boot 4 Features Used

1. ✅ **Declarative HTTP Clients** - `@HttpExchange`
2. ✅ **Virtual Threads** - `spring.threads.virtual.enabled: true`
3. ✅ **Enhanced Observability** - Micrometer 2.0
4. ✅ **Java 21** - Records, pattern matching
5. ✅ **Modular Autoconfiguration** - Smaller JARs

---

## 📚 Next Steps After This Service

1. **Market Data Service** - Reactive, Kafka Streams
2. **Portfolio Service** - CQRS, Event Sourcing
3. **Analytics Service** - Real-time analytics
4. **API Gateway** - Rate limiting, routing

---

**Start with this service and you'll have a solid foundation!** 🚀
