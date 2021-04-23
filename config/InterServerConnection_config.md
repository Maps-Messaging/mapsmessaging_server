# NetworkConnectionManager.yaml

MapsMessaging has a feature that enables it to directly connect to other messaging servers that support MQTT, AMQP and Stomp. The server can also be extended to use proprietary APIs to connect to servers that do not support open wire protocol standards.

To configure an inter-server link the first section is to define the link details

* Protocol to use, once of  Stomp | mqtt | AMQP 
* URL for the remote server, tcp | ssl | ws | wss ://hostname:port/
* Username for authentication of the link
* Password for authentication of the link
* SessionId, for MQTT this is mapped to the client ID and is used locally for log messages

For example

```yaml
      - # ---------------------------------------------------------------------------------------------------------
        #  Interface definitions for local loragateway
        # ---------------------------------------------------------------------------------------------------------
        name: loraGateWayServer
        url: tcp://192.168.1.200:1883/
        protocol: mqtt
        username: loraAdmin
        password: password
        sessionId: loraAdminLink
```

The above configuration will connect to host 192.168.1.200 port 1883 and establish a MQTT connection using the supplied username, password and client ID.
At this point we just have a connection, the next part of the configuration is a list of links that contain 

* Local topic name
* Remote topic name
* Direction of message flow, Pull indicates that the local server will pull (subscribe) events from the other server, Push indicates that the local server will send events to the remote server
* Selector to use as a filter for events, This is a standard JMS-Selector string.

```yaml
        links:
          -
            direction: pull 
            remote_namespace: /outdoor/temperature
            local_namespace: /lora/outdoor/temperature
          -
            direction: pull
            remote_namespace: /outdoor/humidity
            local_namespace: /lora/outdoor/humidity
          -
            direction: pull
            remote_namespace: /outdoor/pressure
            local_namespace: /lora/outdoor/pressure
          -
            direction: push
            remote_namespace: /admin/messages
            local_namespace: /lora/admin
```
The above configuration will create 3 subscriptions to the remote server and remote namespace, ( please note for MQTT wildcard subscription is supported )
It will also create a local subscription to /lore/admin and any events will be published to the remote nodes /admin/messages topic.

There can be multiple links defined for a single connection as well as multiple connections to the same remote host.



#Global Configuration

As for all yaml configuration files for MapsMessaging there is a global section that can be used to set defaults for all configured connections. The values are the same as in the [NetworkManager](NetworkManager_Config.md) configuration file.

For example:

```yaml
NetworkConnectionManager:
    global:
        selector_pool_size: 10

      # ---------------------------------------------------------------------------------------------------------
      # tcp layer configuration
      #
        receiveBufferSize: 128000
        sendBufferSize: 128000
        timeout: 60000

        readDelayOnFragmentation: 100
        enableReadDelayOnFragmentation: true

      # ---------------------------------------------------------------------------------------------------------
      # Generic protocol configuration
      #
        serverReadBufferSize:  1M
        serverWriteBufferSize: 1M
        selectorThreadCount  : 5
```


## Generic MQTT Connection

```yaml
    data:
      - # ---------------------------------------------------------------------------------------------------------
        #  Interface definitions for local loragateway
        # ---------------------------------------------------------------------------------------------------------
        name: <name of the connection, meaningful to you>
        url: <url of mqtt server> (tcp|ssl)://<hostname>:<port>/
        protocol: mqtt
        username: <user name to use for the mqtt connection>
        password: <pass word to use for the mqtt connection>
        sessionId: <client id to use for the mqtt connection>
        links:
          -
            direction: (pull|push) 
            remote_namespace: <remote topic name>
            local_namespace: <local topic name>
```

##Adafruit.IO MQTT connection

For up to date information regarding rhe Adafruit MQTT server please check the [Adafruit IO MQTT API](https://io.adafruit.com/api/docs/mqtt.html#adafruit-io-mqtt-api) page regarding SSL, WS etc.

```yaml
    -
        # ---------------------------------------------------------------------------------------------------------
        #  Interface definitions for io.adafruit.com
        # ---------------------------------------------------------------------------------------------------------
        name: adafruit
        url: tcp://io.adafruit.com:1883/
        protocol: mqtt
        sessionId: <Unique MQTT client ID>
        username: <your adafruit username>
        password: <your adafruit password>
        links:
          -
            direction: push
            remote_namespace: <your adafruit username>/feeds/<feed_name>/json
            local_namespace: <local topic to send events from>
```

## The Things Stack MQTT Connection

To connect and receive messages from your LoRaWAN devices that are configured to [The Things Stack](https://www.thethingsindustries.com/) then the configuration is as simple as

```yaml
    data:
        -
            name: The Things Stack
            url: <url to The Things Stack>
            protocol: mqtt
            username: <local user>
            password: <local password>
            sessionId: <local session id to use>
            remote:
                username: <username>@ttn
                password: <password token supplied by The Thing Stack>
                sessionId: <remote session id to use>

            links:
                -
                    direction: pull
                    remote_namespace: '#' // Could be any configured topic 
                    local_namespace: 'ttn/#' // this is the local topic name map. Here we simply publish data below ttn/
```

Documentation on the URLs and remote name space can be found at [The Things Stack documentation pages](https://www.thethingsindustries.com/docs/integrations/mqtt/)