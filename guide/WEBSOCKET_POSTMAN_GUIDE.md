# WebSocket Connection Guide for Postman

## Connecting to Price WebSocket (Market Data Service)

### Step 1: Get a JWT Token

1. Open Swagger UI: `http://localhost:8081/swagger-ui.html` (Trading Service)
2. Click "Authorize" button
3. Enter your credentials:
   - **Username**: `admin` (or your username)
   - **Password**: Your password
   - **Grant Type**: `password`
4. Click "Authorize"
5. Copy the Bearer token (starts with `eyJ...`)

### Step 2: Connect in Postman

1. **Open Postman** and create a new request
2. **Change request type** to **WebSocket** (dropdown next to URL)
3. **Enter WebSocket URL**:
   ```
   ws://localhost:8082/ws/prices-native?token=YOUR_TOKEN_HERE
   ```
   Replace `YOUR_TOKEN_HERE` with your actual JWT token

4. **Click "Connect"**

### Step 3: Send STOMP CONNECT Frame

After connecting, you need to send a STOMP CONNECT frame:

**Frame Type**: Text
**Message**:
```
CONNECT
accept-version:1.1,1.0
heart-beat:10000,10000

```

Click "Send"

### Step 4: Subscribe to Price Updates

#### Subscribe to a Specific Symbol (e.g., BTC):

**Frame Type**: Text
**Message**:
```
SUBSCRIBE
id:sub-1
destination:/topic/prices/BTC

```

#### Subscribe to All Prices (Admin only):

**Frame Type**: Text
**Message**:
```
SUBSCRIBE
id:sub-all
destination:/topic/prices/all

```

### Step 5: Request Subscription from Server

To activate price streaming, send a subscription request:

**Frame Type**: Text
**Message**:
```
SEND
destination:/app/prices/subscribe
content-type:application/json

{"symbol":"BTC"}
```

For all prices (Admin):
**Frame Type**: Text
**Message**:
```
SEND
destination:/app/prices/subscribe/all
content-type:application/json

```

### Step 6: Receive Price Updates

You should now receive price updates in the format:
```json
{
  "symbol": "BTC",
  "price": 52000.50,
  "volume": 12345,
  "changePercent": 1.25,
  "high24h": 52500.00,
  "low24h": 51000.00,
  "timestamp": "2026-01-28T17:30:00"
}
```

## Troubleshooting

### Connection Fails (1002 Error)
- **Check**: Token is valid and not expired
- **Check**: Server is running on port 8082
- **Check**: Token is properly URL-encoded in the query parameter

### No Messages Received
- **Check**: You sent the STOMP CONNECT frame
- **Check**: You subscribed to the correct topic
- **Check**: You sent the subscription request to `/app/prices/subscribe`
- **Check**: Price simulator is running (check if `@EnableScheduling` is enabled)

### Authentication Errors
- **Error**: "Authentication failed: Token has expired"
  - **Solution**: Get a new token from Keycloak/Swagger UI

- **Error**: "Authentication required"
  - **Solution**: Make sure token is in the URL query parameter: `?token=YOUR_TOKEN`

## Example Complete Flow

1. **Connect**: `ws://localhost:8082/ws/prices?token=eyJhbGc...`
2. **CONNECT Frame**:
   ```
   CONNECT
   accept-version:1.1,1.0
   heart-beat:10000,10000
   
   ```
3. **SUBSCRIBE Frame**:
   ```
   SUBSCRIBE
   id:sub-1
   destination:/topic/prices/BTC
   
   ```
4. **SEND Subscription Request**:
   ```
   SEND
   destination:/app/prices/subscribe
   content-type:application/json
   
   {"symbol":"BTC"}
   ```
5. **Receive Updates**: Price updates will appear in the messages panel

## Notes

- **SockJS vs Native WebSocket**:
  - Browser HTML uses **SockJS** endpoint: `/ws/prices` (SockJS adds paths like `/ws/prices/{serverId}/{sessionId}/websocket`)
  - Postman uses **native WebSocket** endpoint: `/ws/prices-native`
- **STOMP Protocol**: All messages after connection must follow STOMP protocol format
- **Heartbeat**: The CONNECT frame includes heartbeat to keep connection alive
- **Token Expiration**: Tokens typically expire after 1 hour. Get a new one if connection fails.
