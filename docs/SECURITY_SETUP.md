# MAPS Messaging Server - Security & Authentication Setup

This guide explains how to configure authentication and security for the MAPS Messaging server Docker instance to work with the admin-ui login system.

## Overview

The MAPS server supports multiple authentication methods and provides a comprehensive REST API for management. This setup enables local file-based authentication with default admin and user accounts.

## Quick Start

### 1. Deploy with Docker Compose

```bash
# Start the MAPS server with authentication enabled
docker-compose up -d

# Wait for the server to start (approximately 30-60 seconds)
docker-compose logs -f

# Once running, setup the default admin user
./docker-config/setup-admin.sh
```

### 2. Login to Admin UI

- **URL:** http://localhost:8080
- **Admin Credentials:** admin / admin123
- **API Documentation:** http://localhost:8080/swagger-ui/index.html

## Configuration Files

### AuthManager.yaml

Controls the main authentication system:

```yaml
AuthManager:
  authenticationEnabled: true  # Enable authentication
  authorizationEnabled: false   # Disable authorization (for now)
  config:
    identityProvider: "Encrypted-Auth"
    passwordHandler: "EncryptedPasswordCipher"
    configDirectory: "{{MAPS_DATA}}/.security"
```

**Key Settings:**
- `authenticationEnabled: true` - Enables authentication for all protocols
- `configDirectory` - Where user database and security files are stored
- Uses encrypted password storage

### RestApi.yaml

Controls REST API authentication:

```yaml
RestApi:
  enabled: true
  enableAuthentication: true  # Enable REST API authentication
  port: 8080
  enableSwaggerUI: true
```

**Key Settings:**
- `enableAuthentication: true` - Requires authentication for all REST endpoints
- Open endpoints: `/api/v1/login`, `/api/v1/ping`, `/health`, `/openapi.json`

### SecurityManager.yaml & jaasAuth.config

JAAS configuration for username/password authentication:

```properties
UsernamePasswordLoginModule{
  io.mapsmessaging.security.jaas.IdentityLoginModule Required
                                                      debug=false
                                                      siteWide="system";
};
```

## Default Users

The system creates default users on first boot:

| Username | Password | Role | Purpose |
|----------|----------|------|---------|
| admin | admin123 | admin | Full administrative access |
| user | user123 | user | Basic user access |

## API Endpoints

### Authentication Endpoints

- **POST** `/api/v1/login` - User login (no auth required)
- **POST** `/api/v1/logout` - User logout
- **GET** `/api/v1/session` - Get current session info
- **GET** `/api/v1/refreshToken` - Refresh JWT token

### User Management (Admin only)

- **GET** `/api/v1/auth/users` - List all users
- **POST** `/api/v1/auth/users` - Create new user
- **PUT** `/api/v1/auth/users/{username}` - Update user
- **DELETE** `/api/v1/auth/users/{username}` - Delete user

### Login Request/Response

**Request:**
```json
{
  "username": "admin",
  "password": "admin123",
  "persistent": false,
  "longLived": false
}
```

**Response:**
```json
{
  "status": "Success",
  "username": "admin",
  "sessionExpiry": 900,
  "sessionId": "...",
  "uuid": "...",
  "authorised": true,
  "roles": ["admin"],
  "groups": ["admin"]
}
```

## Security Architecture

### Authentication Flow

1. **Login Request:** Client sends credentials to `/api/v1/login`
2. **Validation:** Server validates against encrypted user store
3. **Session Creation:** Creates HTTP session with JWT token
4. **Cookie Storage:** JWT token stored in secure cookie
5. **Request Validation:** Subsequent requests validated via JWT token

### Token Management

- **JWT Tokens:** Used for session authentication
- **Cookie Storage:** Secure HTTP-only cookies
- **Token Lifetime:** 15 minutes (default), 7 days for long-lived sessions
- **Auto-refresh:** Tokens can be refreshed before expiry

### User Storage

- **Database:** MapDB file stored in `/data/.security/.auth.db`
- **Encryption:** Passwords encrypted using EncryptedPasswordCipher
- **Persistence:** Data persists across container restarts via Docker volume

## Production Security Guidelines

### 1. Change Default Passwords

```bash
# Change admin password via API
curl -X PUT "http://localhost:8080/api/v1/auth/users/admin" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"password": "new-secure-password"}'
```

### 2. Enable HTTPS

Update `RestApi.yaml`:

```yaml
RestApi:
  tls:
    keyStore:
      path: /path/to/keystore.jks
      passphrase: your-keystore-password
```

### 3. Network Security

- Use Docker networks to isolate the container
- Only expose necessary ports
- Consider reverse proxy with SSL termination

### 4. Access Control

- Enable authorization (`authorizationEnabled: true`)
- Configure role-based access control
- Use network policies for additional security

## Troubleshooting

### Common Issues

1. **Login Fails with 401 Unauthorized**
   - Check if authentication is enabled in RestApi.yaml
   - Verify user exists in database
   - Check container logs for errors

2. **Users Not Created**
   - Run `./docker-config/setup-admin.sh` script
   - Check if `/data/.security/.auth.db` exists
   - Verify AuthManager configuration

3. **API Returns 403 Forbidden**
   - User may not have required permissions
   - Check user roles and groups
   - Verify authorization settings

### Debug Logging

Enable debug logging in `jaasAuth.config`:

```properties
UsernamePasswordLoginModule{
  io.mapsmessaging.security.jaas.IdentityLoginModule Required
                                                      debug=true
                                                      siteWide="system";
};
```

### Container Logs

```bash
# View container logs
docker-compose logs maps-messaging

# Follow logs in real-time
docker-compose logs -f maps-messaging
```

## Configuration Customization

### Adding Custom Authentication Providers

1. Implement `IdentityLookupFactory` interface
2. Update `AuthManager.yaml` with new provider
3. Add JAAS configuration for new provider

### Custom User Roles

1. Define roles in `AuthManager.java`
2. Update user management APIs
3. Configure authorization policies

### External Authentication

For LDAP, OAuth, or other external providers:

1. Update `identityProvider` in `AuthManager.yaml`
2. Configure external provider settings
3. Update JAAS configuration accordingly

## Backup and Recovery

### Backup User Database

```bash
# Backup authentication database
docker cp maps-messaging-server:/data/.security/.auth.db ./auth-backup.db

# Restore authentication database
docker cp ./auth-backup.db maps-messaging-server:/data/.security/.auth.db
```

### Export Users

```bash
# Export all users to JSON
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/auth/users" > users-backup.json
```

## Support

For additional support:

1. Check container logs: `docker-compose logs`
2. Review API documentation: http://localhost:8080/swagger-ui/index.html
3. Verify configuration files in `docker-config/` directory
4. Test authentication via curl commands

## Security Best Practices

1. **Regular Updates:** Keep Docker image updated
2. **Strong Passwords:** Use complex passwords for all accounts
3. **Network Isolation:** Use Docker networks and firewalls
4. **Monitoring:** Monitor authentication logs for suspicious activity
5. **Backup:** Regular backup of user database and configurations
6. **Testing:** Test authentication and authorization thoroughly