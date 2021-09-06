#
# Define the home directory for the messaging daemon
#
export MAPS_HOME=/opt/message_daemon-1.1-SNAPSHOT
export MAPS_LIB=$MAPS_HOME/lib
export MAPS_CONF=$MAPS_HOME/conf

#
# From there configure all the paths.
#
# Note::: The conf directory must be at the start else the configuration is loaded from the jars
#
export CLASSPATH="$MAPS_CONF":$MAPS_LIB/message_daemon-1.1.4.jar:"$MAPS_LIB/*"
export LD_LIBRARY_PATH=$MAPS_LIB:$LD_LIBRARY_PATH
#
# Now start the the daemon
#
java -classpath $CLASSPATH -Djava.security.auth.login.config=$MAPS_CONF/jaasAuth.config -DMAPS_HOME=$MAPS_HOME io.mapsmessaging.MessageDaemon

