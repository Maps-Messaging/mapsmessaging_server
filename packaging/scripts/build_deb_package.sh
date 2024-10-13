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

export USER=$1
export PASSWORD=$2
export REPO_NAME=maps-snapshot

# Extract POM version from pom.xml
POM_VERSION=$(grep -m 1 "<version>.*</version>$" pom.xml | awk -F'[><]' '{print $3}')
if [[ $POM_VERSION == ml-* ]]; then
  export PACKAGE_NAME="maps-ml-server"
else
  export PACKAGE_NAME="maps-server"
fi

export NEXUS_URL="https://repo.mapsmessaging.io"

export PACKAGE_VERSION=$POM_VERSION
export PACKAGE_FILE="${PACKAGE_NAME}_${PACKAGE_VERSION}_all.deb"

export VERSION_NAME=$POM_VERSION
export PROJECT_NAME=maps
export GITHUB_ORGANIZATION=Maps-Messaging
export GITHUB_REPO=mapsmessaging_server
export TAR_FILE="target/maps-${VERSION_NAME}-install.tar.gz"
export TARGET_DIR="packaging/deb_package"
export INSTALL_DIR="${TARGET_DIR}/opt/maps"
export ETC_DIR="${INSTALL_DIR}/etc"

# Update control file dynamically with the correct package name and version
sed -i "s/^Package:.*/Package: ${PACKAGE_NAME}/" ${TARGET_DIR}/DEBIAN/control
sed -i "s/^Version:.*/Version: ${PACKAGE_VERSION}/" ${TARGET_DIR}/DEBIAN/control


# Create necessary directories
mkdir -p ${INSTALL_DIR}
mkdir -p ${ETC_DIR}

# Extract the tar.gz file into the install directory
tar -xzf ${TAR_FILE} --strip-components=1 -C ${INSTALL_DIR}

# Set necessary permissions
chmod +x ${INSTALL_DIR}/bin/start.sh
chmod +x ${INSTALL_DIR}/bin/maps

# Ensure postinst and prerm scripts are executable
chmod +x ${TARGET_DIR}/DEBIAN/postinst
chmod +x ${TARGET_DIR}/DEBIAN/prerm
chmod +x ${TARGET_DIR}/DEBIAN/preinst

echo "Preparation complete. You can now create the Debian package using dpkg-deb --build ${TARGET_DIR}"
dpkg-deb --build ${TARGET_DIR}

# Rename the generated package to match PACKAGE_FILE
mv ${TARGET_DIR}.deb ${PACKAGE_FILE}
echo "Debian package built and renamed to ${PACKAGE_FILE}"

# Function to delete the old package
delete_old_package() {
  DELETE_URL="${NEXUS_URL}/service/rest/v1/components?repository=${REPO_NAME}&name=${PACKAGE_NAME}&version=${PACKAGE_VERSION}"

  # Fetch component ID of the old package
  COMPONENT_ID=$(curl -u ${USER}:${PASSWORD} -s "${DELETE_URL}" | jq -r '.items[0].id')

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
  cd packaging
  RESPONSE=$(http --auth $USER:$PASSWORD --multipart --ignore-stdin POST "${NEXUS_URL}/service/rest/v1/components?repository=${REPO_NAME}" deb.asset@${PACKAGE_FILE} -v)

  if [[ $RESPONSE == *"201 Created"* ]]; then
    echo "Package upload successful"
  else
    echo "Package upload failed"
    exit 1
  fi
  cd ..
}

# Main script
echo "Starting package replacement process..."

# Delete the old package if it exists
delete_old_package

# Upload the new package
upload_new_package

echo "Package replacement process completed."
