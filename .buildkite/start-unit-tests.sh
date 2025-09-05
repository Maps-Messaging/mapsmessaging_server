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
set -e

WORKDIR="$(mktemp -d -p "$HOME" mapsmessaging-unittest-XXXXXX)"
trap "rm -rf '$WORKDIR'" EXIT

cd "$WORKDIR"

git clone --depth 1 --branch development https://github.com/Maps-Messaging/mapsmessaging_server.git
cd mapsmessaging_server

ulimit -a

git clone https://github.com/eclipse/paho.mqtt.testing.git

mvn clean test -Dpython_command=python3 -Dcom.datastax.driver.FORCE_NIO=true -Ddebug_domain=false -Psnapshot

# Extract test results
RESULTS_DIR="$HOME/mapsmessaging-test-results"
mkdir -p "$RESULTS_DIR"
find . -name "TEST-*.xml" -exec cp {} "$RESULTS_DIR/" \;

# Trigger result pipeline
curl -X POST "https://api.buildkite.com/v2/organizations/mapsmessaging/pipelines/810-retrieve-junit-results/builds" \
  -H "Authorization: Bearer $BUILDKITE_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "commit": "HEAD",
    "branch": "main",
    "message": "JUnit results ready for processing",
    "env": {
      "RESULT_LOCATION": "/home/buildkite/mapsmessaging-test-results"
    }
  }'
