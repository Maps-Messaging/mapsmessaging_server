#
# Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

#
# Configure the java command on the path
#
export JAVA_HOME=/opt/jdk-13
export PATH=$JAVA_HOME/bin:$PATH

export VERSION=%%MAPS_VERSION%%

# Get the value of the ConsulHost environment variable, or default to 127.0.0.1
CONSUL_URL="${ConsulUrl:-'http://127.0.0.1/'}"
export CONSUL_URL

if [ -z ${MAPS_HOME+x} ];
  then export MAPS_HOME=/opt/message_daemon-$VERSION;
fi

echo "Maps Home is set to '$MAPS_HOME'"
export MAPS_LIB=$MAPS_HOME/lib
export MAPS_CONF=$MAPS_HOME/conf

#
# From there configure all the paths.
#
# Note::: The conf directory must be at the start else the configuration is loaded from the jars
#
export CLASSPATH="$MAPS_CONF":$MAPS_LIB/message_daemon-$VERSION.jar:"$MAPS_LIB/*"
#
# Now start the the daemon
#
# Check if FLY_CONSUL_URL is set and non-empty
if [ -n "${FLY_CONSUL_URL}" ]; then
    echo "Detected fly.io consul URL ..."
 # Extract the protocol
    protocol=$(echo "${FLY_CONSUL_URL}" | awk -F '://' '{print $1 "://"}')

    # Extract the token
    token=$(echo "${FLY_CONSUL_URL}" | awk -F '://' '{print $2}' | awk -F '@' '{print $1}')

    # Extract the base URL without the token
    base_url=$(echo "${FLY_CONSUL_URL}" | awk -F '@' '{print $2}' | awk -F '/' '{print $1 "://"}' | awk -F ':' '{print $1 ":" $2}')

    # Extract the path
    path=$(echo "${FLY_CONSUL_URL}" | awk -F '@' '{print $2}' | cut -d '/' -f2- | sed 's/^/\//')

    # Construct the Java command
    java -classpath $CLASSPATH $JAVA_OPTS \
        -DUSE_UUID=false \
        -DConsulUrl="${protocol}${base_url}" \
        -DConsulPath="${path}" \
        -DConsulToken="${token}" \
        -Djava.security.auth.login.config="${MAPS_CONF}/jaasAuth.config" \
        -DMAPS_HOME="${MAPS_HOME}" \
        io.mapsmessaging.MessageDaemon
else
    # Default Java command if FLY_CONSUL_URL is not set
    java -classpath $CLASSPATH $JAVA_OPTS \
        -DUSE_UUID=false \
        -DConsulUrl="${CONSUL_URL}" \
        -DConsulPath="${CONSUL_PATH}" \
        -DConsulToken="${CONSUL_TOKEN}" \
        -Djava.security.auth.login.config="${MAPS_CONF}/jaasAuth.config" \
        -DMAPS_HOME="${MAPS_HOME}" \
        io.mapsmessaging.MessageDaemon
fi


