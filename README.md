# Flux Trading Platform

A microservices-based trading platform built with Spring Boot 4, featuring real-time market data streaming, order management, and event-driven architecture.

## 🏗️ Architecture

### Microservices

1. **Trading Service** (`trading-service`)
   - Order management (create, update, cancel, execute)
   - CQRS pattern with event sourcing
   - WebSocket for real-time order updates
   - REST API with Swagger documentation
   - Port: `8081`

2. **Market Data Service** (`market-data-service`)
   - Real-time price streaming
   - Historical price data
   - GraphQL API (queries + subscriptions)
   - WebSocket/STOMP for price updates
   - Kafka Streams for price aggregation
   - Reactive programming (WebFlux + R2DBC)
   - Port: `8082`

3. **Shared Module** (`shared`)
   - Common DTOs, enums, utilities
   - Shared across all services

### Technology Stack

- **Framework**: Spring Boot 4.0.1 (Java 21)
- **Database**: PostgreSQL (JPA for Trading Service, R2DBC for Market Data Service)
- **Cache**: Redis
- **Message Broker**: Apache Kafka
- **Stream Processing**: Kafka Streams
- **Authentication**: Keycloak (OAuth2/JWT)
- **API Documentation**: Swagger/OpenAPI (SpringDoc)
- **GraphQL**: Spring GraphQL
- **WebSocket**: STOMP over WebSocket
- **Monitoring**: Spring Actuator + Prometheus
- **Resilience**: Resilience4j (Circuit Breaker)

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **PostgreSQL 14+**
- **Redis 7+**
- **Apache Kafka 3.5+**
- **Keycloak 23+** (for authentication)

### 1. Clone Repository

```bash
git clone https://github.com/Angel1os/flux.git
cd flux
```

### 2. Set Up Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
# Edit .env with your values
```

See [Environment Variables](#environment-variables) section for details.

### 3. Start Infrastructure

#### PostgreSQL
```bash
# Create databases
createdb trading_db
createdb market_data_db
```

#### Redis
```bash
redis-server
```

#### Kafka
```bash
# Start Zookeeper (if needed)
# Start Kafka broker on port 19092
```

#### Keycloak
```bash
# Start Keycloak on port 8080
# Follow guide/KEYCLOAK_SETUP_GUIDE.md for setup
```

### 4. Build Project

```bash
mvn clean install
```

### 5. Run Services

**Trading Service:**
```bash
cd trading-service
mvn spring-boot:run
# Or: java -jar target/trading-service.jar
```

**Market Data Service:**
```bash
cd market-data-service
mvn spring-boot:run
# Or: java -jar target/market-data-service.jar
```

### 6. Access Services

- **Trading Service Swagger**: http://localhost:8081/swagger-ui.html
- **Market Data Service Swagger**: http://localhost:8082/swagger-ui.html
- **Market Data GraphQL**: http://localhost:8082/graphql
- **GraphiQL UI**: http://localhost:8082/graphiql
- **Actuator Health**: http://localhost:8081/actuator/health

---

## 📋 Environment Variables

Create a `.env` file in the project root (not committed to git):

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password

# Trading Service Database
TRADING_DB_NAME=trading_db

# Market Data Service Database
MARKET_DATA_DB_NAME=market_data_db

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_USERNAME=default
REDIS_PASSWORD=your_redis_password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:19092

# Keycloak
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=trading-platform

# Service Ports (optional, defaults in application.yml)
TRADING_SERVICE_PORT=8081
MARKET_DATA_SERVICE_PORT=8082
```

Services will read these from environment variables or use defaults from `application.yml`.

---

## 🔐 Authentication

### Get JWT Token

**Via Swagger UI:**
1. Open Swagger UI (http://localhost:8081/swagger-ui.html)
2. Click "Authorize" button
3. Enter credentials:
   - Username: `admin` (or `trader1`)
   - Password: `password` (or your password)
   - Grant Type: `password`
4. Click "Authorize"
5. Copy the Bearer token

**Via curl:**
```bash
curl -X POST http://localhost:8080/realms/trading-platform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=trading-service" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=password"
```

### Use Token in Requests

```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8081/api/trading/orders
```

---

## 📡 API Endpoints

### Trading Service

**Base URL**: `http://localhost:8081/api/trading/orders`

- `GET /` - Get all orders (with pagination/filtering)
- `GET /{id}` - Get order by ID
- `GET /symbol/{symbol}` - Get orders by symbol
- `GET /status/{status}` - Get orders by status
- `GET /history` - Get order history (paginated)
- `POST /` - Create order
- `PUT /{id}` - Update order
- `DELETE /{id}` - Cancel order
- `POST /{id}/execute` - Execute order (ADMIN only)

**WebSocket**: `ws://localhost:8081/ws/orders?token=JWT`

### Market Data Service

**Base URL**: `http://localhost:8082/api/v1/market-data/prices`

- `GET /{symbol}/current` - Get current price
- `GET /{symbol}/history?startDate=...&endDate=...` - Get price history
- `GET /symbols` - Get all symbols
- `GET /latest` - Get latest prices for all symbols

**GraphQL**: `POST http://localhost:8082/graphql`

**WebSocket**: `ws://localhost:8082/ws/prices-native?token=JWT`

See [Postman Collection](guide/flux-postman-collection.json) for complete API examples.

---

## 🧪 Testing

### Postman Collection

Import `guide/flux-postman-collection.json` into Postman:
1. Set collection variable `access_token` to your JWT token
2. All requests will use `Authorization: Bearer {{access_token}}`

### WebSocket Testing

**Trading Service (Orders):**
- HTML Test Client: `guide/websocket-test.html`
- Postman: See `guide/WEBSOCKET_POSTMAN_GUIDE.md`

**Market Data Service (Prices):**
- HTML Test Client: `guide/price-websocket-test.html`
- GraphQL Test Client: `guide/graphql-test.html`

### GraphQL Testing

See `guide/GRAPHQL_TESTING_GUIDE.md` for:
- Query examples
- Subscription testing
- GraphiQL usage

---

## 🏛️ Architecture Patterns

### CQRS (Command Query Responsibility Segregation)

- **Write Model**: PostgreSQL (Trading Service)
- **Read Model**: Redis cache + PostgreSQL queries
- **Event Sourcing**: Kafka events for order state changes

### Event-Driven Architecture

- **Events**: OrderCreatedEvent, OrderUpdatedEvent, OrderExecutedEvent, OrderCancelledEvent
- **Topics**: `order-events`, `price-events`, `price-aggregated`, `price-changes`
- **DLQ**: `order-events-dlq`, `price-events-dlq` (for failed messages)

### Reactive Programming

- **Market Data Service**: Full reactive stack (WebFlux + R2DBC)
- **Trading Service**: Traditional servlet-based (JPA)

### Dead Letter Queue (DLQ)

Both services implement DLQ for failed Kafka messages:
- Max retry attempts: 3
- Failed messages routed to DLQ topic
- See `guide/DLQ_IMPLEMENTATION.md` for details

---

## 📊 Kafka Topics

### Trading Service
- `order-events` - Order state change events
- `order-events-dlq` - Failed order events

### Market Data Service
- `price-events` - Raw price events from simulator
- `price-aggregated` - Aggregated prices (Kafka Streams)
- `price-changes` - Price change notifications
- `price-events-dlq` - Failed price events

---

## 🔍 Monitoring

### Actuator Endpoints

**Health Check:**
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

**Metrics (Prometheus):**
```bash
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
```

### Logging

Logs are configured in `application.yml`:
- Trading Service: `DEBUG` for `com.angellos.trading.service`
- Market Data Service: `INFO` for `com.angellos.market.data.service`

---

## 🛠️ Development

### Project Structure

```
flux/
├── shared/                    # Shared module (DTOs, utilities)
├── trading-service/          # Order management service
├── market-data-service/       # Market data service
├── guide/                     # Documentation and test files
│   ├── KEYCLOAK_SETUP_GUIDE.md
│   ├── GRAPHQL_TESTING_GUIDE.md
│   ├── DLQ_IMPLEMENTATION.md
│   ├── flux-postman-collection.json
│   └── *.html                # Test clients
└── pom.xml                   # Parent POM
```

### Building

```bash
# Build all modules
mvn clean install

# Build specific module
cd trading-service && mvn clean install
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
cd trading-service && mvn test
```

---

## 📚 Documentation

- [Keycloak Setup Guide](guide/KEYCLOAK_SETUP_GUIDE.md)
- [GraphQL Testing Guide](guide/GRAPHQL_TESTING_GUIDE.md)
- [DLQ Implementation](guide/DLQ_IMPLEMENTATION.md)
- [WebSocket Postman Guide](guide/WEBSOCKET_POSTMAN_GUIDE.md)

---

## 🔒 Security

- **Authentication**: OAuth2/JWT via Keycloak
- **Authorization**: Role-based (TRADER, ADMIN, VIEWER)
- **Secrets**: Stored in environment variables (`.env` file)
- **CORS**: Configured per service (update for production)

---

## 🐛 Troubleshooting

### Service Won't Start

1. **Check ports**: Ensure ports 8081, 8082 are available
2. **Check databases**: Verify PostgreSQL databases exist
3. **Check Kafka**: Ensure Kafka is running on port 19092
4. **Check Keycloak**: Verify Keycloak is running on port 8080

### Authentication Issues

1. **Token expired**: Get a new token from Keycloak
2. **401 Unauthorized**: Check token is valid and includes required roles
3. **403 Forbidden**: User doesn't have required role (TRADER, ADMIN, VIEWER)

### WebSocket Connection Fails

1. **Check token**: Ensure JWT token is valid and not expired
2. **Check URL**: Use correct WebSocket endpoint (`/ws/orders` or `/ws/prices-native`)
3. **Check logs**: Review service logs for authentication errors

### Kafka Issues

1. **Messages not consumed**: Check consumer group ID and topic names
2. **DLQ messages**: Check `*-dlq` topics for failed messages
3. **No price events**: Ensure `PriceSimulatorService` is running (Market Data Service)

---

## 🚧 Future Enhancements

- [ ] API Gateway service
- [ ] Portfolio Service
- [ ] Analytics Service
- [ ] Notification Service
- [ ] Order History Service (read-only)
- [ ] Price History Service (read-only)
- [ ] Docker Compose for local development
- [ ] Kubernetes deployment manifests
- [ ] CI/CD pipeline
- [ ] Integration tests
- [ ] Performance testing

---

## 📝 License

This project is licensed under the Flux License.

---

## 👤 Author

**Prince Amofah**
- Email: angellosprince@gmail.com
- GitHub: [@Angel1os](https://github.com/Angel1os)

---

## 🙏 Acknowledgments

- Spring Boot team for excellent framework
- Apache Kafka for event streaming
- Keycloak for authentication
- All open-source contributors

---

## 📞 Support

For issues and questions:
1. Check [Troubleshooting](#-troubleshooting) section
2. Review documentation in `guide/` directory
3. Open an issue on GitHub

---

**Happy Trading! 📈**
