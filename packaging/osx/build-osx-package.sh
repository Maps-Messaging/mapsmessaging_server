#!/bin/bash

#
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

set -e

VERSION="${1:-3.3.7-SNAPSHOT}"
APP_NAME="${2:-MapsMessaging}"

BASE_DIR="$(pwd)"
STAGING_DIR="$BASE_DIR/build/staging"
RUNTIME_DIR="$BASE_DIR/build/runtime"
OUTPUT_DIR="$BASE_DIR/out/mac"

# Clean
rm -rf "$STAGING_DIR" "$RUNTIME_DIR" "$OUTPUT_DIR"
mkdir -p "$STAGING_DIR"

# Run jdk copy
bash "$BASE_DIR/jdk_copy.sh"

# Run jpackage script
bash "$BASE_DIR/jpackage_script.sh" \
  "$VERSION" \
  "$APP_NAME" \
  "$BASE_DIR" \
  "$OUTPUT_DIR" \
  "$RUNTIME_DIR" \
  "$STAGING_DIR"
