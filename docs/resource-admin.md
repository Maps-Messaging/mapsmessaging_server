# MAPS Messaging Server - Resource Admin Documentation

## Overview

The Resource Admin interface provides comprehensive management capabilities for MAPS Messaging Server resources including destinations, connections, interfaces, integrations, and sessions. This web-based admin tool offers real-time monitoring, CRUD operations, and bulk management capabilities.

## Features

### Resource Management
- **Destinations**: View, create, and manage message queues and topics
- **Connections**: Monitor and manage client connections
- **Interfaces**: Control server endpoints (start, stop, pause, resume)
- **Integrations**: Manage inter-server connections
- **Sessions**: View and terminate user sessions

### Key Capabilities
- Real-time data tables with sorting and filtering
- Detailed resource information in sliding drawers
- Bulk operations with selection capabilities
- Auto-refresh functionality
- Search and advanced filtering
- Responsive design for mobile and desktop
- Permission-aware interface elements
- Optimistic UI updates with rollback on error
- Success/error toast notifications
- Confirmation dialogs for destructive operations

## API Endpoints

### Authentication Requirements
All endpoints require authentication if enabled in the server configuration. The following roles are typically required:
- **Admin Role**: Full access to all resources and operations
- **Reader Role**: Read-only access to monitoring endpoints
- **Operator Role**: Read access plus control operations (start/stop/pause/resume)

### Destinations API
```
GET    /api/v1/server/destinations     - List all destinations
GET    /api/v1/server/destination      - Get destination details
POST   /api/v1/server/destination      - Create new destination
PUT    /api/v1/server/destination      - Update destination
DELETE /api/v1/server/destination      - Delete destination
```

**Query Parameters:**
- `filter`: Selector expression for filtering (e.g., "type = 'queue' AND storedMessages > 50")
- `size`: Maximum number of results (default: 40)
- `sortBy`: Sort field (Name, Published, Delivered, Stored, Pending, Delayed, Expired)

**Filter Examples:**
```javascript
// Queues with messages
"type = 'queue' AND storedMessages > 0"

// Topics with high activity
"type = 'topic' AND publishedMessages > 1000"

// Empty destinations
"storedMessages = 0 AND pendingMessages = 0"
```

### Connections API
```
GET /api/v1/server/connections        - List all connections
GET /api/v1/server/connection         - Get connection details
PUT /api/v1/server/connection/close    - Close specific connection
```

**Filter Examples:**
```javascript
// MQTT connections
"protocolName = 'MQTT'"

// Long-running connections
"connectedTimeMs > 3600000"

// High activity connections
"totalMessages > 10000"
```

### Interfaces API
```
GET  /api/v1/server/interfaces                    - List all interfaces
GET  /api/v1/server/interface/{endpoint}          - Get interface details
PUT  /api/v1/server/interface/{endpoint}/start    - Start interface
PUT  /api/v1/server/interface/{endpoint}/stop     - Stop interface
PUT  /api/v1/server/interface/{endpoint}/pause    - Pause interface
PUT  /api/v1/server/interface/{endpoint}/resume   - Resume interface
PUT  /api/v1/server/interfaces/startAll           - Start all interfaces
PUT  /api/v1/server/interfaces/stopAll            - Stop all interfaces
PUT  /api/v1/server/interfaces/pauseAll           - Pause all interfaces
PUT  /api/v1/server/interfaces/resumeAll          - Resume all interfaces
```

### Integrations API
```
GET  /api/v1/server/integration                    - List all integrations
GET  /api/v1/server/integration/{name}             - Get integration details
GET  /api/v1/server/integration/{name}/connection  - Get integration connection status
PUT  /api/v1/server/integration/{name}/start       - Start integration
PUT  /api/v1/server/integration/{name}/stop        - Stop integration
PUT  /api/v1/server/integration/{name}/pause       - Pause integration
PUT  /api/v1/server/integration/{name}/resume      - Resume integration
PUT  /api/v1/server/integration/startAll            - Start all integrations
PUT  /api/v1/server/integration/stopAll             - Stop all integrations
PUT  /api/v1/server/integration/pauseAll            - Pause all integrations
PUT  /api/v1/server/integration/resumeAll           - Resume all integrations
```

### Sessions API (New)
```
GET    /api/v1/session                    - List all active sessions
GET    /api/v1/session/{sessionId}        - Get session details
DELETE /api/v1/session/{sessionId}        - Terminate specific session
PUT    /api/v1/session/terminateAll        - Terminate all sessions (except current)
```

**Filter Examples:**
```javascript
// Admin sessions
"user = 'admin'"

// Long-running sessions
"connectedTimeMs > 7200000"

// Specific protocol sessions
"protocolName = 'REST'"
```

## User Interface

### Navigation
- **Sidebar**: Quick navigation between resource types
- **Search Bar**: Real-time search with debouncing
- **Filter Dropdown**: Pre-defined filters for common queries
- **Sort Options**: Multiple sorting criteria
- **Auto-refresh Toggle**: Automatic data updates

### Data Tables
- **Selection**: Individual and bulk selection with checkboxes
- **Status Indicators**: Color-coded status badges
- **Action Buttons**: Context-sensitive actions per resource
- **Pagination**: Efficient handling of large datasets
- **Responsive Design**: Mobile-friendly table layout

### Detail Drawers
- **Slide-in Panel**: Non-intrusive detail view
- **Edit Mode**: Inline editing with validation
- **Metrics Display**: Key performance indicators
- **Related Information**: Associated resources and statistics
- **Action Controls**: Individual resource management

### Notifications
- **Success Toasts**: Confirmation of successful operations
- **Error Messages**: Detailed error information
- **Warnings**: Non-critical issues and warnings
- **Loading Indicators**: Visual feedback during operations

## Configuration

### Server Configuration
The REST API server must be enabled in the server configuration:

```yaml
rest:
  enabled: true
  port: 8080
  host: 0.0.0.0
  authentication:
    enabled: true
    type: jwt  # or basic, oauth
  cors:
    enabled: true
    allowedOrigins: ["*"]
```

### Authentication Setup
Configure authentication based on your requirements:

#### JWT Authentication
```yaml
rest:
  authentication:
    type: jwt
    jwt:
      secret: your-secret-key
      expiration: 3600
```

#### Basic Authentication
```yaml
rest:
  authentication:
    type: basic
    users:
      - username: admin
        password: admin123
        roles: [admin]
      - username: operator
        password: operator123
        roles: [operator]
```

### Permission Mapping
- **admin**: Full access to all endpoints and operations
- **operator**: Read access + control operations (start/stop/pause/resume)
- **reader**: Read-only access to monitoring endpoints

## Rate Limiting and Performance

### Rate Limiting
The API implements rate limiting to prevent abuse:
- **Default Limit**: 100 requests per minute per user
- **Burst Limit**: 200 requests per minute
- **Admin Exemption**: Admin users have higher limits

### Caching
Responses are cached to improve performance:
- **Cache Duration**: 30 seconds for list endpoints
- **Cache Invalidation**: Automatic on mutations
- **Cache Keys**: Based on endpoint, parameters, and user context

### Pagination
Large datasets are paginated to ensure performance:
- **Default Page Size**: 20 items
- **Maximum Page Size**: 100 items
- **Page Offset**: Zero-based indexing

## Security Considerations

### Input Validation
- All inputs are validated server-side
- SQL injection protection via parameterized queries
- XSS protection via output encoding
- CSRF protection via token validation

### Authentication Security
- JWT tokens with expiration
- Secure password storage (bcrypt)
- Session management with timeout
- Logout functionality

### Authorization
- Role-based access control
- Resource-level permissions
- Action-specific permissions
- Audit logging for all operations

## Monitoring and Logging

### Request Logging
All API requests are logged with:
- Timestamp
- User identity
- Endpoint
- HTTP method
- Response status
- Processing time

### Audit Trail
Important operations are audited:
- Resource creation/modification/deletion
- Control operations (start/stop/pause/resume)
- Session termination
- Authentication events

### Performance Metrics
- Response times
- Request counts
- Error rates
- Resource utilization

## Troubleshooting

### Common Issues

#### Authentication Failures
- Check authentication configuration
- Verify user credentials
- Ensure proper role assignments
- Check token expiration

#### Permission Errors
- Verify user roles
- Check resource permissions
- Review access control lists
- Check group memberships

#### Performance Issues
- Check database performance
- Review query complexity
- Monitor resource utilization
- Check network latency

#### Connection Issues
- Verify network connectivity
- Check firewall rules
- Review load balancer configuration
- Monitor SSL certificates

### Debug Mode
Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    io.mapsmessaging.rest: DEBUG
    io.mapsmessaging.auth: DEBUG
```

### Health Checks
Monitor system health via:
```
GET /api/v1/health
GET /api/v1/metrics
GET /api/v1/status
```

## Development

### Local Development
1. Start the MAPS server with REST API enabled
2. Open `resource-admin.html` in a web browser
3. Ensure CORS is properly configured

### Testing
Run the test suite:
1. Open `resource-admin-tests.html` in a browser
2. All tests should pass
3. Check browser console for any errors

### Customization
The interface can be customized by:
- Modifying CSS variables in `resource-admin.css`
- Extending the JavaScript class in `resource-admin.js`
- Adding new resource types to the configuration
- Customizing field definitions and display formats

## API Examples

### Creating a Destination
```bash
curl -X POST http://localhost:8080/api/v1/server/destination \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "my-queue",
    "type": "queue",
    "schemaId": "my-schema"
  }'
```

### Starting an Interface
```bash
curl -X PUT http://localhost:8080/api/v1/server/interface/mqtt/start \
  -H "Authorization: Bearer <token>"
```

### Terminating a Session
```bash
curl -X DELETE http://localhost:8080/api/v1/session/12345 \
  -H "Authorization: Bearer <token>"
```

### Filtering Destinations
```bash
curl "http://localhost:8080/api/v1/server/destinations?filter=type='queue'%20AND%20storedMessages%3E0" \
  -H "Authorization: Bearer <token>"
```

## Version History

### v1.0.0
- Initial release
- Basic CRUD operations for all resource types
- Table-based interface with filtering and sorting
- Session management capabilities
- Unit test coverage

### Future Enhancements
- Real-time WebSocket updates
- Advanced analytics dashboard
- Custom alert configurations
- Integration with external monitoring systems
- Mobile application support