#
# Define the home directory for the messaging daemon
#
export MAPS_HOME=<INSERT_PATH_TO_INSTALLATION>
export MAPS_LIB=$MAPS_HOME/lib
export MAPS_EXT=$MAPS_HOME/ext
#
# From there configure all the paths.
#
# Note::: The conf directory must be at the start else the configuration is loaded from the jars
#
export CLASSPATH=.:$MAPS_HOME/conf/:$MAPS_LIB/message_daemon-1.1-SNAPSHOT.jar:"$MAPS_LIB/*":"$MAPS_EXT/*"

#
# Now start the the daemon
#
java -classpath $CLASSPATH -Djava.security.auth.login.config=$MAPS_HOME/conf/jaasAuth.config -DMAPS_HOME=$MAPS_HOME org.maps.messaging.MessageDaemon
