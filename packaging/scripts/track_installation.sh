#!/bin/bash

#
#  Copyright [ 2020 - 2024 ] Matthew Buckton
#  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
#
#  Licensed under the Apache License, Version 2.0 with the Commons Clause
#  (the "License"); you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at:
#
#      http://www.apache.org/licenses/LICENSE-2.0
#      https://commonsclause.com/
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# PostHog installation tracking script
# Usage: track_installation.sh <package_type> <version> [distro] [arch]

set -e

# Load PostHog configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "${SCRIPT_DIR}/posthog_config.sh" ]; then
    source "${SCRIPT_DIR}/posthog_config.sh"
fi

# Configuration fallbacks
POSTHOG_API_URL="${POSTHOG_API_URL:-https://eu.i.posthog.com/capture/}"
POSTHOG_API_KEY="${POSTHOG_API_KEY:-}"
POSTHOG_PROJECT_ID="${POSTHOG_PROJECT_ID:-45683}"

# Default values
PACKAGE_TYPE="${1:-unknown}"
VERSION="${2:-unknown}"
DISTRO="${3:-unknown}"
ARCH="${4:-unknown}"

# Generate a unique installation ID (based on hostname + timestamp)
INSTALLATION_ID=$(echo "${HOSTNAME:-$(hostname)}-$(date +%s)" | sha256sum | cut -d' ' -f1)

# Create the JSON payload for PostHog
JSON_PAYLOAD=$(cat <<EOF
{
  "api_key": "${POSTHOG_API_KEY}",
  "event": "maps_installation",
  "distinct_id": "${INSTALLATION_ID}",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
  "properties": {
    "package_type": "${PACKAGE_TYPE}",
    "version": "${VERSION}",
    "distro": "${DISTRO}",
    "arch": "${ARCH}",
    "project_id": "${POSTHOG_PROJECT_ID}",
    "hostname": "${HOSTNAME:-$(hostname)}",
    "os": "$(uname -s)",
    "os_version": "$(uname -r)",
    "installation_timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
    "\$lib": "maps-packaging",
    "\$lib_version": "${VERSION}"
  }
}
EOF
)

# Function to send the event to PostHog
send_to_posthog() {
    # Check if tracking is enabled
    if [ "${POSTHOG_TRACKING_ENABLED:-true}" != "true" ]; then
        echo "PostHog tracking disabled, skipping installation tracking"
        return 0
    fi

    if [ -z "${POSTHOG_API_KEY}" ] || [ "${POSTHOG_API_KEY}" = "phc_" ]; then
        echo "PostHog API key not configured, skipping installation tracking"
        return 0
    fi

    # Check if curl is available
    if ! command -v curl >/dev/null 2>&1; then
        echo "curl not available, skipping installation tracking"
        return 0
    fi

    # Send the event (non-blocking)
    (
        curl -s -X POST \
            -H "Content-Type: application/json" \
            -d "${JSON_PAYLOAD}" \
            "${POSTHOG_API_URL}" \
            --max-time "${POSTHOG_TIMEOUT:-10}" \
            --connect-timeout 5 \
            --retry "${POSTHOG_RETRY_COUNT:-2}" \
            --retry-delay 1 \
            >/dev/null 2>&1 || true
    ) &
    
    echo "Installation tracking event sent to PostHog"
}

# Send the tracking event
send_to_posthog

exit 0
