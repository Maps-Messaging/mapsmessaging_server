# Installation of MapsMessaging Server

Firstly download from our nightly builds server 

## Prerequisites 

* JDK 13 or higher - We use and develop with [Zulu-13](https://www.azul.com/downloads/zulu-community/?package=jdk)
* Installation package from the following links

  [message_daemon-1.2.1-install.tar.gz](https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-images-prod/message_daemon-1.2.1-install.tar.gz) \
  [message_daemon-1.2.1-install.zip](https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-images-prod/message_daemon-1.2.1-install.zip)

## Linux / OS-X

Once you have downloaded the install image simply

### installation

```shell
cd /opt
tar -xvzf message_daemon-1.1-SNAPSHOT-install.tar.gz
```

### Starting
This will create a directory under /opt with the installation, then to start the server
```shell
cd  message_daemon-1.1-SNAPSHOT
chmod +x bin/start.sh
nohup ./bin/start.sh &
```

This will start the server with the following configuration

* MQTT on port 1883 
* AMQP on port 5672
* MQTT-SN on port 1884
* Stomp on port 8675

Please note: <u>NO</u> authentication is configured by default

### Modifying installation path

The installation package assumes the installation directory is /opt if this is not the case, then a simple edit in the start.sh and change the line

Change the following
```shell
#
# Define the home directory for the messaging daemon
#
export MAPS_HOME=<your path to the installation here>
export MAPS_LIB=$MAPS_HOME/lib
export MAPS_CONF=$MAPS_HOME/conf
```

To 

```shell
#
# Define the home directory for the messaging daemon
#
export MAPS_HOME=/opt/message_daemon-1.1-SNAPSHOT
export MAPS_LIB=$MAPS_HOME/lib
export MAPS_CONF=$MAPS_HOME/conf
```

Then start as previously shown.

### Shutting down

To stop the server simply remove the pid file in the home directory

```shell
rm /opt/message_daemon-1.1-SNAPSHOT/pid
```
The server will then perform a graceful shutdown