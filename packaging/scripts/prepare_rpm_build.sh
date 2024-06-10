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

SOURCE_DIR="rpmbuild/SOURCES"
SPEC_DIR="rpmbuild/SPECS"

# Create source directories
mkdir -p ${PROJECT_NAME}-${VERSION_NAME}/bin
mkdir -p ${PROJECT_NAME}-${VERSION_NAME}/etc
mkdir -p ${PROJECT_NAME}-${VERSION_NAME}/systemd

# Copy files to source directories
cp src/main/scripts/start.sh ${PROJECT_NAME}-${VERSION_NAME}/start.sh
cp src/main/scripts/message_daemon ${PROJECT_NAME}-${VERSION_NAME}/bin/message_daemon
cp packaging/etc/message_daemon.env ${PROJECT_NAME}-${VERSION_NAME}/etc/message_daemon.env
cp packaging/etc/message_daemon.service ${PROJECT_NAME}-${VERSION_NAME}/systemd/message_daemon.service

# Create tarball from source directories
mkdir -p target
tar -czf ${TAR_FILE} -C ${PROJECT_NAME}-${VERSION_NAME}/ .

# Move the tarball to SOURCES
mkdir -p ${SOURCE_DIR}
mv ${TAR_FILE} ${SOURCE_DIR}

# Copy the SPEC file to SPECS
mkdir -p ${SPEC_DIR}
cp message_daemon.spec ${SPEC_DIR}

# Build the RPM package
rpmbuild -ba ${SPEC_DIR}/message_daemon.spec

echo "RPM build complete."
