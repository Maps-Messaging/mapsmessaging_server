#!/bin/bash

# Schema Inspection Utility for Maps Messaging Server
# This script helps inspect generated JSON schemas

SCHEMA_DIR="${MAPS_HOME:-.}/schemas/cache"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

show_usage() {
    cat << EOF
Schema Inspection Utility

Usage: $0 [COMMAND] [OPTIONS]

Commands:
    list                List all generated schemas
    view <name>         View a specific schema (pretty printed)
    count               Count total schemas
    search <term>       Search for a term in all schemas
    validate <name>     Validate a schema file
    stats               Show schema statistics
    clean               Remove all cached schemas
    help                Show this help message

Examples:
    $0 list
    $0 view MessageDaemonConfigDTO
    $0 search delayedPublishInterval
    $0 stats
    $0 clean

Environment:
    MAPS_HOME           Base directory for schema cache (default: current directory)

Schema Location: ${SCHEMA_DIR}
EOF
}

check_schema_dir() {
    if [ ! -d "$SCHEMA_DIR" ]; then
        echo -e "${RED}Error: Schema directory not found: ${SCHEMA_DIR}${NC}"
        echo "Run the application or tests first to generate schemas."
        exit 1
    fi
}

list_schemas() {
    check_schema_dir
    echo -e "${GREEN}Generated JSON Schemas:${NC}"
    echo "Location: $SCHEMA_DIR"
    echo ""

    if command -v tree &> /dev/null; then
        tree -h "$SCHEMA_DIR"
    else
        ls -lh "$SCHEMA_DIR"/*.schema.json 2>/dev/null || echo "No schemas found"
    fi
}

view_schema() {
    check_schema_dir
    local schema_name="$1"

    if [ -z "$schema_name" ]; then
        echo -e "${RED}Error: Please specify a schema name${NC}"
        echo "Usage: $0 view <name>"
        echo "Example: $0 view MessageDaemonConfigDTO"
        exit 1
    fi

    # Add .schema.json if not present
    if [[ ! "$schema_name" =~ \.schema\.json$ ]]; then
        schema_name="${schema_name}.schema.json"
    fi

    local schema_file="$SCHEMA_DIR/$schema_name"

    if [ ! -f "$schema_file" ]; then
        echo -e "${RED}Error: Schema file not found: ${schema_file}${NC}"
        echo ""
        echo "Available schemas:"
        ls "$SCHEMA_DIR"/*.schema.json 2>/dev/null | xargs -n1 basename
        exit 1
    fi

    echo -e "${GREEN}Schema: ${schema_name}${NC}"
    echo -e "${YELLOW}Location: ${schema_file}${NC}"
    echo ""

    if command -v jq &> /dev/null; then
        jq . "$schema_file"
    else
        cat "$schema_file"
        echo ""
        echo -e "${YELLOW}Tip: Install 'jq' for pretty-printed JSON${NC}"
    fi
}

count_schemas() {
    check_schema_dir
    local count=$(ls "$SCHEMA_DIR"/*.schema.json 2>/dev/null | wc -l)
    echo -e "${GREEN}Total Schemas: ${count}${NC}"
}

search_schemas() {
    check_schema_dir
    local search_term="$1"

    if [ -z "$search_term" ]; then
        echo -e "${RED}Error: Please specify a search term${NC}"
        echo "Usage: $0 search <term>"
        exit 1
    fi

    echo -e "${GREEN}Searching for '${search_term}' in all schemas:${NC}"
    echo ""

    grep -r --color=always "$search_term" "$SCHEMA_DIR"/*.schema.json 2>/dev/null || \
        echo "No matches found"
}

validate_schema() {
    check_schema_dir
    local schema_name="$1"

    if [ -z "$schema_name" ]; then
        echo -e "${RED}Error: Please specify a schema name${NC}"
        exit 1
    fi

    # Add .schema.json if not present
    if [[ ! "$schema_name" =~ \.schema\.json$ ]]; then
        schema_name="${schema_name}.schema.json"
    fi

    local schema_file="$SCHEMA_DIR/$schema_name"

    if [ ! -f "$schema_file" ]; then
        echo -e "${RED}Error: Schema file not found: ${schema_file}${NC}"
        exit 1
    fi

    echo -e "${GREEN}Validating schema: ${schema_name}${NC}"

    if command -v jq &> /dev/null; then
        if jq empty "$schema_file" 2>/dev/null; then
            echo -e "${GREEN}✓ Valid JSON${NC}"

            # Check for required JSON Schema fields
            if jq -e '."$schema"' "$schema_file" &>/dev/null; then
                echo -e "${GREEN}✓ Has \$schema field${NC}"
            else
                echo -e "${YELLOW}⚠ Missing \$schema field${NC}"
            fi

            if jq -e '.type' "$schema_file" &>/dev/null; then
                echo -e "${GREEN}✓ Has type field${NC}"
            else
                echo -e "${YELLOW}⚠ Missing type field${NC}"
            fi

            if jq -e '.properties' "$schema_file" &>/dev/null; then
                local prop_count=$(jq '.properties | length' "$schema_file")
                echo -e "${GREEN}✓ Has properties (${prop_count} fields)${NC}"
            else
                echo -e "${YELLOW}⚠ Missing properties field${NC}"
            fi
        else
            echo -e "${RED}✗ Invalid JSON${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}Install 'jq' for detailed validation${NC}"
        if python3 -m json.tool "$schema_file" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Valid JSON (basic check)${NC}"
        else
            echo -e "${RED}✗ Invalid JSON${NC}"
            exit 1
        fi
    fi
}

show_stats() {
    check_schema_dir

    echo -e "${GREEN}Schema Statistics:${NC}"
    echo ""

    local count=$(ls "$SCHEMA_DIR"/*.schema.json 2>/dev/null | wc -l)
    echo "Total Schemas: $count"

    local total_size=$(du -sh "$SCHEMA_DIR" 2>/dev/null | cut -f1)
    echo "Total Size: $total_size"

    echo ""
    echo "Schemas by size:"
    ls -lhS "$SCHEMA_DIR"/*.schema.json 2>/dev/null | head -5 | awk '{print $9, "-", $5}'

    if command -v jq &> /dev/null; then
        echo ""
        echo "Schemas by property count:"
        for file in "$SCHEMA_DIR"/*.schema.json; do
            if [ -f "$file" ]; then
                local prop_count=$(jq '.properties | length' "$file" 2>/dev/null || echo 0)
                echo "$(basename "$file"): $prop_count properties"
            fi
        done | sort -t: -k2 -nr | head -5
    fi
}

clean_cache() {
    check_schema_dir

    echo -e "${YELLOW}This will delete all cached schemas in: ${SCHEMA_DIR}${NC}"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -f "$SCHEMA_DIR"/*.schema.json
        echo -e "${GREEN}Schema cache cleared${NC}"
    else
        echo "Cancelled"
    fi
}

# Main command dispatcher
case "$1" in
    list)
        list_schemas
        ;;
    view)
        view_schema "$2"
        ;;
    count)
        count_schemas
        ;;
    search)
        search_schemas "$2"
        ;;
    validate)
        validate_schema "$2"
        ;;
    stats)
        show_stats
        ;;
    clean)
        clean_cache
        ;;
    help|--help|-h)
        show_usage
        ;;
    *)
        show_usage
        exit 1
        ;;
esac
