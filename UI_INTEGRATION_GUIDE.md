# UI Integration Guide

This guide describes how to integrate the new React Admin UI with the Maps Messaging Server and what needs to be implemented on the backend.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Backend API Requirements](#backend-api-requirements)
3. [Building the UI](#building-the-ui)
4. [Development Workflow](#development-workflow)
5. [Production Deployment](#production-deployment)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)

## Architecture Overview

The application uses a modern frontend architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────┐
│ React Admin UI (Port 3000 in dev)                   │
├─────────────────────────────────────────────────────┤
│ ┌─────────────────┐  ┌──────────────────────────┐  │
│ │ React Router    │  │ React Query (TanStack)   │  │
│ │ - PrivateRoute  │  │ - Server state           │  │
│ │ - Layouts       │  │ - Caching & sync        │  │
│ └─────────────────┘  └──────────────────────────┘  │
├─────────────────────────────────────────────────────┤
│ Axios HTTP Client with Interceptors                │
│ ├─ Request: Add Authorization header               │
│ ├─ Response: Detect 401 → Refresh token            │
│ └─ Queue: Hold failed requests during refresh      │
├─────────────────────────────────────────────────────┤
│ AuthContext (Redux-like state management)          │
│ ├─ isAuthenticated, user, token, expiresIn         │
│ └─ login(), logout(), refreshTokens()              │
└─────────────────────────────────────────────────────┘
         │
         │ HTTPS / CORS
         │
┌─────────────────────────────────────────────────────┐
│ Maps Messaging Server (Port 8080)                   │
├─────────────────────────────────────────────────────┤
│ Authentication Endpoints:                           │
│ - POST /api/v1/login                               │
│ - POST /api/v1/refreshToken                        │
│ - POST /api/v1/logout                              │
│                                                     │
│ Feature Endpoints (require valid token):           │
│ - GET /api/v1/dashboard                            │
│ - GET /api/v1/messaging/topics                     │
│ - ... (future endpoints)                           │
└─────────────────────────────────────────────────────┘
```

## Backend API Requirements

### Critical: Three Authentication Endpoints

The server **must** implement these three endpoints for the UI to function:

#### 1. POST /api/v1/login

**Purpose**: Authenticate and issue tokens

```
Request:
{
  "username": "admin",
  "password": "secret123"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "username": "admin",
    "roles": ["admin"],
    "email": "admin@example.com"
  }
}

Error Response (401 Unauthorized):
{
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid username or password"
}
```

**Implementation Notes**:
- Validate credentials against user database
- Generate JWT token with 1-hour expiry (`expiresIn: 3600`)
- Generate refresh token with longer expiry (7-30 days)
- Include `sub` (username) and `exp` claims in JWT
- Support optional Basic Authentication header

#### 2. POST /api/v1/refreshToken

**Purpose**: Issue new token using refresh token

```
Request:
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}

Error Response (401 Unauthorized):
{
  "error": "INVALID_TOKEN",
  "message": "Refresh token has expired or is invalid"
}
```

**Implementation Notes**:
- Validate refresh token signature and expiry
- Check if token is in blacklist/revocation list (if implemented)
- Issue new access token
- Optionally rotate refresh token
- Store in-memory or database: `(userId, refreshToken, expiresAt)`

#### 3. POST /api/v1/logout

**Purpose**: Invalidate tokens and end session

```
Request:
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Response (200 OK):
{
  "success": true
}

Error Response (401 Unauthorized):
{
  "error": "UNAUTHORIZED",
  "message": "Token is missing or invalid"
}
```

**Implementation Notes**:
- Extract token from `Authorization: Bearer {token}` header
- Add token to blacklist (in-memory set or database)
- Log the logout event for audit trail
- Can invalidate all user's refresh tokens (optional)

### Token Implementation

Use JWT (JSON Web Token) for stateless authentication:

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "admin",        // Subject (username)
  "iat": 1704067200,     // Issued at
  "exp": 1704070800,     // Expiry (3600 seconds later)
  "roles": ["admin"],    // Optional: user roles
  "permissions": [...]   // Optional: user permissions
}
```

### CORS Configuration

The server must allow the UI to make cross-origin requests:

```java
// In your Jersey/Spring config:
@CrossOriginResourceSharing(
  allowedOrigins = {"http://localhost:3000", "https://admin.yourdomain.com"},
  allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"},
  allowedHeaders = {"Content-Type", "Authorization"},
  exposedHeaders = {"Content-Type", "Authorization"},
  allowCredentials = true,
  maxAge = 86400
)
```

Or add to response headers:

```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 86400
```

## Building the UI

### Development Build

```bash
cd ui/maps-admin-ui
npm install
npm run dev
```

The UI runs on `http://localhost:3000` and proxies API requests to `http://localhost:8080`.

### Production Build (with Maven)

Add to `pom.xml`:

```xml
<plugin>
  <groupId>com.github.eirslett</groupId>
  <artifactId>frontend-maven-plugin</artifactId>
  <version>1.14.2</version>
  <configuration>
    <nodeVersion>v18.16.0</nodeVersion>
    <npmVersion>9.6.4</npmVersion>
    <workingDirectory>ui/maps-admin-ui</workingDirectory>
  </configuration>
  <executions>
    <execution>
      <id>install node and npm</id>
      <goals>
        <goal>install-node-and-npm</goal>
      </goals>
    </execution>
    <execution>
      <id>npm install</id>
      <goals>
        <goal>npm</goal>
      </goals>
      <configuration>
        <arguments>install</arguments>
      </configuration>
    </execution>
    <execution>
      <id>npm build</id>
      <goals>
        <goal>npm</goal>
      </goals>
      <configuration>
        <arguments>run build:prod</arguments>
      </configuration>
      <phase>prepare-package</phase>
    </execution>
  </executions>
</plugin>
```

Build:

```bash
mvn clean package -P ui
```

Output placed in `target/classes/html/admin/` and packaged in server JAR.

## Development Workflow

### 1. Start the Server

```bash
cd /home/engine/project
mvn clean install
mvn spring-boot:run
# Server runs on http://localhost:8080
```

Note: Currently, the server requires implementing the auth endpoints first.

### 2. Start the UI Dev Server

In a separate terminal:

```bash
cd ui/maps-admin-ui
npm install  # First time only
npm run dev
# UI runs on http://localhost:3000
```

### 3. Implement Backend Endpoints

Implement the three auth endpoints in your server:

```java
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1. Validate credentials
        // 2. Generate JWT token
        // 3. Generate refresh token
        // 4. Return response
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody TokenRefreshRequest request) {
        // 1. Validate refresh token
        // 2. Generate new JWT token
        // 3. Return response
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
        @RequestHeader("Authorization") String bearerToken) {
        // 1. Extract token
        // 2. Add to blacklist
        // 3. Return success
    }
}
```

### 4. Test Authentication Flow

1. Open browser to http://localhost:3000
2. You should see the login page
3. Enter credentials (once backend is ready)
4. Should be redirected to dashboard
5. Check browser DevTools:
   - Network tab: Verify token in Authorization header
   - Application tab: Check localStorage for `auth_tokens`

## Production Deployment

### Build Steps

1. **Build the UI**:
   ```bash
   cd ui/maps-admin-ui
   npm install
   npm run build:prod
   ```

2. **Build the Server**:
   ```bash
   mvn clean package -P ui -DskipTests
   ```

3. **Deploy JAR**:
   ```bash
   java -jar target/maps-*.jar
   ```

### Configuration

1. **Set environment variables** for the build:
   ```bash
   export APP_TITLE="My Organization Admin"
   export APP_VERSION="1.0.0"
   export API_ENDPOINT="/api/v1"
   npm run build:prod
   ```

2. **Configure CORS** for production domain:
   ```
   Access-Control-Allow-Origin: https://admin.yourdomain.com
   ```

3. **Enable HTTPS**:
   - Use reverse proxy (Nginx, Apache)
   - Or configure Java keystore with SSL cert

4. **Configure authentication**:
   - Set token expiry appropriately
   - Implement token blacklist for logout
   - Consider implementing refresh token rotation

### File Serving

The built UI files (`src/main/html/admin/`) should be served at:
```
https://yourdomain.com/admin/
```

Configure your web server to serve `index.html` for all `*` paths to support client-side routing.

**Nginx example**:
```nginx
location /admin {
  alias /var/lib/maps/html/admin;
  try_files $uri $uri/ /admin/index.html;
}
```

## Testing

### Run All Tests

```bash
cd ui/maps-admin-ui
npm test
```

### Run Specific Test

```bash
npm test -- --run src/tests/LoginPage.test.jsx
```

### Watch Mode

```bash
npm test -- --watch
```

### Coverage Report

```bash
npm test -- --coverage
```

### Storybook Visual Testing

```bash
npm run storybook
```

Opens at http://localhost:6006

## Troubleshooting

### "Cannot GET /" on http://localhost:3000

**Cause**: Dev server not running
**Solution**:
```bash
cd ui/maps-admin-ui
npm run dev
```

### CORS error in console

**Cause**: Server not sending correct CORS headers
**Solution**:
1. Verify server has `@CrossOriginResourceSharing` or CORS filter
2. Verify origin matches (http://localhost:3000 for dev)
3. Check browser Network tab for Response Headers

### "Invalid credentials" on every login

**Cause**: 
- Endpoint not implemented
- Credentials wrong
- Server error

**Solution**:
1. Check server logs for errors
2. Use curl to test endpoint: `curl -X POST http://localhost:8080/api/v1/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin"}'`
3. Verify endpoint returns correct response format

### "Session expired" immediately after login

**Cause**: Token expiry time calculated wrong or token invalid
**Solution**:
1. Check `expiresIn` value in response (should be > 0, e.g., 3600)
2. Verify token signature/validation logic
3. Check browser localStorage: `JSON.parse(localStorage.auth_tokens)`

### Logout doesn't redirect to login

**Cause**: localStorage not cleared or redirect not triggered
**Solution**:
1. Manually clear: `localStorage.removeItem('auth_tokens')`
2. Hard refresh: Ctrl+Shift+R
3. Check browser console for errors

### Tests fail with "localStorage is not defined"

**Cause**: Test setup missing
**Solution**:
```bash
npm test -- --run
```

Or verify `vitest.config.js` has:
```javascript
test: {
  environment: 'jsdom',
  setupFiles: './src/tests/setup.js'
}
```

## Next Steps

1. **Implement the three API endpoints** in your server
2. **Test authentication flow** manually in browser
3. **Add feature endpoints** as needed (topics, queues, etc.)
4. **Implement authorization** (role-based access control)
5. **Add audit logging** for security events
6. **Deploy to production** with HTTPS and proper CORS

## Support & Issues

For issues, check:
1. `ui/README.md` - UI documentation
2. `docs/auth-ui-integration.md` - Detailed auth architecture
3. Browser DevTools Network and Console tabs
4. Server logs for API errors
