#
# Define the home directory for the messaging daemon
#
export VERSION=%%MAPS_VERSION%%

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
export LD_LIBRARY_PATH=$MAPS_LIB:$LD_LIBRARY_PATH
#
# Now start the the daemon
#
java -classpath $CLASSPATH -Djava.security.auth.login.config=$MAPS_CONF/jaasAuth.config -DMAPS_HOME=$MAPS_HOME io.mapsmessaging.MessageDaemon

