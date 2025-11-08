# MAPS Messaging Server - Authentication Setup

This repository contains the configuration files and documentation to enable authentication for the MAPS Messaging server Docker instance.

## 🚀 Quick Start

```bash
# 1. Start the MAPS server with authentication enabled
docker-compose up -d

# 2. Wait for the server to start (30-60 seconds)
docker-compose logs -f

# 3. Setup default admin user
./docker-config/setup-admin.sh

# 4. Test the authentication setup
./docker-config/test-auth.sh

# 5. Access the admin UI
# URL: http://localhost:8080
# Username: admin
# Password: admin123
```

## 📁 File Structure

```
.
├── docker-compose.yml              # Docker Compose configuration
├── docker-config/                  # Authentication configuration files
│   ├── AuthManager.yaml           # Main authentication configuration
│   ├── SecurityManager.yaml       # JAAS security configuration  
│   ├── RestApi.yaml               # REST API authentication settings
│   ├── jaasAuth.config            # JAAS login module configuration
│   ├── setup-admin.sh             # Script to create default users
│   └── test-auth.sh               # Script to test authentication
├── docs/
│   └── SECURITY_SETUP.md          # Comprehensive security documentation
└── README_AUTHENTICATION.md       # This file
```

## 🔐 Default Credentials

| Username | Password | Role | Access |
|----------|----------|------|--------|
| admin | admin123 | admin | Full administrative access |
| user | user123 | user | Basic user access |

**⚠️ IMPORTANT:** Change these default passwords in production environments!

## 🛠️ Configuration Details

### Key Changes Made

1. **Enabled REST API Authentication** (`RestApi.yaml`)
   - `enableAuthentication: true`
   - All endpoints except `/api/v1/login` require authentication

2. **Configured Local User Store** (`AuthManager.yaml`)
   - Uses "Encrypted-Auth" identity provider
   - Passwords encrypted and stored in MapDB
   - Persistent storage in `/data/.security/`

3. **JAAS Authentication** (`SecurityManager.yaml`, `jaasAuth.config`)
   - Username/password authentication
   - Integration with Java security architecture

4. **Docker Integration** (`docker-compose.yml`)
   - Mounts configuration files
   - Persistent data volume
   - Health checks and proper networking

## 🔌 Available Endpoints

### Public Endpoints (No Authentication Required)
- `GET /api/v1/ping` - Health check
- `GET /health` - Health check
- `POST /api/v1/login` - User login
- `GET /openapi.json` - API documentation

### Protected Endpoints (Authentication Required)
- `GET /api/v1/auth/users` - List users (admin only)
- `POST /api/v1/auth/users` - Create user (admin only)
- `GET /api/v1/session` - Current session info
- `POST /api/v1/logout` - User logout
- All other REST API endpoints

## 🧪 Testing

Run the test script to verify authentication is working:

```bash
./docker-config/test-auth.sh
```

This script will:
- Test server health
- Verify login functionality
- Test protected endpoints
- Verify user management
- Test logout functionality

## 📚 Documentation

For detailed information, see:
- [`docs/SECURITY_SETUP.md`](docs/SECURITY_SETUP.md) - Comprehensive security guide
- [`docker-config/`](docker-config/) - Configuration files with inline comments

## 🔧 Customization

### Adding New Users

```bash
# Via API
curl -X POST "http://localhost:8080/api/v1/auth/users" \
  -H "Content-Type: application/json" \
  -d '{
      "username": "newuser",
      "password": "newpass123",
      "enabled": true,
      "roles": ["user"]
  }'

# Via setup script (modify and run)
./docker-config/setup-admin.sh
```

### Changing Passwords

```bash
curl -X PUT "http://localhost:8080/api/v1/auth/users/admin" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"password": "new-secure-password"}'
```

### Enabling HTTPS

Update `docker-config/RestApi.yaml` with SSL certificate configuration and mount the certificates in `docker-compose.yml`.

## 🚨 Production Deployment

### Security Checklist

- [ ] Change default passwords
- [ ] Enable HTTPS/TLS
- [ ] Configure proper network isolation
- [ ] Enable authorization (`authorizationEnabled: true`)
- [ ] Set up backup procedures
- [ ] Configure monitoring and logging
- [ ] Review user roles and permissions

### Environment Variables

Key environment variables for production:

```yaml
environment:
  - MAPS_HOME=/opt/maps
  - MAPS_DATA=/data
  - JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC
  - CONSUL_URL= # Optional: for distributed configuration
```

## 🐛 Troubleshooting

### Common Issues

1. **401 Unauthorized on Login**
   - Check if server is fully started
   - Verify configuration files are mounted correctly
   - Check container logs: `docker-compose logs`

2. **Users Not Created**
   - Run setup script: `./docker-config/setup-admin.sh`
   - Check if database file exists: `docker exec maps-messaging-server ls -la /data/.security/`

3. **Authentication Disabled**
   - Verify `enableAuthentication: true` in RestApi.yaml
   - Check `authenticationEnabled: true` in AuthManager.yaml

### Debug Commands

```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs -f maps-messaging

# Access container shell
docker-compose exec maps-messaging /bin/sh

# Check configuration files
docker-compose exec maps-messaging cat /opt/maps/conf/RestApi.yaml
```

## 📞 Support

For additional help:

1. Check the [comprehensive documentation](docs/SECURITY_SETUP.md)
2. Review the test script output
3. Check container logs for errors
4. Verify API documentation at http://localhost:8080/swagger-ui/index.html

## 🔄 Updates

To update the configuration:

1. Modify files in `docker-config/`
2. Restart the container: `docker-compose restart`
3. Test with the test script: `./docker-config/test-auth.sh`

---

**Note:** This configuration enables basic local authentication. For enterprise deployments, consider integrating with LDAP, OAuth, or other external authentication providers.