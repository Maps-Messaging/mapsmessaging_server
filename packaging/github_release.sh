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

export GITHUB_ORGANIZATION=Maps-Messaging
export GITHUB_REPO=mapsmessaging_server
export VERSION_NAME=$POM_VERSION
export PROJECT_NAME=message_daemon

export PATH=/root/go/bin:$PATH

echo "Exporting token and enterprise api to enable github-release tool"
export GITHUB_TOKEN=$1

echo "Deleting release from github before creating new one"
github-release delete --user ${GITHUB_ORGANIZATION} --repo ${GITHUB_REPO} --tag ${VERSION_NAME} || true
sleep 5

echo "Creating a new release in github"
github-release release --user ${GITHUB_ORGANIZATION} --repo ${GITHUB_REPO} --tag ${VERSION_NAME} --name "${VERSION_NAME}"

sleep 5
echo "Uploading the artifacts into github"
github-release upload --user ${GITHUB_ORGANIZATION} --repo ${GITHUB_REPO} --tag ${VERSION_NAME} --name "${PROJECT_NAME}-${VERSION_NAME}-install.zip" --file target/${PROJECT_NAME}-${VERSION_NAME}-install.zip
github-release upload --user ${GITHUB_ORGANIZATION} --repo ${GITHUB_REPO} --tag ${VERSION_NAME} --name "${PROJECT_NAME}-${VERSION_NAME}-install.tar.gz" --file target/${PROJECT_NAME}-${VERSION_NAME}-install.tar.gz
github-release upload --user ${GITHUB_ORGANIZATION} --repo ${GITHUB_REPO} --tag ${VERSION_NAME} --name "${PROJECT_NAME}-${VERSION_NAME}-jar-with-dependencies.jar" --file target/${PROJECT_NAME}-${VERSION_NAME}-jar-with-dependencies.jar
