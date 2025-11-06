# Authentication & UI Integration Guide

This document describes the authentication architecture, API expectations, and configuration for the Maps Messaging Admin UI.

## Overview

The Admin UI implements OAuth 2.0-like flows with JWT tokens, supporting both credential-based and basic authentication methods. The server must implement three core authentication endpoints.

## Required API Endpoints

All endpoints should respond with appropriate HTTP status codes and error handling as described below.

### 1. Login Endpoint

**Endpoint**: `POST /api/v1/login`

**Purpose**: Authenticate a user and issue tokens

**Request**:
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Success Response** (200 OK):
```json
{
  "token": "string (JWT bearer token)",
  "refreshToken": "string (refresh token)",
  "expiresIn": 3600,
  "user": {
    "username": "string",
    "roles": ["string"],
    "permissions": ["string"]
  }
}
```

**Error Response** (401 Unauthorized):
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid username or password"
}
```

**Notes**:
- Token should be a JWT with standard claims (sub, iat, exp)
- `expiresIn` is token lifetime in seconds (recommend 3600 = 1 hour)
- `refreshToken` is long-lived (recommend 7-30 days)
- `user` object should include username at minimum

### 2. Token Refresh Endpoint

**Endpoint**: `POST /api/v1/refreshToken`

**Purpose**: Issue a new access token using a refresh token

**Request**:
```json
{
  "refreshToken": "string (required)"
}
```

**Success Response** (200 OK):
```json
{
  "token": "string (new JWT bearer token)",
  "refreshToken": "string (may be same or rotated)",
  "expiresIn": 3600
}
```

**Error Response** (401 Unauthorized):
```json
{
  "error": "INVALID_TOKEN",
  "message": "Refresh token has expired or is invalid"
}
```

**Notes**:
- Should validate refresh token signature and expiry
- Can optionally rotate refresh token (issue new one each time)
- Return new token with updated expiry
- Reject invalid/expired refresh tokens with 401

### 3. Logout Endpoint

**Endpoint**: `POST /api/v1/logout`

**Purpose**: Invalidate tokens and end session

**Request**:
```
Headers:
  Authorization: Bearer {token}
```

**Success Response** (200 OK):
```json
{
  "success": true
}
```

**Error Response** (401 Unauthorized):
```json
{
  "error": "UNAUTHORIZED",
  "message": "Token is missing or invalid"
}
```

**Notes**:
- Should invalidate the provided token server-side (optional but recommended)
- Can log the logout event for audit trails
- Should succeed even if token already expired

## Authentication Flow Diagram

```
┌─────────────┐
│  LoginPage  │
└──────┬──────┘
       │
       │ Username/Password
       ▼
┌─────────────────────────────────────┐
│ POST /api/v1/login                  │
│ Content-Type: application/json      │
└──────┬──────────────────────────────┘
       │
       ├─ Success (200)
       │  └─ Store token + refreshToken
       │     Set isAuthenticated = true
       │     Redirect to /dashboard
       │
       └─ Failure (401)
          └─ Show error message
             Remain on login page

┌──────────────────────────────────────────┐
│  Authenticated API Request               │
│  GET /api/v1/some-resource               │
│  Authorization: Bearer {token}           │
└──────┬───────────────────────────────────┘
       │
       ├─ Success (200, 201, etc.)
       │  └─ Return response data
       │
       └─ Token Expired (401)
          │
          ├─ POST /api/v1/refreshToken
          │  └─ Success (200)
          │     │─ Store new token
          │     └─ Retry original request
          │
          └─ Failure (401)
             └─ Clear tokens
                Redirect to /login
                Show "Session expired" notice
```

## Token Storage & Security

### Storage Method

The UI stores tokens in the browser's `localStorage`:

```javascript
// Stored as:
localStorage['auth_tokens'] = JSON.stringify({
  token: "eyJhbGc...",
  refreshToken: "eyJhbGc...",
  expiryTime: 1704067200000,  // Timestamp when token expires
  expiresIn: 3600,             // Duration in seconds
  user: { username: "admin", ... }
})
```

### Security Considerations

1. **HTTPS Only**: Always deploy over HTTPS in production
   - Tokens transmitted over HTTP are vulnerable to man-in-the-middle attacks

2. **Secure Token Storage**:
   - localStorage is accessible to any JavaScript (vulnerable to XSS)
   - Recommend adding Content-Security-Policy (CSP) headers
   - Consider HttpOnly cookies for production (requires backend support)

3. **CORS Configuration**:
   - Ensure server sets appropriate CORS headers
   - Allow credentials: `Access-Control-Allow-Credentials: true`
   - Restrict origins in production

4. **Token Rotation**:
   - Optionally rotate refresh tokens on each refresh
   - Invalidate old tokens server-side

5. **Token Validation**:
   - Always validate token signature server-side
   - Check token expiry before using
   - Never trust client-side expiry validation

## Basic Authentication Support

The UI supports optional basic authentication fallback:

```javascript
// When "Use Basic Authentication" checkbox is checked:
axios.post('/api/v1/login', {}, {
  auth: {
    username: "testuser",
    password: "password"
  }
})
```

### Server Implementation

The server should:
1. Accept Authorization header with Basic scheme: `Authorization: Basic base64(username:password)`
2. Decode and validate credentials
3. Return same response as credential-based login
4. Set HttpOnly cookie with session info (optional)

## Request Interceptor Behavior

The UI automatically adds the token to all requests:

```javascript
// Automatic request header injection:
GET /api/v1/dashboard
Authorization: Bearer eyJhbGc...
```

Exception: Login request doesn't include Authorization header (it's the auth endpoint).

## Response Interceptor Behavior

On 401 responses, the interceptor:

1. **If refresh token available**:
   - Queue the failed request
   - Call POST /api/v1/refreshToken
   - On success: Retry original request with new token
   - On failure: Clear tokens, redirect to login

2. **If no refresh token**:
   - Clear authentication state
   - Redirect to login
   - Set `sessionTimeout = true` flag

3. **Concurrent Requests**:
   - Multiple 401s don't trigger multiple refreshes
   - Failed requests queued and retried after refresh completes

## CORS Configuration

Required CORS headers for the UI to function:

```
Access-Control-Allow-Origin: https://yourui.domain.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 86400
```

## Environment Configuration

The UI reads configuration from `public/config.json` at runtime:

```json
{
  "title": "Maps Messaging Admin",
  "version": "1.0.0",
  "buildTime": "2024-01-01T00:00:00Z",
  "apiEndpoint": "/api/v1"
}
```

### Generation During Build

The Maven build can populate config.json using the `process-config.js` script:

```bash
npm run build:prod
```

This reads environment variables:
- `APP_TITLE`: UI title (default: "Maps Messaging Admin")
- `APP_VERSION`: Version string (default: "1.0.0")
- `API_ENDPOINT`: API base URL (default: "/api/v1")

## Session Timeout Handling

### Client-Side Detection

The UI detects token expiry by checking stored `expiryTime`:

```javascript
const now = Date.now()
if (storedToken.expiryTime <= now) {
  // Token expired, redirect to login
}
```

### Recommended Server Timeout

- **Access Token**: 1 hour (3600 seconds)
- **Refresh Token**: 7-30 days
- **Idle Session Timeout**: Configure server-side (recommend 30 mins)

### Idle Timeout Implementation

For idle timeouts, the server should:
1. Track last activity timestamp per session
2. On each request, check if `now - lastActivity > timeout`
3. If yes, invalidate session and return 401
4. Client will refresh or redirect to login

## Error Handling

### Common Error Scenarios

1. **Invalid Credentials** (401):
   - User sees "Invalid username or password"
   - Remains on login page
   - Can retry

2. **Token Expired** (401 on protected route):
   - Auto-refresh triggered
   - If refresh succeeds, request retried transparently
   - If refresh fails, redirect to login with "Session expired" notice

3. **Network Error**:
   - Retry logic (default: 1 retry)
   - User sees error toast/notification
   - Can retry manually

4. **Server Error** (500, 502, etc.):
   - Return to user (not retried)
   - User sees error toast
   - Can retry manually

### Error Response Format

Recommended error response format:

```json
{
  "error": "ERROR_CODE",
  "message": "User-friendly error message",
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/endpoint"
}
```

## Testing Authentication

### Test Credentials

During development, consider creating test accounts:

```
Username: admin
Password: admin123

Username: user
Password: user123
```

### Test Flows

1. **Happy Path**:
   - Login with valid credentials
   - Access protected page
   - Logout

2. **Token Refresh**:
   - Login
   - Wait for token to expire
   - Make API request (triggers refresh)
   - Verify new token is used

3. **Invalid Token**:
   - Login
   - Manually invalidate token in localStorage
   - Make API request
   - Verify redirect to login

4. **Refresh Token Expired**:
   - Login
   - Invalidate refresh token
   - Make request that triggers refresh
   - Verify redirect to login

## Monitoring & Debugging

### Enable Debug Logging

Add to `apiClient.js` for development:

```javascript
apiClient.interceptors.request.use((config) => {
  console.log('[API Request]', config.method, config.url)
  return config
})

apiClient.interceptors.response.use(
  (response) => {
    console.log('[API Response]', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('[API Error]', error.response?.status, error.config.url)
    return Promise.reject(error)
  }
)
```

### Browser DevTools

1. **Application tab**: Check localStorage for tokens
2. **Network tab**: Monitor API requests and responses
3. **Console**: Check for auth errors and warnings

### Server Logs

Monitor server logs for:
- Failed login attempts
- Token validation failures
- Session timeouts
- Rate limiting (if configured)

## Deployment Checklist

- [ ] All three API endpoints implemented (`/api/v1/login`, `/api/v1/refreshToken`, `/api/v1/logout`)
- [ ] HTTPS enabled for production
- [ ] CORS headers configured correctly
- [ ] Token expiry times set appropriately
- [ ] Error responses follow recommended format
- [ ] Idle timeout detection implemented (optional but recommended)
- [ ] Audit logging for authentication events
- [ ] Rate limiting on login endpoint
- [ ] public/config.json generated with correct values
- [ ] CSP headers configured to prevent XSS
- [ ] Testing completed for all error scenarios

## Troubleshooting

### "Session expired" on every request

**Cause**: Token expiry time calculated incorrectly
**Solution**: Verify `expiryTime = now + (expiresIn * 1000)` calculation

### 401 loop / infinite refresh

**Cause**: Refresh token also expired or always invalid
**Solution**: 
- Extend refresh token lifetime
- Add validation to reject invalid tokens immediately
- Check server logs for refresh failures

### CORS errors in browser

**Cause**: Missing CORS headers
**Solution**:
- Add `Access-Control-Allow-Credentials: true`
- Allow `Authorization` in `Access-Control-Allow-Headers`
- Set correct `Access-Control-Allow-Origin`

### Logout doesn't clear session

**Cause**: Token stored in multiple places or not cleared completely
**Solution**:
- Clear localStorage `auth_tokens` entry
- Clear any session cookies
- Verify state updated in AuthContext

## References

- [JWT Best Practices (RFC 8949)](https://datatracker.ietf.org/doc/html/rfc7519)
- [OAuth 2.0 Security Best Practices](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
