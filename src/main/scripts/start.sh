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

#
# Define the home directory for the messaging daemon
#
export VERSION=%%MAPS_VERSION%%

current_dir=$(pwd)
if [[ "$current_dir" == */bin ]]; then
    parent_dir=$(dirname "$current_dir")
else
    parent_dir="$current_dir"
fi

if [ -z ${MAPS_HOME+x} ]; then
    export MAPS_HOME="$parent_dir"
fi

if [ -z ${MAPS_DATA+x} ];
  then export MAPS_DATA=$MAPS_HOME/data;
fi

# Check if FLY_CONSUL_URL is set and not empty
if [[ -n "${FLY_CONSUL_URL}" ]]; then
  # If FLY_CONSUL_URL is set, use its value for CONSUL_URL
  CONSUL_URL="${FLY_CONSUL_URL}"
else
  # If FLY_CONSUL_URL is not set, use the default value
  CONSUL_URL="${ConsulUrl:-'http://127.0.0.1/'}"
fi

# Export the CONSUL_URL so it becomes an environment variable
export CONSUL_URL

echo "Maps Home is set to '$MAPS_HOME'"
echo "Maps Data is set to '$MAPS_DATA'"

export MAPS_LIB=$MAPS_HOME/lib
export MAPS_CONF=$MAPS_HOME/conf

#
# From there configure all the paths.
#
# Note::: The conf directory must be at the start else the configuration is loaded from the jars
#
CLASSPATH="$MAPS_CONF:$MAPS_LIB/maps-$VERSION.jar"
for jar in "$MAPS_LIB"/*.jar; do
  [[ "$jar" != "$MAPS_LIB/maps-$VERSION.jar" ]] && CLASSPATH="$CLASSPATH:$jar"
done

export LD_LIBRARY_PATH=$MAPS_LIB:$LD_LIBRARY_PATH
#
# Loop to restart server on specific exit code
#
while true; do
    java -Xss256k \
         -classpath $CLASSPATH $JAVA_OPTS \
        -DUSE_UUID=false \
        -DConsulUrl="${CONSUL_URL}" \
        -DConsulPath="${CONSUL_PATH}" \
        -DConsulToken="${CONSUL_TOKEN}" \
        -Djava.security.auth.login.config="${MAPS_CONF}/jaasAuth.config" \
        -DMAPS_HOME="${MAPS_HOME}" \
        io.mapsmessaging.MessageDaemon

    EXIT_CODE=$?

    if [ $EXIT_CODE -eq 8 ]; then
        echo "Restarting server..."
    else
        echo "Exiting with code $EXIT_CODE"
        exit $EXIT_CODE
    fi
done