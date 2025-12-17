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
# Configure the java command on the path
#
export JAVA_HOME=/usr/lib/jvm/zulu-21
export PATH=$JAVA_HOME/bin:$PATH

echo $PATH

export VERSION=%%MAPS_VERSION%%

# Check if FLY_CONSUL_URL is set and not empty
if [[ -n "${FLY_CONSUL_URL}" ]]; then
  # If FLY_CONSUL_URL is set, use its value for CONSUL_URL
  CONSUL_URL="${FLY_CONSUL_URL}"
fi

# Export the CONSUL_URL so it becomes an environment variable
export CONSUL_URL
echo $CONSUL_URL

if [ -z ${MAPS_HOME+x} ];
  then export MAPS_HOME=/maps-$VERSION;
fi

if [ -z ${MAPS_DATA+x} ];
  then export MAPS_DATA=/data
fi

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

#
# Now start the the daemon
java  -Xss256k  \
    -classpath $CLASSPATH $JAVA_OPTS \
    -DUSE_UUID=false \
    -DConsulUrl="${CONSUL_URL}" \
    -DConsulPath="${CONSUL_PATH}" \
    -DConsulToken="${CONSUL_TOKEN}" \
    -Djava.security.auth.login.config="${MAPS_CONF}/jaasAuth.config" \
    -DMAPS_HOME="${MAPS_HOME}" \
    io.mapsmessaging.MessageDaemon