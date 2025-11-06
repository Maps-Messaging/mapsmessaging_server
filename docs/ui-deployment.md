# Maps Messaging UI Deployment Guide

This document provides detailed instructions for deploying the Maps Messaging Admin UI in various scenarios.

## Deployment Scenarios

### 1. Bundled Deployment (Recommended)

The UI is bundled into the server JAR and served from the same application server.

#### Building with UI

```bash
# Build server with UI included
./build.sh --with-ui

# Or using Maven
mvn clean install -Pui -Dapi.base.url=https://your-server.com
```

#### Configuration

The UI reads configuration from `config.json` which is generated during build time. You can override the API base URL:

```bash
# Build with custom API URL
mvn clean install -Pui -Dapi.base.url=https://prod-server.example.com
```

#### Deployment

1. Deploy the JAR as usual: `java -jar maps-4.1.1.jar`
2. Access the UI at: `http://localhost:8080/admin/`
3. API documentation at: `http://localhost:8080/api/docs`

### 2. Separate Deployment

Deploy the UI separately from the server (useful for CDNs or different domains).

#### Build UI Separately

```bash
cd ui/maps-admin-ui
npm ci
npm run build:prod
```

#### Configure for Separate Deployment

1. Update `config.json` with your server API URL:
   ```json
   {
     "apiBaseUrl": "https://your-api-server.com",
     "apiVersion": "v1",
     "title": "Maps Messaging Admin",
     "version": "4.1.1",
     "buildTime": "2024-01-15T10:30:00Z"
   }
   ```

2. Deploy the built files to your web server:
   ```bash
   cp -r dist/* /var/www/html/admin/
   ```

3. Configure CORS on the Maps Messaging Server if needed.

### 3. Docker Deployment

#### Dockerfile Example

```dockerfile
FROM node:20-alpine AS ui-builder
WORKDIR /app/ui
COPY ui/maps-admin-ui/package*.json ./
RUN npm ci
COPY ui/maps-admin-ui/ .
RUN npm run build:prod

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=ui-builder /app/ui/target/classes/html/admin ./src/main/html/admin
COPY target/maps-*.jar ./maps.jar
EXPOSE 8080
CMD ["java", "-jar", "maps.jar"]
```

#### Multi-stage Build

```bash
# Build with UI
docker build -t maps-messaging:with-ui .

# Run
docker run -p 8080:8080 maps-messaging:with-ui
```

## Configuration Options

### Environment Variables

The UI can be configured using environment variables during build:

- `API_BASE_URL` - Base URL for the Maps Messaging API
- `PROJECT_VERSION` - Version string displayed in the UI
- `BUILD_TIME` - Build timestamp (auto-generated)

### Runtime Configuration

For production deployments, you may want to configure:

#### Reverse Proxy (Nginx)

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /admin/ {
        alias /var/www/html/admin/;
        try_files $uri $uri/ /admin/index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### Apache HTTPD

```apache
<VirtualHost *:80>
    ServerName your-domain.com
    
    Alias /admin /var/www/html/admin
    <Directory /var/www/html/admin>
        RewriteEngine On
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /admin/index.html [L]
    </Directory>
    
    ProxyPass /api/ http://localhost:8080/api/
    ProxyPassReverse /api/ http://localhost:8080/api/
</VirtualHost>
```

## Security Considerations

### HTTPS

Always use HTTPS in production:

1. Configure SSL/TLS on your Maps Messaging Server
2. Update `API_BASE_URL` to use `https://`
3. Ensure your reverse proxy handles SSL termination properly

### Authentication

The UI relies on the Maps Messaging Server's authentication. Ensure:

1. Authentication is properly configured on the server
2. CORS settings allow the UI domain
3. Session cookies are properly configured with `Secure` and `HttpOnly` flags

### Content Security Policy

Consider adding CSP headers:

```http
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; connect-src 'self' https://your-api-server.com
```

## Troubleshooting

### Common Issues

1. **UI can't connect to API**
   - Check `config.json` has correct `apiBaseUrl`
   - Verify server is running and accessible
   - Check CORS configuration

2. **404 errors on refresh**
   - Ensure your web server handles client-side routing
   - Configure fallback to `index.html`

3. **Build failures**
   - Verify Node.js version (v20+)
   - Clear npm cache: `npm cache clean --force`
   - Remove `node_modules` and reinstall

### Debug Mode

To enable debug mode in the browser:

```javascript
localStorage.setItem('debug', 'maps-admin:*')
```

This will enable additional logging in the browser console.

## Performance Optimization

### Production Builds

Always use production builds:

```bash
npm run build:prod
```

This includes:
- Code minification
- Tree shaking
- Asset optimization

### Caching

Configure appropriate caching headers:

```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### CDN

For high-traffic deployments, consider serving the UI from a CDN:

1. Upload the built UI files to your CDN
2. Update `config.json` with the correct API URL
3. Configure the CDN to handle API requests appropriately

## Monitoring

### Health Checks

Monitor the UI health:

```bash
curl -f http://localhost:8080/admin/ || echo "UI health check failed"
```

### API Monitoring

Monitor API endpoints that the UI depends on:

```bash
curl -f http://localhost:8080/api/openapi.json || echo "API health check failed"
```