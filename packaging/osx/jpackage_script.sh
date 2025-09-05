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

VERSION="$1"
APP_NAME="$2"
BASE_DIR="$3"
OUTPUT_DIR="$4"
RUNTIME_IMAGE="$5"
STAGING_DIR="$6"

INPUT_DIR="$STAGING_DIR/maps-$VERSION"
MAIN_JAR="maps-$VERSION.jar"
MAIN_CLASS="io.mapsmessaging.MessageDaemon"

SANITIZED_VERSION="${VERSION}"
SOURCE_DIR="maps-$VERSION"
DEST_DIR="$STAGING_DIR/maps-$SANITIZED_VERSION"

mv -f "$SOURCE_DIR" "$DEST_DIR"

INPUT_DIR="$DEST_DIR"
MAIN_JAR="maps-$VERSION.jar"  # original JAR name remains

# Replace MAPS_DATA with ProgramData in logback.xml
sed -i '' 's|MAPS_DATA|/Library/Logs/MapsMessaging|g' "$INPUT_DIR/conf/logback.xml"
mkdir -p out/mac

# Run jpackage for macOS
"$JAVA_HOME/bin/jpackage" \
  --type pkg \
  --name "$APP_NAME" \
  --app-version "$SANITIZED_VERSION" \
  --input "$INPUT_DIR" \
  --main-jar "lib/$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --runtime-image "$RUNTIME_IMAGE" \
  --icon "favicon.icns" \
  --dest "$OUTPUT_DIR" \
  --resource-dir "$INPUT_DIR" \
  --install-dir "/Applications/MapsMessaging" \
  --vendor "Maps Messaging" \
  --add-launcher mapsTop=mapsTop.properties \
  --license-file "$INPUT_DIR/LICENSE" \
  --java-options '-DMAPS_HOME="$APPDIR" -DMAPS_CONF="$APPDIR/conf" -DMAPS_DATA="/Library/Application\ Support/MapsMessaging/data" -DCONSUL_URL=http://localhost:8500/'

