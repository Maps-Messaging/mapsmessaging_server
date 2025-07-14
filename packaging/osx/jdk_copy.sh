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

JAVA_PATH=$(which java)
JDK_HOME="$(cd "$(dirname "$JAVA_PATH")/.." && pwd)"
RUNTIME_DIR="build/runtime"

# Clear existing runtime directory
rm -rf "$RUNTIME_DIR"

# Copy full JDK
echo "Copying JDK from $JDK_HOME to $RUNTIME_DIR"
mkdir -p "$RUNTIME_DIR"
rsync -a --no-compress --exclude '.DS_Store' "$JDK_HOME/" "$RUNTIME_DIR/" 2>/dev/null || cp -Rp "$JDK_HOME/." "$RUNTIME_DIR/"

