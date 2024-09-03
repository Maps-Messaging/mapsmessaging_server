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

# Variables
POM_VERSION=$(cat pom.xml | grep -m 1 "<version>.*</version>$" | awk -F'[><]' '{print $3}')

NEXUS_URL="https://repository.mapsmessaging.io:8081"
REPO_NAME="maps_messaging_rpm_repo"
PACKAGE_VERSION=POM_VERSION
PACKAGE_FILE="${PACKAGE_NAME}-${PACKAGE_VERSION}-1.el7.noarch.rpm"
UPLOAD_URL="${NEXUS_URL}/repository/${REPO_NAME}/${PACKAGE_FILE}"
USER=$1
PASSWORD=$2
if [[ $POM_VERSION == ml-* ]]; then
  PACKAGE_NAME="maps-ml"
else
  PACKAGE_NAME="maps"
fi
# Function to delete the old package
delete_old_package() {
  # URL to the package in the repository
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
  cd packaging/rpmbuild/RPMS/noarch
  http \
	--auth $USER:$PASSWORD \
  	--multipart \
  	--ignore-stdin \
    POST "${NEXUS_URL}/service/rest/v1/components?repository=${REPO_NAME}" \
    rpm.asset@${PACKAGE_FILE}
  echo "Uploaded new package: ${PACKAGE_FILE}"
}

# Main script
echo "Starting package replacement process..."

# Delete the old package if it exists
delete_old_package

# Upload the new package
upload_new_package

echo "Package replacement process completed."
