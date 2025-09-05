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

USERNAME="$1"
PASSWORD="$2"
VERSION="3.3.7-SNAPSHOT"
APP_NAME="MapsMessaging"
ZIP_REPO="maps-snapshot"
PUSH_REPO="maps_osx_installer"

ZIP_NAME="maps-$VERSION-install.zip"
ZIP_URL="https://github.com/Maps-Messaging/mapsmessaging_server/releases/download/$VERSION/$ZIP_NAME"
PUSH_URL="https://repository.mapsmessaging.io/service/rest/v1/components?repository=$PUSH_REPO"

# Download ZIP
curl -L -o "$ZIP_NAME" "$ZIP_URL"

# Unzip
unzip -o "$ZIP_NAME"

# Build installer
BASE_DIR="$(pwd)/maps-$VERSION"
bash ./build-osx-package.sh "$VERSION" "$APP_NAME" "$BASE_DIR"

# Find the installer
INSTALLER_PATH=$(find out/mac -name "${APP_NAME}*.pkg" | head -n 1)

if [[ -n "$INSTALLER_PATH" ]]; then
  ENCODED_CREDS=$(printf "%s:%s" "$USERNAME" "$PASSWORD" | base64)
  FILENAME=$(basename "$INSTALLER_PATH")
  DIRECTORY="mac/$VERSION"

  # Delete old component if it exists
  SEARCH_URL="https://repository.mapsmessaging.io/service/rest/v1/search?repository=$PUSH_REPO&name=${FILENAME%.pkg}&version=$VERSION"
  COMPONENT_ID=$(curl -s -H "Authorization: Basic $ENCODED_CREDS" "$SEARCH_URL" | jq -r '.items[0].id // empty')

  if [[ -n "$COMPONENT_ID" ]]; then
    curl -X DELETE -H "Authorization: Basic $ENCODED_CREDS" \
         "https://repository.mapsmessaging.io/service/rest/v1/components/$COMPONENT_ID"
  fi

  # Upload new installer
  curl -u "$USERNAME:$PASSWORD" \
       -F "raw.directory=$DIRECTORY" \
       -F "raw.asset1=@$INSTALLER_PATH" \
       -F "raw.asset1.filename=$FILENAME" \
       "$PUSH_URL"
else
  echo "No installer found."
  exit 1
fi
