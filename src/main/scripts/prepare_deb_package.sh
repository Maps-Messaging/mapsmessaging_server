#!/bin/bash

#
# Copyright [ 2020 - 2024 ] [Matthew Buckton]
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
#

set -e

# Variables
TAR_FILE="message_daemon-3.3.7-SNAPSHOT-install.tar.gz"
TARGET_DIR="src/main/deb_package"
INSTALL_DIR="${TARGET_DIR}/opt/message_daemon"
BIN_DIR="src/main/scripts"

# Create target directory
mkdir -p ${INSTALL_DIR}/bin

# Download the tar.gz file
wget -O ${TAR_FILE} ${URL}

# Extract the tar.gz file into the install directory
tar -xzf ${TAR_FILE} --strip-components=1 -C ${INSTALL_DIR}

# Copy the provided start.sh and message-daemon scripts into the install directory
cp ${BIN_DIR}/start.sh ${INSTALL_DIR}/start.sh
chmod +x ${INSTALL_DIR}/start.sh

cp ${BIN_DIR}/message_daemon ${INSTALL_DIR}/bin/message_daemon
chmod +x ${INSTALL_DIR}/bin/message_daemon

# Ensure postinst script is executable
chmod +x ${TARGET_DIR}/DEBIAN/postinst

echo "Preparation complete. You can now create the Debian package using dpkg-deb --build ${TARGET_DIR}"
dpkg-deb --build ${TARGET_DIR}
