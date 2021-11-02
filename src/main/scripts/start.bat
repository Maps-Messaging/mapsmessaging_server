REM 
REM  Define the home directory for the messaging daemon
REM 
set MAPS_HOME=<INSERT_PATH_TO_INSTALLATION>
set VERSION=%%MAPS_VERSION%%
set MAPS_LIB=%MAPS_HOME%/lib
set MAPS_CONF=%MAPS_HOME%/conf

REM
REM  From there configure all the paths.
REM 
REM  Note::: The conf directory must be at the start else the configuration is loaded from the jars
REM 
export CLASSPATH=%MAPS_CONF%;%MAPS_LIB%/message_daemon-%VERSION%.jar;"$MAPS_LIB/*"

REM 
REM  Now start the the daemon
REM 
java -classpath $CLASSPATH -Djava.security.auth.login.config=%MAPS_CONF%/jaasAuth.config -DMAPS_HOME=$MAPS_HOME io.mapsmessaging.MessageDaemon
