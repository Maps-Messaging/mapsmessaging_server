#!/bin/bash

# OpenAPI Client Generation Script
# This script automates the process of downloading the OpenAPI spec and generating the client

set -e

echo "🚀 Starting OpenAPI client generation..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    print_error "package.json not found. Please run this script from the ui/maps-admin-ui directory."
    exit 1
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    print_warning "node_modules not found. Installing dependencies..."
    npm install
fi

# Download OpenAPI spec
print_status "Downloading OpenAPI specification from http://cloud.kritikal.org:8080/openapi.json..."

if curl -f -o openapi/openapi.json http://cloud.kritikal.org:8080/openapi.json; then
    print_status "✅ OpenAPI spec downloaded successfully"
else
    print_error "❌ Failed to download OpenAPI spec. Please check if the server is accessible."
    exit 1
fi

# Generate TypeScript client
print_status "Generating TypeScript client..."

if npx openapi-generator-cli generate -i openapi/openapi.json -g typescript-fetch -c openapi-generator.json -o src/api/generated; then
    print_status "✅ TypeScript client generated successfully"
else
    print_error "❌ Failed to generate TypeScript client"
    exit 1
fi

# Run smoke tests to validate generated code
print_status "Running smoke tests to validate generated code..."

if npm run test:smoke; then
    print_status "✅ Smoke tests passed"
else
    print_warning "⚠️  Smoke tests failed, but generation completed. Please check the generated code."
fi

# Run type checking
print_status "Running type checking..."

if npm run type-check; then
    print_status "✅ Type checking passed"
else
    print_warning "⚠️  Type checking failed. Please review the generated code for issues."
fi

print_status "🎉 OpenAPI client generation completed successfully!"
print_status "Generated files are in src/api/generated/"
print_status "You can now use the generated APIs in your React components."

echo ""
echo "Next steps:"
echo "1. Review the generated code in src/api/generated/"
echo "2. Import and use the APIs in your components"
echo "3. Run 'npm run dev' to start the development server"
echo "4. Check the example component for usage patterns"