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

# Set the GitHub token for authentication (this is automatically picked up by `gh` if it's set)
export GITHUB_TOKEN=$1

# Delete the release from GitHub before creating a new one
echo "Deleting release from GitHub before creating a new one"
gh release delete ${VERSION_NAME} --repo ${GITHUB_ORGANIZATION}/${GITHUB_REPO} --yes || true
sleep 5

# Create a new release in GitHub
echo "Creating a new release in GitHub"
gh release create ${VERSION_NAME} --repo ${GITHUB_ORGANIZATION}/${GITHUB_REPO} --title "${VERSION_NAME}" --notes "Automated release ${VERSION_NAME}" --target main --draft

sleep 5

# Upload the artifacts to GitHub
echo "Uploading the artifacts to GitHub"
gh release upload ${VERSION_NAME} target/${PROJECT_NAME}-${VERSION_NAME}-install.zip --repo ${GITHUB_ORGANIZATION}/${GITHUB_REPO} --clobber
gh release upload ${VERSION_NAME} target/${PROJECT_NAME}-${VERSION_NAME}-install.tar.gz --repo ${GITHUB_ORGANIZATION}/${GITHUB_REPO} --clobber
gh release upload ${VERSION_NAME} target/${PROJECT_NAME}-${VERSION_NAME}-jar-with-dependencies.jar --repo ${GITHUB_ORGANIZATION}/${GITHUB_REPO} --clobber