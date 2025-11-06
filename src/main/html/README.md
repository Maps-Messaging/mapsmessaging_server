# MAPS Messaging Server - Resource Admin Interface

A comprehensive web-based administration interface for managing MAPS Messaging Server resources.

## Quick Start

1. **Start the MAPS Server** with REST API enabled:
   ```bash
   # Ensure REST API is enabled in your configuration
   java -jar maps-messaging-server.jar
   ```

2. **Access the Admin Interface**:
   - Open your web browser
   - Navigate to: `http://localhost:8080/resource-admin.html`
   - Login with your admin credentials

3. **Explore the Interface**:
   - Use the sidebar to navigate between resource types
   - Search and filter resources using the controls
   - Click on items to view detailed information
   - Use action buttons to manage resources

## Features

### 🎯 Resource Management
- **Destinations**: Create, view, update, and delete queues and topics
- **Connections**: Monitor client connections and close when needed
- **Interfaces**: Control server endpoints (start/stop/pause/resume)
- **Integrations**: Manage inter-server connections
- **Sessions**: View and terminate user sessions

### 🔍 Monitoring & Search
- Real-time data tables with auto-refresh
- Advanced filtering with selector expressions
- Sort by multiple columns
- Search across all resource properties

### ⚡ Operations
- Bulk selection and operations
- Individual resource actions
- Optimistic UI updates
- Success/error notifications
- Confirmation dialogs for destructive actions

### 📱 User Experience
- Responsive design for all devices
- Sliding detail drawers
- Keyboard shortcuts (Esc to close, Ctrl+R to refresh)
- Loading indicators and empty states

## API Endpoints

The admin interface uses the following REST API endpoints:

### Destinations
- `GET /api/v1/server/destinations` - List destinations
- `GET /api/v1/server/destination` - Get destination details
- `POST /api/v1/server/destination` - Create destination
- `PUT /api/v1/server/destination` - Update destination
- `DELETE /api/v1/server/destination` - Delete destination

### Connections
- `GET /api/v1/server/connections` - List connections
- `GET /api/v1/server/connection` - Get connection details
- `PUT /api/v1/server/connection/close` - Close connection

### Interfaces
- `GET /api/v1/server/interfaces` - List interfaces
- `GET /api/v1/server/interface/{endpoint}` - Get interface details
- `PUT /api/v1/server/interface/{endpoint}/start` - Start interface
- `PUT /api/v1/server/interface/{endpoint}/stop` - Stop interface
- `PUT /api/v1/server/interface/{endpoint}/pause` - Pause interface
- `PUT /api/v1/server/interface/{endpoint}/resume` - Resume interface

### Integrations
- `GET /api/v1/server/integration` - List integrations
- `GET /api/v1/server/integration/{name}` - Get integration details
- `PUT /api/v1/server/integration/{name}/start` - Start integration
- `PUT /api/v1/server/integration/{name}/stop` - Stop integration
- `PUT /api/v1/server/integration/{name}/pause` - Pause integration
- `PUT /api/v1/server/integration/{name}/resume` - Resume integration

### Sessions (New)
- `GET /api/v1/session` - List active sessions
- `GET /api/v1/session/{sessionId}` - Get session details
- `DELETE /api/v1/session/{sessionId}` - Terminate session
- `PUT /api/v1/session/terminateAll` - Terminate all sessions

## Filtering Examples

### Destinations
```javascript
// Queues with messages
"type = 'queue' AND storedMessages > 0"

// Topics with high activity
"type = 'topic' AND publishedMessages > 1000"

// Empty destinations
"storedMessages = 0 AND pendingMessages = 0"
```

### Connections
```javascript
// MQTT connections
"protocolName = 'MQTT'"

// Long-running connections
"connectedTimeMs > 3600000"
```

### Sessions
```javascript
// Admin sessions
"user = 'admin'"

// Long-running sessions
"connectedTimeMs > 7200000"
```

## Testing

Run the test suite to verify functionality:

1. Open `resource-admin-tests.html` in your browser
2. All tests should pass
3. Check console for any errors

## Configuration

### Authentication
Configure authentication in your MAPS server:

```yaml
rest:
  enabled: true
  port: 8080
  authentication:
    enabled: true
    type: jwt  # or basic, oauth
```

### CORS
Enable CORS for web interface:

```yaml
rest:
  cors:
    enabled: true
    allowedOrigins: ["*"]
```

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Esc` | Close detail drawer |
| `Ctrl+R` | Refresh current data |
| `Ctrl+A` | Select all items |
| `Delete` | Delete selected items |

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Check authentication configuration
   - Verify user credentials
   - Ensure proper role assignments

2. **CORS Errors**
   - Enable CORS in server configuration
   - Check allowed origins
   - Verify preflight requests

3. **Connection Issues**
   - Verify server is running
   - Check port configuration
   - Review firewall settings

4. **Performance Issues**
   - Check server resources
   - Review query complexity
   - Monitor network latency

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    io.mapsmessaging.rest: DEBUG
```

## Support

For issues and support:
1. Check the [documentation](../docs/resource-admin.md)
2. Review test results
3. Check server logs
4. Verify configuration

## Contributing

To extend the resource admin:
1. Add new resource types to `resourceConfig`
2. Define API endpoints and field mappings
3. Update UI components as needed
4. Add corresponding tests
5. Update documentation

---

**Version**: 1.0.0  
**Last Updated**: 2025-06-17  
**Compatibility**: MAPS Messaging Server 1.0+