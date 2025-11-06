# Messaging Tools

## Overview

The Messaging Tools provides a comprehensive interactive messaging interface for the MAPS Messaging Server. It includes endpoints for publishing messages, managing subscriptions, handling transactions, and consuming messages via real-time Server-Sent Events (SSE).

## Features

### 1. Publish Console (`/api/v1/messaging/publish`)

#### Capabilities
- **Topic Input**: Specify the destination topic for message publishing
- **Payload Editor**: Support for multiple payload formats:
  - JSON documents
  - Plain text
  - Binary data (via Base64 encoding)
  - File upload support
- **Message Properties**: Configure message metadata
  - Quality of Service (QoS): AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE
  - Priority levels: 0-10
  - Expiry time
  - Content type
  - Correlation data
- **Headers and Properties**: Add custom headers to messages
- **Delivery Options**: Configure delivery behavior
  - Timeout settings
  - Retry count
  - Backoff strategies
- **Retention**: Mark messages for retention on the broker
- **Session Management**: Publish within named transactional sessions

#### Request Format

```json
{
  "destinationName": "sensor/temperature",
  "message": {
    "payload": "base64-encoded-data",
    "contentType": "application/json",
    "qualityOfService": 1,
    "priority": 4,
    "expiry": 60000,
    "headers": {
      "correlationId": "123",
      "source": "sensor-1"
    }
  },
  "headers": {
    "correlationId": "123"
  },
  "deliveryOptions": {
    "timeout": "5000",
    "retryCount": "3"
  },
  "retain": false,
  "sessionName": "session-1"
}
```

### 2. Subscription and Session Management

#### Endpoints

- **`/api/v1/messaging/subscribe`** (POST) - Create a subscription
- **`/api/v1/messaging/unsubscribe`** (POST) - Remove a subscription
- **`/api/v1/messaging/subscriptionDepth`** (POST) - Get queue depth
- **`/api/v1/messaging/consume`** (POST) - Retrieve messages

#### Capabilities

- **Named Sessions**: Create named subscription sessions for easy management
- **Outstanding Message Tracking**: View count of pending messages per destination
- **Message Filtering**: Apply JMS selector syntax filters
  - Example: `temperature > 25 AND humidity < 80`
- **Wildcard Subscriptions**: Support for MQTT wildcard patterns
  - Single level: `sensor/+/temperature`
  - Multi-level: `building/floor-1/#`
- **Transactional Subscriptions**: Subscribe with transaction support for EXACTLY_ONCE delivery
- **Max Depth Control**: Limit queued messages per subscription
- **Retention Messages**: Receive retained messages on subscription

#### Request Format - Subscribe

```json
{
  "destinationName": "sensors/temperature",
  "namedSubscription": "tempMonitor",
  "filter": "temperature > 20",
  "maxDepth": 100,
  "transactional": true,
  "retainMessage": false
}
```

#### Request Format - Consume

```json
{
  "destination": "sensors/temperature",
  "depth": 50
}
```

### 3. Real-time Consumption via SSE

#### Endpoints

- **`/api/v1/messaging/sse`** (GET) - Request SSE token
- **`/api/v1/messaging/sse/stream/{token}`** (GET) - Connect to message stream

#### Capabilities

- **Real-time Streaming**: Messages delivered via Server-Sent Events
- **Filters and Queries**: Apply filters to the stream
- **Message Export**: Export streamed messages for analysis
- **Async Delivery**: Non-blocking message consumption
- **Connection Management**: Automatic cleanup on client disconnect

#### Usage

```
1. GET /api/v1/messaging/sse?destination=sensor/temperature
   Response: token-xyz123
   
2. GET /api/v1/messaging/sse/stream/token-xyz123 (with EventSource)
   Receives: Server-sent events with AsyncMessageDTO
```

### 4. Transaction Management

#### Endpoints

- **`/api/v1/messaging/commit`** (POST) - Commit messages
- **`/api/v1/messaging/abort`** (POST) - Abort/rollback messages

#### Capabilities

- **Message Acknowledgment**: Acknowledge received messages
- **Rollback Support**: Rollback messages for redelivery
- **Transaction Status Tracking**: Monitor transaction state
  - PENDING, COMMITTED, FAILED, ROLLED_BACK
- **Transaction Metadata**: Track transaction IDs and timing
- **Delivery Status**: Monitor individual message delivery status

#### Request Format - Commit

```json
{
  "destinationName": "txn/queue",
  "eventIds": [1, 2, 3, 4, 5]
}
```

### 5. Telemetry and Metrics

#### Tracking

- **Publish Success/Failure**: Track publish operation outcomes
- **Message Timing**: Measure delivery latency
  - Min, max, average, percentiles (p50, p95, p99)
- **Throughput**: Messages per second
- **Error Rates**: Failure and error tracking
- **Success Rate**: Percentage of successful operations

#### Response Format

```json
{
  "messageId": "msg-12345",
  "success": true,
  "message": "Message published successfully",
  "publishTime": "2024-01-15T10:30:00",
  "deliveryLatency": 45,
  "transactionId": "txn-001",
  "transactionStatus": "COMMITTED",
  "errorDetails": null
}
```

### 6. Auth Context Integration

- **Credential Reuse**: Automatically uses HTTP session credentials
- **User Isolation**: Messages confined to authenticated user context
- **Session Tracking**: Sessions tied to HTTP session lifecycle
- **Access Control**: Resource access checks via `hasAccess()` method

## Message DTO Structure

### MessageDTO

```java
{
  "identifier": 123,
  "payload": "base64-encoded-string",
  "contentType": "application/json",
  "correlationData": [1, 2, 3, 4],
  "expiry": 60000,
  "priority": 4,
  "qualityOfService": 1,
  "creation": "2024-01-15T10:30:00",
  "dataMap": {
    "temperature": 25.5,
    "humidity": 60
  },
  "metaData": {
    "time_ms": "1705318200000",
    "server": "server-1"
  },
  "headers": {
    "correlationId": "123"
  },
  "transactionId": "txn-001",
  "deliveryStatus": "DELIVERED"
}
```

### AsyncMessageDTO

Extends MessageDTO with:

```java
{
  "destinationName": "/folder/topic",
  // ... all MessageDTO fields
}
```

## Quality of Service Levels

| Level | Name | Guarantee |
|-------|------|-----------|
| 0 | AT_MOST_ONCE | Fire and forget |
| 1 | AT_LEAST_ONCE | At least once delivery |
| 2 | EXACTLY_ONCE | Exactly once delivery |

## Priority Levels

Priority range: 0-10
- 0: Lowest priority
- 4: Normal (default)
- 10: Highest priority

## Error Handling

### Status Codes
- **200 OK**: Operation successful
- **400 Bad Request**: Invalid request format
- **401 Unauthorized**: Missing or invalid credentials
- **403 Forbidden**: User lacks necessary permissions
- **500 Internal Error**: Server error

## Testing

The messaging tools include comprehensive test coverage:

- **PublishPayloadValidationTest**: 15 tests for payload encoding and validation
- **SseStreamParsingTest**: 18 tests for SSE message parsing and formatting
- **TransactionFlowTest**: 20 tests for transaction handling and state management
- **SubscriptionSessionManagementTest**: 24 tests for subscriptions and sessions
- **MessagingTelemetryTest**: 18 tests for telemetry and performance metrics
- **MessagingToolsIntegrationTest**: 21 integration tests for complete workflows

Run tests with:
```bash
mvn test -Dtest=PublishPayloadValidationTest
mvn test -Dtest=SseStreamParsingTest
mvn test -Dtest=TransactionFlowTest
mvn test -Dtest=SubscriptionSessionManagementTest
mvn test -Dtest=MessagingTelemetryTest
mvn test -Dtest=MessagingToolsIntegrationTest
```

## Best Practices

### Publish Operations
1. Use appropriate QoS level based on requirements
2. Include correlation IDs for tracking
3. Set reasonable expiry times for messages
4. Use transactions for critical operations

### Subscriptions
1. Use named subscriptions for long-lived consumer groups
2. Apply filters to reduce unnecessary message processing
3. Monitor subscription depth to detect backlog
4. Use transactional subscriptions for guaranteed delivery

### Error Handling
1. Check for HTTP status codes in responses
2. Monitor transaction status for failures
3. Implement retry logic with exponential backoff
4. Log delivery failures and error details

### Performance
1. Batch operations when possible
2. Use appropriate depth limits for consumption
3. Monitor latency metrics
4. Clean up unused sessions regularly

## Examples

### Publishing a Message

```json
POST /api/v1/messaging/publish
Content-Type: application/json

{
  "destinationName": "sensor/data",
  "message": {
    "payload": "eyJ0ZW1wZXJhdHVyZSI6IDI1LjUsICJodW1pZGl0eSI6IDYwfQ==",
    "contentType": "application/json",
    "qualityOfService": 1,
    "priority": 4
  },
  "headers": {
    "source": "sensor-1",
    "location": "room-101"
  },
  "retain": false
}
```

### Creating a Subscription

```json
POST /api/v1/messaging/subscribe
Content-Type: application/json

{
  "destinationName": "sensor/+/temperature",
  "namedSubscription": "tempAlerts",
  "filter": "temperature > 30",
  "maxDepth": 100,
  "transactional": false
}
```

### Consuming Messages with Depth

```json
POST /api/v1/messaging/consume
Content-Type: application/json

{
  "destination": "sensor/data",
  "depth": 50
}
```

### Committing Messages

```json
POST /api/v1/messaging/commit
Content-Type: application/json

{
  "destinationName": "sensor/data",
  "eventIds": [1, 2, 3, 4, 5]
}
```

## Configuration

Messaging tools use the standard MAPS Messaging Server configuration. Key properties:

- Session timeout: Configured in HTTP session settings
- Message retention: Configured per destination
- QoS support: Depends on broker implementation
- SSE settings: Default 30-second keep-alive

## Troubleshooting

### Messages Not Being Received
1. Verify subscription exists and is active
2. Check filter expressions for syntax errors
3. Verify topic permissions
4. Check queue depth and increase if needed

### High Latency
1. Monitor server resources
2. Check network conditions
3. Review retry and backoff settings
4. Consider batching operations

### Failed Transactions
1. Check transaction status field
2. Review error details in response
3. Verify message formats
4. Check transaction timeout settings

## Security Considerations

1. All operations require authentication via HTTP session
2. Access control checks are enforced per operation
3. Messages are isolated per session
4. Credentials are handled by standard auth mechanism
5. SSL/TLS should be used for secure transport
