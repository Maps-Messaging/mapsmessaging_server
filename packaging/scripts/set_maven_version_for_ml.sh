#!/usr/bin/env bash

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


set -euo pipefail


# Must run from project root
test -f pom.xml || { echo "pom.xml not found"; exit 1; }

# Read effective project version (single line, no CRs)
cur=$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version | tr -d '\r' | tail -n1)
[ -z "${cur}" ] && { echo "Could not read project.version"; exit 1; }
echo "project.version=${cur}"

# Idempotent prefix (avoid ml-ml-…)
new="ml-${cur#ml-}"
echo "new version=${new}"

# Update only <project><version>
mvn -q versions:set -DnewVersion="${new}" -DgenerateBackupPoms=false