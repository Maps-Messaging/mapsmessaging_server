# MAPS Messaging Server - Monitoring Dashboard

A comprehensive web-based monitoring interface for the MAPS Messaging Server that provides real-time insights into server performance, health status, and operational metrics.

## Features

### 🎯 Real-time Dashboard
- **Server Health Overview**: At-a-glance view of server status with severity indicators
- **Auto-refresh Controls**: Configurable refresh intervals (1s, 5s, 10s, 30s, 1m)
- **Performance Metrics**: Live CPU, memory, and connection statistics
- **Message Rate Charts**: Real-time visualization of publish/subscribe rates

### 📊 Detailed Metrics
- **System Information**: Server version, build details, Java runtime info
- **Performance Statistics**: Memory usage, GC statistics, thread distribution
- **Cache Information**: Hit rates, evictions, memory consumption
- **Interactive Charts**: CPU usage, message rates, thread state distribution

### 🔍 Subsystem Monitoring
- **Status Overview**: Grouped view of all server subsystems
- **Severity Indicators**: Color-coded status (OK, WARN, ERROR, STOPPED)
- **Drill-down Details**: Metrics, configuration, and operational data
- **Management Actions**: Restart, start, stop operations where supported

### 📝 Live Log Streaming
- **Real-time Logs**: Server-Sent Events (SSE) for instant log updates
- **Advanced Filtering**: Text search and log level filtering
- **Playback Controls**: Pause, resume, and clear log display
- **Export Functionality**: Download filtered logs as text files
- **Fallback Support**: Manual polling when SSE is unavailable

### 🔄 Update Notifications
- **Automatic Detection**: Alerts for server and schema updates
- **Non-intrusive**: Dismissible notification banners
- **Background Checking**: Periodic verification of available updates

## 🚀 Getting Started

### Prerequisites
- MAPS Messaging Server v4.1.1 or later
- Modern web browser with JavaScript enabled
- Server with REST API enabled

### Accessing the Dashboard

1. Start your MAPS Messaging Server
2. Open your web browser and navigate to:
   ```
   http://localhost:8080/
   ```
3. Click on "Monitoring Dashboard" from the landing page

### Direct Access
You can also access the dashboard directly:
```
http://localhost:8080/monitoring.html
```

## 🧪 Testing

### Built-in Test Suite
Access the comprehensive test suite at:
```
http://localhost:8080/monitoring-tests.html
```

The test suite includes:
- **API Connectivity Tests**: Validates all REST API endpoints
- **SSE Functionality**: Tests Server-Sent Events implementation
- **UI Component Tests**: Verifies dashboard interface elements
- **Polling Simulation**: Tests auto-refresh and data handling

### Manual Testing
Open browser console and run:
```javascript
dashboardTests.runAllTests()
```

## 🔧 Configuration

### Server Configuration
Ensure the following are enabled in your server configuration:

```yaml
rest:
  enabled: true
  port: 8080
  authentication:
    enabled: true

logging:
  sse:
    enabled: true
    maxConnections: 10
```

### CORS Configuration
For cross-origin requests, configure CORS headers:

```yaml
rest:
  cors:
    enabled: true
    allowedOrigins: ["*"]
    allowedMethods: ["GET", "POST", "PUT"]
    allowedHeaders: ["Content-Type", "Authorization"]
```

## 🌐 API Endpoints

The dashboard integrates with these REST API endpoints:

### Server Status
- `GET /api/v1/server/status` - Subsystem status information
- `GET /api/v1/server/health` - Overall server health summary
- `GET /api/v1/server/details/info` - Server build and runtime information
- `GET /api/v1/server/details/stats` - Performance statistics and metrics
- `GET /api/v1/server/cache` - Cache performance and usage information

### Log Streaming
- `GET /api/v1/server/log/sse` - Obtain streaming token
- `GET /api/v1/server/log/sse/stream/{token}` - Live log stream
- `GET /api/v1/server/log` - Manual log snapshot

### Updates
- `GET /api/v1/updates` - Available server and schema updates

## 🔍 Troubleshooting

### Common Issues

#### SSE Connection Fails
1. **Check CORS**: Ensure server allows cross-origin requests
2. **Authentication**: Verify user has sufficient permissions
3. **Firewall**: Check that port 8080 is accessible
4. **Browser**: Try a different browser (Chrome, Firefox, Safari)

#### Dashboard Not Loading
1. **Server Status**: Verify MAPS server is running
2. **API Access**: Test with `curl http://localhost:8080/api/v1/server/health`
3. **JavaScript**: Check browser console for error messages
4. **Network**: Use browser dev tools to inspect failed requests

#### Charts Not Displaying
1. **Chart.js**: Verify library loads correctly (check network tab)
2. **Data Format**: Check API responses contain expected data structure
3. **Browser Console**: Look for JavaScript errors
4. **Responsive**: Try resizing browser window

### Debug Mode
Enable debug logging in browser console:
```javascript
localStorage.setItem('maps-debug', 'true');
location.reload();
```

## 🎨 Customization

### Theming
The dashboard uses CSS variables for easy customization:

```css
:root {
    --primary-color: #2c3e50;
    --secondary-color: #3498db;
    --success-color: #27ae60;
    --warning-color: #f39c12;
    --error-color: #e74c3c;
}
```

### Configuration File
Edit `monitoring/config.json` to customize:
- Refresh intervals
- Chart settings
- API timeouts
- UI colors and icons

## 📱 Mobile Support

The dashboard is fully responsive and works on:
- **Desktop**: Chrome, Firefox, Safari, Edge
- **Tablet**: iPad, Android tablets
- **Mobile**: iPhone, Android phones

### Mobile Considerations
- **Touch Gestures**: Swipe to navigate between sections
- **Performance**: Reduced animation on mobile devices
- **Data Usage**: Lower refresh intervals recommended

## 🔒 Security

### Authentication
- Dashboard respects server authentication settings
- Sessions are managed via server-side tokens
- No credentials stored in browser

### Permissions
Required permissions for full functionality:
- `server:read` - View server status and metrics
- `logs:read` - Access log streaming
- `cache:read` - View cache information

## 📈 Performance

### Optimization Features
- **Data Caching**: 5-second cache for API responses
- **Efficient Updates**: Only changed data is refreshed
- **Lazy Loading**: Charts load data only when visible
- **Connection Pooling**: Reuses HTTP connections

### Recommended Settings
- **Production**: 10-30 second refresh intervals
- **Development**: 1-5 second refresh intervals
- **Mobile**: 30-60 second refresh intervals

## 🤝 Contributing

Want to improve the monitoring dashboard?

1. **Report Issues**: Use GitHub issue tracker
2. **Feature Requests**: Submit enhancement ideas
3. **Pull Requests**: Follow contribution guidelines
4. **Testing**: Include tests with new features

## 📚 Additional Resources

- [MAPS Server Documentation](https://docs.mapsmessaging.io/)
- [REST API Reference](http://localhost:8080/swagger-ui/index.html)
- [Configuration Guide](https://docs.mapsmessaging.io/docs/configuration)
- [Security Best Practices](https://docs.mapsmessaging.io/docs/security)

---

**Version**: 1.0.0  
**Last Updated**: 2024  
**Compatible**: MAPS Messaging Server v4.1.1+