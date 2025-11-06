# Maps Messaging Admin UI

This is the React-based administrative interface for the Maps Messaging Server.

## Prerequisites

- Node.js LTS (v20+) - [Download Node.js](https://nodejs.org/)
- npm (comes with Node.js)

## Development

### Local Development

To start the development server:

```bash
cd ui/maps-admin-ui
npm install
npm run dev
```

The development server will start at `http://localhost:3000` and will proxy API requests to `http://localhost:8080`.

### Building for Production

To build the UI for production:

```bash
cd ui/maps-admin-ui
npm ci
npm run build -- --mode production
```

The build output will be placed in `../../../target/classes/html/admin/` to be included in the server JAR.

## Configuration

The UI loads configuration from `public/config.json`. The file contains placeholders that can be replaced by Maven properties during the build process:

```json
{
  "apiBaseUrl": "${API_BASE_URL:http://localhost:8080}",
  "apiVersion": "v1",
  "title": "Maps Messaging Admin",
  "version": "${PROJECT_VERSION:4.1.1}",
  "buildTime": "${BUILD_TIME:unknown}"
}
```

### Environment Variables

You can override configuration using environment variables:

- `VITE_API_BASE_URL` - Base URL for API requests (default: http://localhost:8080)
- `VITE_API_VERSION` - API version (default: v1)

## Integration with Maven Build

The UI build is integrated into the main Maven build through the `ui` profile:

```bash
# Build with UI included
./build.sh --with-ui

# Or using Maven directly
mvn clean install -Pui
```

The frontend-maven-plugin will:
1. Install Node.js v20.12.0 and npm 10.5.0
2. Run `npm ci` to install dependencies
3. Run `npm run build -- --mode production` to build the UI

## Deployment

### Production Deployment

1. Build the server with UI:
   ```bash
   ./build.sh --with-ui
   ```

2. The UI will be bundled into the server JAR under `/html/admin/`

3. Configure the API base URL by setting the appropriate environment variable or updating the config.json

### Manual UI Deployment

If you prefer to build and deploy the UI separately:

1. Build the UI:
   ```bash
   cd ui/maps-admin-ui
   npm ci
   npm run build -- --mode production
   ```

2. Copy the built files to your server deployment:
   ```bash
   cp -r dist/* /path/to/server/www/admin/
   ```

3. Configure your web server to serve the UI files from the appropriate path

## OpenAPI Integration

The UI is designed to work with the Maps Messaging Server's OpenAPI specification. The API documentation is available at:

- Swagger UI: `http://localhost:8080/api/docs` (when server is running)
- OpenAPI JSON: `http://localhost:8080/api/openapi.json`

## Troubleshooting

### Node.js Version Issues

Make sure you're using Node.js LTS v20 or later:
```bash
node --version  # Should be v20.x.x or later
```

### Build Failures

If the build fails, try:
```bash
# Clear npm cache
npm cache clean --force

# Remove node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Reinstall dependencies
npm ci
```

### API Connection Issues

If the UI can't connect to the API:

1. Check that the Maps Messaging Server is running
2. Verify the API base URL configuration
3. Check browser console for CORS errors
4. Ensure the API endpoints are accessible

## Development Tips

- Use `npm run lint` to check code style
- Use `npm run preview` to preview the production build locally
- The Vite config includes proxy settings for development API access
- All API requests should use relative paths when deployed with the server