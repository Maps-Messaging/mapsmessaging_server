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

# Variables
TIMESTAMP=$(date +"%d-%b-%Y %H:%M:%S")
POM_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

# Paths
BUILD_INFO_FILE="src/main/java/io/mapsmessaging/BuildInfo.java"

# Step a: Replace the date string in BuildInfo.java
sed -i "s/dd-mmm-yyyy HH:MM:SS/${TIMESTAMP}/g" $BUILD_INFO_FILE

# Step b: Replace the snapshot string in BuildInfo.java
sed -i "s/00.00.00-SNAPSHOT/${POM_VERSION}/g" $BUILD_INFO_FILE

# Step c: Replace %%MAPS_VERSION%% with $POM_VERSION in ./src/**
find ./src/ -type f -exec sed -i "s|%%MAPS_VERSION%%|${POM_VERSION}|g" {} +


if [[ $POM_VERSION == ml-* ]]; then
   sed -i 's#<version>\([^<]*\)</version>#<version>ml-\1</version>#' pom.xml
   sed -i 's/Package: maps/Package: maps-ml/' packaging/deb_package/DEBIAN/control
fi


echo "Replacements done."
