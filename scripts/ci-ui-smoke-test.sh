#!/bin/bash

# CI smoke test for Maps Messaging Admin UI
# This script tests that the UI can be built successfully

set -e

echo "Starting UI smoke test..."

# Check if Node.js is available
if ! command -v node &> /dev/null; then
    echo "Node.js not found. Installing Node.js LTS..."
    # Install Node.js using apt (Ubuntu/Debian)
    curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
    sudo apt-get install -y nodejs
fi

NODE_VERSION=$(node --version)
echo "Using Node.js $NODE_VERSION"

# Navigate to UI directory
cd ui/maps-admin-ui

# Clean any previous builds
rm -rf node_modules dist package-lock.json

# Install dependencies
echo "Installing dependencies..."
npm install

# Run build in production mode
echo "Building UI in production mode..."
npm run build:prod

# Check if build output exists
if [ ! -d "../../../target/classes/html/admin" ]; then
    echo "ERROR: Build output directory not found!"
    exit 1
fi

# Check if key files exist
if [ ! -f "../../../target/classes/html/admin/index.html" ]; then
    echo "ERROR: index.html not found in build output!"
    exit 1
fi

echo "UI smoke test passed successfully!"
echo "Build output located in: ../../../target/classes/html/admin/"