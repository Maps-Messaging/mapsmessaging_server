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

# PostHog Configuration
# This file should be sourced by the tracking scripts to set up PostHog configuration

# PostHog API Key - Set this environment variable when building packages
# Example: export POSTHOG_API_KEY="phc_your_api_key_here"
export POSTHOG_API_KEY="${POSTHOG_API_KEY:-}"

# PostHog Project ID (optional, for additional validation)
export POSTHOG_PROJECT_ID="${POSTHOG_PROJECT_ID:-45683}"

# Enable/disable tracking (default: enabled unless in CI/CD)
if [ "${CI:-}" = "true" ] || [ "${BUILDKITE:-}" = "true" ] || [ "${GITHUB_ACTIONS:-}" = "true" ]; then
    export POSTHOG_TRACKING_ENABLED="false"
else
    export POSTHOG_TRACKING_ENABLED="${POSTHOG_TRACKING_ENABLED:-true}"
fi

# PostHog API URL
export POSTHOG_API_URL="${POSTHOG_API_URL:-https://eu.i.posthog.com/capture/}"

# Additional configuration
export POSTHOG_TIMEOUT="${POSTHOG_TIMEOUT:-10}"
export POSTHOG_RETRY_COUNT="${POSTHOG_RETRY_COUNT:-2}"
