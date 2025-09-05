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

POM_VERSION=$(grep -m 1 "<version>.*</version>$" pom.xml | awk -F'[><]' '{print $3}')

export PACKAGE_NAME="maps"


echo "Defining environment variables"

export USER=$1
export PASSWORD=$2
export REPO_NAME=$3

# Variables
export VERSION_NAME=$POM_VERSION

DATE_SUFFIX=$(date +%Y%m%d.%H%M)

BASE_VERSION="${POM_VERSION}"
BASE_VERSION="${BASE_VERSION/ml-/}"

if [[ "$POM_VERSION" == *-SNAPSHOT ]]; then
  export PACKAGE_VERSION="${BASE_VERSION}.${DATE_SUFFIX}"
else
  export PACKAGE_VERSION="${BASE_VERSION}"
fi

export NEXUS_URL="https://repository.mapsmessaging.io"

export TAR_FILE="target/${PACKAGE_NAME}-${VERSION_NAME}-install.tar.gz"
export SOURCE_DIR="packaging/rpmbuild/SOURCES"
export SPEC_DIR="packaging/rpmbuild/SPECS"
export BUILD_ROOT=${PWD}/packaging/rpmbuild

# Apply sed updates to the correct spec file
if [[ "$POM_VERSION" == ml-* ]]; then
  export PACKAGE_FILE="${PACKAGE_NAME}-ml-${PACKAGE_VERSION}-1.noarch.rpm"
  export SPEC_FILE="${SPEC_DIR}/${PACKAGE_NAME}-ml.spec"
  export ML="-ml"
else
  export PACKAGE_FILE="${PACKAGE_NAME}-${PACKAGE_VERSION}-1.noarch.rpm"
  export SPEC_FILE="${SPEC_DIR}/${PACKAGE_NAME}.spec"
  export ML=""
fi

# Update spec values
sed -i "s|%%VERSION%%|${PACKAGE_VERSION}|g" "$SPEC_FILE"
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
RPM_DBPATH="/var/lib/buildkite-agent/builds/rpmdb-${DATE_SUFFIX}"
mkdir -p "$RPM_DBPATH"
rpm --dbpath "$RPM_DBPATH" --rebuilddb
export RPM_DBPATH
rpmbuild --define "_topdir ${BUILD_ROOT}" -ba ${SPEC_FILE}

echo rpm files built

# Function to delete the old package
delete_old_package() {
  echo "Deleting all ${PACKAGE_NAME}${ML} packages with base version ${BASE_VERSION}"

  SEARCH_URL="${NEXUS_URL}/service/rest/v1/search?repository=${REPO_NAME}&name=${PACKAGE_NAME}${ML}"
  ITEMS=$(curl -u ${USER}:${PASSWORD} -s "${SEARCH_URL}")

  echo "$ITEMS" | jq -c '.items[]' | while read -r item; do
    VERSION=$(echo "$item" | jq -r '.version')
    COMPONENT_ID=$(echo "$item" | jq -r '.id')

    # Check if version starts with BASE_VERSION
    if [[ $VERSION == ${BASE_VERSION}* ]]; then
      DELETE_COMPONENT_URL="${NEXUS_URL}/service/rest/v1/components/${COMPONENT_ID}"
      echo "Deleting version ${VERSION} with component ID ${COMPONENT_ID}"
      curl -u ${USER}:${PASSWORD} -X DELETE "${DELETE_COMPONENT_URL}"
    fi
  done
}

# Function to upload the new package
upload_new_package() {
  cd packaging/rpmbuild/RPMS/noarch || exit
  echo ${PACKAGE_FILE}
  ls -lsa ${PACKAGE_FILE}
  curl -v  \
   -u $USER:$PASSWORD  \
   -F "yum.asset=@${PACKAGE_FILE}" \
   -F "yum.asset.filename=${PACKAGE_FILE}" \
       "${NEXUS_URL}/service/rest/v1/components?repository=${REPO_NAME}"
  echo "Uploaded new package: ${PACKAGE_FILE}"
  cd ../../../..
  rm -rf "/var/lib/buildkite-agent/builds/rpmdb-${DATE_SUFFIX}"
}

# Main script
echo "Starting package replacement process..."

# Delete the old package if it exists
delete_old_package

# Upload the new package
upload_new_package

echo "Package replacement process completed."
