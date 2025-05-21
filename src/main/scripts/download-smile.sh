#!/bin/bash

# This script downloads Smile ML libraries (GPL-3.0 licensed) from Maven Central.
# You are responsible for complying with the Smile license (https://github.com/haifengl/smile).
# MapsMessaging does not distribute Smile or bundle it directly.

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

current_dir=$(pwd)
if [[ "$current_dir" == */bin ]]; then
    parent_dir=$(dirname "$current_dir")
else
    parent_dir="$current_dir"
fi

if [ -z ${MAPS_HOME+x} ]; then
    export MAPS_HOME="$parent_dir"
fi

VERSION=4.3.0
BASE_URL=https://repo1.maven.org/maven2

GROUP_ID=dev/smile
MODULES=(
  smile-core
  smile-data
  smile-math
  smile-graph
  smile-plot
)

TARGET_DIR=$MAPS_HOME/lib
mkdir -p "$TARGET_DIR"

echo "Downloading Smile $VERSION JARs..."

for MODULE in "${MODULES[@]}"; do
  FILE="$MODULE-$VERSION.jar"
  URL="$BASE_URL/$GROUP_ID/$MODULE/$VERSION/$FILE"
  echo "-> $FILE"
  curl -sSL "$URL" -o "$TARGET_DIR/$FILE"
done

echo "All Smile JARs downloaded to $TARGET_DIR"
