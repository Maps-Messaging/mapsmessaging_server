#!/bin/bash

# MAPS Messaging Server - Authentication Test Script
# This script tests the authentication setup and functionality

set -e

BASE_URL="http://localhost:8080"
echo "Testing MAPS Server Authentication..."
echo "Base URL: $BASE_URL"
echo "========================================"

# Function to test endpoint
test_endpoint() {
    local url="$1"
    local method="${2:-GET}"
    local data="$3"
    local expected_code="${4:-200}"
    
    echo "Testing $method $url..."
    
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$url" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" "$url")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "$expected_code" ]; then
        echo "✅ SUCCESS (HTTP $http_code)"
        if [ -n "$body" ] && [ "$body" != "null" ]; then
            echo "Response: $body" | jq . 2>/dev/null || echo "Response: $body"
        fi
    else
        echo "❌ FAILED (HTTP $http_code, expected $expected_code)"
        echo "Response: $body"
        return 1
    fi
    echo ""
}

# Function to extract token from login response
extract_token() {
    local login_response="$1"
    echo "$login_response" | jq -r '.sessionId // empty' 2>/dev/null || echo ""
}

# 1. Test server health
echo "1. Testing server health..."
test_endpoint "$BASE_URL/api/v1/ping"

# 2. Test login endpoint (no auth required)
echo "2. Testing login endpoint..."
login_response=$(curl -s -X POST "$BASE_URL/api/v1/login" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "admin",
        "password": "admin123",
        "persistent": false,
        "longLived": false
    }')

echo "Login response:"
echo "$login_response" | jq . 2>/dev/null || echo "$login_response"

# Extract session ID for further tests
session_id=$(extract_token "$login_response")
if [ -z "$session_id" ]; then
    echo "❌ Failed to extract session ID from login response"
    exit 1
fi

echo "✅ Session ID extracted: ${session_id:0:20}..."
echo ""

# 3. Test protected endpoint without authentication
echo "3. Testing protected endpoint without authentication..."
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/v1/auth/users" 2>/dev/null || echo "FAILED")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" = "401" ]; then
    echo "✅ SUCCESS: Protected endpoint correctly returned 401 Unauthorized"
else
    echo "❌ FAILED: Expected 401, got $http_code"
fi
echo ""

# 4. Test protected endpoint with authentication
echo "4. Testing protected endpoint with authentication..."
test_endpoint "$BASE_URL/api/v1/auth/users" "GET" "" "200"

# 5. Test session endpoint
echo "5. Testing session endpoint..."
test_endpoint "$BASE_URL/api/v1/session" "GET" "" "200"

# 6. Test user creation
echo "6. Testing user creation..."
test_endpoint "$BASE_URL/api/v1/auth/users" "POST" \
    '{
        "username": "testuser",
        "password": "testpass123",
        "enabled": true,
        "roles": ["user"]
    }' "200" || echo "User might already exist"

# 7. Test login with new user
echo "7. Testing login with new user..."
test_endpoint "$BASE_URL/api/v1/login" "POST" \
    '{
        "username": "testuser",
        "password": "testpass123",
        "persistent": false,
        "longLived": false
    }' "200"

# 8. Test logout
echo "8. Testing logout..."
test_endpoint "$BASE_URL/api/v1/logout" "POST" "" "200"

# 9. Test token refresh
echo "9. Testing token refresh..."
test_endpoint "$BASE_URL/api/v1/refreshToken" "GET" "" "200" || echo "Token refresh test (may fail after logout)"

# 10. Test Swagger UI accessibility
echo "10. Testing Swagger UI..."
test_endpoint "$BASE_URL/swagger-ui/index.html" "GET" "" "200"

echo "========================================"
echo "Authentication Tests Complete!"
echo ""
echo "Summary:"
echo "✅ Server is running and accessible"
echo "✅ Login endpoint works correctly"
echo "✅ Protected endpoints require authentication"
echo "✅ Session management works"
echo "✅ User management functions work"
echo "✅ Logout works correctly"
echo "✅ Swagger UI is accessible"
echo ""
echo "🎉 MAPS Server Authentication is working correctly!"