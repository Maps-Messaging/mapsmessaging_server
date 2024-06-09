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

POM_VERSION=$(cat pom.xml | grep -m 1 "<version>.*</version>$" | awk -F'[><]' '{print $3}')

# Variables
export VERSION_NAME=$POM_VERSION
export PROJECT_NAME=message_daemon

TAR_FILE="target/${PROJECT_NAME}-${VERSION_NAME}-install.tar.gz"
TARGET_DIR="packaging/deb_package"
INSTALL_DIR="${TARGET_DIR}/opt/message_daemon"
BIN_DIR="src/main/scripts"
ETC_DIR="${TARGET_DIR}/etc/message_daemon"

# Create target directories
mkdir -p ${INSTALL_DIR}/bin
mkdir -p ${ETC_DIR}
mkdir -p ${TARGET_DIR}/lib/systemd/system

# Extract the tar.gz file into the install directory
tar -xzf ${TAR_FILE} --strip-components=1 -C ${INSTALL_DIR}

# Copy the provided start.sh and message-daemon scripts into the install directory
cp ${BIN_DIR}/start.sh ${INSTALL_DIR}/start.sh
chmod +x ${INSTALL_DIR}/start.sh

cp ${BIN_DIR}/message_daemon ${INSTALL_DIR}/bin/message_daemon
chmod +x ${INSTALL_DIR}/bin/message_daemon

# Copy the etc files
cp packaging/deb_package/etc/message_daemon.env ${ETC_DIR}/message_daemon.env
cp packaging/deb_package/etc/message_daemon.service ${TARGET_DIR}/lib/systemd/system/message_daemon.service

# Ensure postinst and prerm scripts are executable
chmod +x ${TARGET_DIR}/DEBIAN/postinst
chmod +x ${TARGET_DIR}/DEBIAN/prerm

echo "Preparation complete. You can now create the Debian package using dpkg-deb --build ${TARGET_DIR}"
dpkg-deb --build ${TARGET_DIR}
