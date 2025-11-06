#!/bin/bash

# Pre-commit hook to check if OpenAPI client is up to date
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

print_status "Checking if OpenAPI client is up to date..."

# Try to download the latest spec
if curl -f -s -o openapi/openapi.json.new http://cloud.kritikal.org:8080/openapi.json; then
    # Compare with existing spec
    if [ -f "openapi/openapi.json" ]; then
        if ! diff -q openapi/openapi.json openapi/openapi.json.new > /dev/null; then
            print_warning "OpenAPI specification has changed"
            mv openapi/openapi.json.new openapi/openapi.json
            
            # Regenerate the client
            print_status "Regenerating API client..."
            npx openapi-generator-cli generate -i openapi/openapi.json -g typescript-fetch -c openapi-generator.json -o src/api/generated
            
            print_error "❌ OpenAPI client was out of date and has been regenerated."
            print_error "Please review the changes and stage them before committing."
            exit 1
        else
            rm openapi/openapi.json.new
            print_status "✅ OpenAPI specification is up to date"
        fi
    else
        mv openapi/openapi.json.new openapi/openapi.json
        print_status "Downloaded OpenAPI specification for the first time"
        
        # Generate the client
        print_status "Generating API client..."
        npx openapi-generator-cli generate -i openapi/openapi.json -g typescript-fetch -c openapi-generator.json -o src/api/generated
        
        print_error "❌ OpenAPI client has been generated for the first time."
        print_error "Please review the changes and stage them before committing."
        exit 1
    fi
else
    if [ ! -f "openapi/openapi.json" ]; then
        print_warning "Could not download OpenAPI spec and no cached version found"
        print_warning "Skipping OpenAPI client check"
        exit 0
    else
        print_warning "Could not download latest OpenAPI spec, using cached version"
        print_status "✅ Using cached OpenAPI specification"
    fi
fi

print_status "✅ OpenAPI client check passed"