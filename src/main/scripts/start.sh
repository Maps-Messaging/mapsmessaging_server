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
# Define the home directory for the messaging daemon
#
export VERSION=%%MAPS_VERSION%%

if [ -z ${MAPS_HOME+x} ];
  then export MAPS_HOME=/opt/message_daemon-$VERSION;
fi

# Get the value of the ConsulHost environment variable, or default to 127.0.0.1
CONSUL_URL="${ConsulUrl:-'http://127.0.0.1/'}"
export CONSUL_URL

JAVA_OPTS="${JavaOpts:-''}"
export JAVA_OPTS

echo "Maps Home is set to '$MAPS_HOME'"
export MAPS_LIB=$MAPS_HOME/lib
export MAPS_CONF=$MAPS_HOME/conf

#
# From there configure all the paths.
#
# Note::: The conf directory must be at the start else the configuration is loaded from the jars
#
export CLASSPATH="$MAPS_CONF":$MAPS_LIB/message_daemon-$VERSION.jar:"$MAPS_LIB/*"
export LD_LIBRARY_PATH=$MAPS_LIB:$LD_LIBRARY_PATH
#
# Now start the the daemon
#
if [ -z ${FLY_CONSUL_URL+x} ];
then
    echo "Detected fly.io consul URL ..."
    protocol=$(echo "$FLY_CONSUL_URL" | grep "://" | sed -e's,^\(.*://\).*,\1,g')
    # Remove the protocol
    url_no_protocol=$(echo "${1/$protocol/}")
    # Use tr: Make the protocol lower-case for easy string compare
    protocol=$(echo "$protocol" | tr '[:upper:]' '[:lower:]')
    # Extract the user and password (if any)
    # cut 1: Remove the path part to prevent @ in the querystring from breaking the next cut
    # rev: Reverse string so cut -f1 takes the (reversed) rightmost field, and -f2- is what we want
    # cut 2: Remove the host:port
    # rev: Undo the first rev above
    userpass=$(echo "$url_no_protocol" | grep "@" | cut -d"/" -f1 | rev | cut -d"@" -f2- | rev)
    pass=$(echo "$userpass" | grep ":" | cut -d":" -f2)
    if [ -n "$pass" ]; then
      user=$(echo "$userpass" | grep ":" | cut -d":" -f1)
    else
      user="$userpass"
    fi
    # Extract the host
    hostport=$(echo "${url_no_protocol/$userpass@/}" | cut -d"/" -f1)
    host=$(echo "$hostport" | cut -d":" -f1)
    port=$(echo "$hostport" | grep ":" | cut -d":" -f2)
    path=$(echo "$url_no_protocol" | grep "/" | cut -d"/" -f2-)
    java -classpath $CLASSPATH $JAVA_OPTS -DUSE_UUID=false -DConsulUrl=$protocol$hostport -DConsulPath=$path -DConsulToken=$pass -Djava.security.auth.login.config=$MAPS_CONF/jaasAuth.config -DMAPS_HOME=$MAPS_HOME io.mapsmessaging.MessageDaemon
else
    java -classpath $CLASSPATH $JAVA_OPTS -DUSE_UUID=false -DConsulUrl=$CONSUL_URL -DConsulPath=$ConsulPath -Djava.security.auth.login.config=$MAPS_CONF/jaasAuth.config -DMAPS_HOME=$MAPS_HOME io.mapsmessaging.MessageDaemon
fi

