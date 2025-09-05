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

docker login --username $1 --password $2

POM_VERSION=$(cat pom.xml | grep -m 1 "<version>.*</version>$" | awk -F'[><]' '{print $3}')
export LOWERCASE_VERSION="${POM_VERSION,,}"

# ---------------------------------------------------
# Setup and Authenticate Docker to AWS ECR
AWS_ECR_PASSWORD=$3
AWS_ECR_REPOSITORY_URI="public.ecr.aws/u9e3v0s2"
AWS_REGION="ap-southeast-2"

aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin $AWS_ECR_REPOSITORY_URI

# ---------------------------------------------------


# ---------------------------------------------------
# Build the new x86 Docker image
cd src/main/docker || exit
mv Dockerfile Dockerfile.orig
sed s/%%MAPS_VERSION%%/$POM_VERSION/g Dockerfile.orig > Dockerfile
docker build --no-cache -t mapsmessaging/server_daemon_$LOWERCASE_VERSION --label $LOWERCASE_VERSION . --push
#
# Tag for AWS ECR and push
docker tag mapsmessaging/server_daemon_$LOWERCASE_VERSION $AWS_ECR_REPOSITORY_URI/maps-messaging:server_daemon_$LOWERCASE_VERSION
docker push $AWS_ECR_REPOSITORY_URI/maps-messaging:server_daemon_$LOWERCASE_VERSION
#
# ---------------------------------------------------

# ---------------------------------------------------
# Build the new arm64 Docker image
cd arm
mv Dockerfile Dockerfile.orig
sed s/%%MAPS_VERSION%%/$POM_VERSION/g Dockerfile.orig > Dockerfile
docker buildx build --platform linux/arm64 --no-cache -t mapsmessaging/server_daemon_arm_$LOWERCASE_VERSION --label $LOWERCASE_VERSION . --load

# Tag the image for AWS ECR
docker tag mapsmessaging/server_daemon_arm_$LOWERCASE_VERSION $AWS_ECR_REPOSITORY_URI/maps-messaging:server_daemon_arm_$LOWERCASE_VERSION

# Push to Docker Hub
docker push mapsmessaging/server_daemon_arm_$LOWERCASE_VERSION

# Push to AWS ECR
docker push $AWS_ECR_REPOSITORY_URI/maps-messaging:server_daemon_arm_$LOWERCASE_VERSION

#
# ---------------------------------------------------

# ---------------------------------------------------
# Clean up so we do not waste disk space
docker image prune -af
docker system prune -af --volumes
# ---------------------------------------------------
