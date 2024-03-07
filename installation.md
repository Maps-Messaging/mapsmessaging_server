# Installation of MapsMessaging Server

Ensure you have JDK 13 or higher installed. You can download a suitable JDK from [Zulu-13](https://www.azul.com/downloads/zulu-community/?&version=java-13).

## Downloading the Server

Download the MapsMessaging Server from the [Releases](https://github.com/Maps-Messaging/mapsmessaging_server/releases) section of the mapsmessaging_server GitHub repository.

- For the latest stable release, look for the release version (e.g., "3.3.6").
- For the latest development version, download the snapshot (e.g., "3.3.7-SNAPSHOT").

## Installation on Linux / OS-X

After downloading, execute the following commands:

```bash
cd /opt
tar -xvzf message_daemon-<VERSION>.tar.gz
ln -s message_daemon-<VERSION> message_daemon
```
Replace `<VERSION>` with the downloaded version number, such as `3.3.4` for a release or `3.3.5-SNAPSHOT` for a snapshot.

## Starting the Server

Navigate to the symlinked directory and start the server with:

```bash
cd message_daemon
chmod +x bin/start.sh
nohup ./bin/start.sh &
```

## Default Port Configuration

The server starts with the following default ports for each protocol:

| Protocol  | Port(s)       |
|-----------|---------------|
| MQTT      | 1883, 2883    |
| MQTT-SN   | 1884, 2442    |
| CoAP      | 5683          |
| AMQP      | 5672          |
| Stomp     | 8675          |
| RestAPI   | 8082          |
| Hawtio    | 8080          |


**Note:** No authentication is configured by default.

## Customizing the Installation Path

If you need to customize the installation path, modify the `MAPS_HOME` variable in the `start.sh` script accordingly.

## Shutting Down the Server

To stop the server, remove the pid file with:

```bash
rm /opt/message_daemon/pid
```

The server will shut down gracefully.
