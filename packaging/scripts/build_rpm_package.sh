#!/bin/bash

#
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
#

POM_VERSION=$(grep -m 1 "<version>.*</version>$" pom.xml | awk -F'[><]' '{print $3}')


export PACKAGE_NAME="maps"


echo "Defining environment variables"

export USER=$1
export PASSWORD=$2

# Variables
export VERSION_NAME=$POM_VERSION

DATE_SUFFIX=$(date +%Y%m%d.%H%M)

if [[ "$POM_VERSION" == *-SNAPSHOT ]]; then
  BASE_VERSION="${POM_VERSION/-SNAPSHOT/}"
  export PACKAGE_VERSION="${BASE_VERSION}.${DATE_SUFFIX}"
else
  export PACKAGE_VERSION="${POM_VERSION}"
fi

export NEXUS_URL="https://repository.mapsmessaging.io"
export REPO_NAME="maps_yum_snapshot"

export PACKAGE_FILE="${PACKAGE_NAME}-${PACKAGE_VERSION}-1.noarch.rpm"
export TAR_FILE="target/${PACKAGE_NAME}-${VERSION_NAME}-install.tar.gz"
export SOURCE_DIR="packaging/rpmbuild/SOURCES"
export SPEC_DIR="packaging/rpmbuild/SPECS"
export BUILD_ROOT=${PWD}/packaging/rpmbuild

# Apply sed updates to the correct spec file
SPEC_FILE="${SPEC_DIR}/${PACKAGE_NAME}.spec"

# Update spec values
sed -i "s/^Version:.*/Version:        ${PACKAGE_VERSION}/" "$SPEC_FILE"
sed -i "s/^Release:.*/Release:        1%{?dist}/" "$SPEC_FILE"
sed -i "s|^Source0:.*|Source0:        ${PACKAGE_NAME}-${VERSION_NAME}-install.tar.gz|" "$SPEC_FILE"
sed -i "s|^%setup -q -n .*|%setup -q -n ${PACKAGE_NAME}-${VERSION_NAME}|" "$SPEC_FILE"

echo "Validating files"

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


# Build the RPM package
echo "Building the rpm files"
if [[ $POM_VERSION == ml-* ]]; then
  rpmbuild --define "_topdir ${BUILD_ROOT}" -ba ${SPEC_DIR}/maps-ml.spec
else
  rpmbuild --define "_topdir ${BUILD_ROOT}" -ba ${SPEC_DIR}/maps.spec
fi
echo "rpm files built"

# Function to delete the old package
delete_old_package() {
  # URL to the package in the repository
  DELETE_URL="${NEXUS_URL}/service/rest/v1/components?repository=${REPO_NAME}&name=${PACKAGE_NAME}&version=${PACKAGE_VERSION}"

  DELETE_URL="${NEXUS_URL}/service/rest/v1/components?repository=${REPO_NAME}&name=${PACKAGE_NAME}&version=${PACKAGE_VERSION}"

  # Fetch the list of items
  ITEMS=$(curl -u ${USER}:${PASSWORD} -s "${DELETE_URL}" | jq '.items[] | select(.name == "'${PACKAGE_NAME}'")')

  # Extract the first matching component ID for the given package name
  COMPONENT_ID=$(echo "$ITEMS" | jq -r '.id')

  echo "Checking for existing package at ${DELETE_URL} found component with name ${PACKAGE_NAME} and ID ${COMPONENT_ID}"
  # Check if the component ID exists and delete the old package
  if [ -n "${COMPONENT_ID}" ]; then
    DELETE_COMPONENT_URL="${NEXUS_URL}/service/rest/v1/components/${COMPONENT_ID}"
    curl -u ${USER}:${PASSWORD} -X DELETE "${DELETE_COMPONENT_URL}"
    echo "Deleted old package with component ID: ${COMPONENT_ID}"
  else
    echo "No old package found to delete"
  fi
}

# Function to upload the new package
upload_new_package() {
  cd packaging/rpmbuild/RPMS/noarch || exit
  curl -v  \
   -u $USER:$PASSWORD  \
   -F "yum.asset=@${PACKAGE_FILE}" \
   -F "yum.asset.filename=${PACKAGE_FILE}" \
       "${NEXUS_URL}/service/rest/v1/components?repository=maps_yum_snapshot"
  echo "Uploaded new package: ${PACKAGE_FILE}"
  cd ../../../..
}

# Main script
echo "Starting package replacement process..."

# Delete the old package if it exists
delete_old_package

# Upload the new package
upload_new_package

echo "Package replacement process completed."
