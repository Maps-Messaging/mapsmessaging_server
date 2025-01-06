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

POM_VERSION=$(cat pom.xml | grep -m 1 "<version>.*</version>$" | awk -F'[><]' '{print $3}')
echo $POM_VERSION
cd src/main/docker
# Build the new Docker image

mv Dockerfile Dockerfile.orig
sed s/%%MAPS_VERSION%%/$POM_VERSION/g Dockerfile.orig > Dockerfile

docker build -t mapsmessaging-daemon --label $POM_VERSION .
