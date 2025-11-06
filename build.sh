#!/bin/bash

# Copyright [ 2020 - 2024 ] [Matthew Buckton]
# Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Check for UI build flag
BUILD_UI=false
if [[ "$1" == "--with-ui" ]] || [[ "$1" == "-ui" ]]; then
    BUILD_UI=true
    shift
fi

# Check if Node.js is available for UI build
if [ "$BUILD_UI" = true ]; then
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version)
        echo "Node.js $NODE_VERSION found, building with UI support"
        mvn clean install -Pui "$@"
    else
        echo "Node.js not found. Please install Node.js LTS (v20+) to build with UI support."
        echo "Visit https://nodejs.org/ to download and install Node.js."
        exit 1
    fi
else
    echo "Building server without UI (use --with-ui to include admin UI)"
    mvn clean install "$@"
fi
