#
#    Copyright [ 2020 - 2021 ] [Matthew Buckton]
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#
#
#
#
# Define the home directory for the messaging daemon
#
export JAVA_HOME=/opt/jdk-13
export PATH=$JAVA_HOME/bin:$PATH
#
export MAPS_HOME=/message_daemon-1.1-SNAPSHOT
export MAPS_LIB=$MAPS_HOME/lib
export MAPS_CONF=$MAPS_HOME/conf
#
# From there configure all the paths.
#
# Note::: The conf directory must be at the start else the configuration is loaded from the jars
#
export CLASSPATH="$MAPS_CONF":$MAPS_LIB/message_daemon-1.1-SNAPSHOT.jar:"$MAPS_LIB/*"
#
# Now start the the daemon
#
java -classpath $CLASSPATH -Djava.security.auth.login.config=$MAPS_CONF/jaasAuth.config -DMAPS_HOME=$MAPS_HOME org.maps.messaging.MessageDaemon