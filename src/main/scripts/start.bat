REM 
REM  Define the home directory for the messaging daemon
REM 
set MAPS_HOME=<INSERT_PATH_TO_INSTALLATION>
set MAPS_LIB=$MAPS_HOME/lib
set MAPS_EXT=$MAPS_HOME/ext
REM 
REM  From there configure all the paths.
REM 
REM  Note::: The conf directory must be at the start else the configuration is loaded from the jars
REM 
export CLASSPATH=.:$MAPS_HOME/conf/:$MAPS_LIB/message_daemon-1.1-SNAPSHOT.jar:"$MAPS_LIB/*":"$MAPS_EXT/*"

REM 
REM  Now start the the daemon
REM 
java -classpath $CLASSPATH -Djava.security.auth.login.config=$MAPS_HOME/conf/jaasAuth.config -DMAPS_HOME=$MAPS_HOME org.maps.messaging.MessageDaemon
