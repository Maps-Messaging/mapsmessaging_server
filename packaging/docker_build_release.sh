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
echo "$2" | docker login --username "$1" --password-stdin

POM_VERSION=$(grep -m1 "<version>.*</version>$" pom.xml | awk -F'[><]' '{print $3}')
export LOWERCASE_VERSION="${POM_VERSION,,}"

AWS_ECR_REPOSITORY_URI="public.ecr.aws/u9e3v0s2"
AWS_REGION="us-east-1"
aws ecr-public get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$AWS_ECR_REPOSITORY_URI"

# ensure a buildx builder with emulation
docker buildx create --name mapsbuilder --driver docker-container --use >/dev/null 2>&1 || true
docker run --privileged --rm tonistiigi/binfmt --install amd64,arm64 >/dev/null
docker buildx inspect --bootstrap >/dev/null

# -------------------------
# x86_64 (amd64) IMAGE
# -------------------------
cd src/main/docker || exit
mv Dockerfile Dockerfile.orig
sed "s/%%MAPS_VERSION%%/$POM_VERSION/g" Dockerfile.orig > Dockerfile

docker buildx build \
  --platform linux/amd64 \
  --no-cache \
  -t "mapsmessaging/server_daemon_${LOWERCASE_VERSION}" \
  -t "${AWS_ECR_REPOSITORY_URI}/maps-messaging:server_daemon_${LOWERCASE_VERSION}" \
  --label "version=${LOWERCASE_VERSION}" \
  . --push

# -------------------------
# arm64 IMAGE
# -------------------------
cd arm || exit
mv Dockerfile Dockerfile.orig
sed "s/%%MAPS_VERSION%%/$POM_VERSION/g" Dockerfile.orig > Dockerfile

docker buildx build \
  --platform linux/arm64 \
  --no-cache \
  -t "mapsmessaging/server_daemon_arm_${LOWERCASE_VERSION}" \
  -t "${AWS_ECR_REPOSITORY_URI}/maps-messaging:server_daemon_arm_${LOWERCASE_VERSION}" \
  --label "version=${LOWERCASE_VERSION}" \
  . --push

# cleanup
docker image prune -af
docker system prune -af --volumes
