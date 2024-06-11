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

POM_VERSION=$(grep -m 1 "<version>.*</version>$" pom.xml | awk -F'[><]' '{print $3}')

# Variables
export VERSION_NAME=$POM_VERSION
export PROJECT_NAME=message_daemon

TAR_FILE="target/${PROJECT_NAME}-${VERSION_NAME}-install.tar.gz"

SOURCE_DIR="packaging/rpmbuild/SOURCES"
SPEC_DIR="packaging/rpmbuild/SPECS"

# Verify the tarball exists
if [[ ! -f ${TAR_FILE} ]]; then
  echo "Error: tarball ${TAR_FILE} not found."
  exit 1
fi

# Move the tarball to SOURCES
mkdir -p ${SOURCE_DIR}
cp ${TAR_FILE} ${SOURCE_DIR}

# Verify the tarball is in the SOURCES directory
if [[ ! -f ${SOURCE_DIR}/$(basename ${TAR_FILE}) ]]; then
  echo "Error: tarball ${TAR_FILE} not found in ${SOURCE_DIR}."
  exit 1
fi

export BUILD_ROOT=${PWD}/packaging/rpmbuild
# Build the RPM package
rpmbuild --define "_topdir ${BUILD_ROOT}" -ba ${SPEC_DIR}/message_daemon.spec

