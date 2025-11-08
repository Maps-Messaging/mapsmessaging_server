#!/bin/bash

# MAPS Messaging Server - Admin User Setup Script
# This script creates the default admin user for the MAPS server

set -e

echo "Setting up MAPS Messaging Server admin user..."

# Check if container is running
if ! docker ps | grep -q maps-messaging-server; then
    echo "Error: MAPS server container is not running."
    echo "Please start it first with: docker-compose up -d"
    exit 1
fi

# Wait for server to be ready
echo "Waiting for MAPS server to be ready..."
until curl -f http://localhost:8080/api/v1/ping >/dev/null 2>&1; do
    echo "Server not ready, waiting 5 seconds..."
    sleep 5
done

echo "Server is ready. Creating admin user..."

# Create admin user using the REST API
ADMIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/auth/users" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "admin",
        "password": "admin123",
        "enabled": true,
        "roles": ["admin"]
    }')

if echo "$ADMIN_RESPONSE" | grep -q "error\|Error"; then
    echo "Admin user might already exist or there was an error:"
    echo "$ADMIN_RESPONSE"
else
    echo "Admin user created successfully!"
fi

# Create regular user
USER_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/auth/users" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "user",
        "password": "user123",
        "enabled": true,
        "roles": ["user"]
    }')

if echo "$USER_RESPONSE" | grep -q "error\|Error"; then
    echo "Regular user might already exist or there was an error:"
    echo "$USER_RESPONSE"
else
    echo "Regular user created successfully!"
fi

echo ""
echo "=== MAPS Server Authentication Setup Complete ==="
echo ""
echo "Default Login Credentials:"
echo "Admin User:"
echo "  Username: admin"
echo "  Password: admin123"
echo ""
echo "Regular User:"
echo "  Username: user"
echo "  Password: user123"
echo ""
echo "Access the Admin UI at: http://localhost:8080"
echo "API Documentation at: http://localhost:8080/swagger-ui/index.html"
echo ""
echo "IMPORTANT: Change these default passwords in production!"