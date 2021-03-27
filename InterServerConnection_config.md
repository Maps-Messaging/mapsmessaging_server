# Connecting to other Messaging Servers

MapsMessaging has a feature that enables it to directly connect to other messaging servers that support MQTT, AMQP and Stomp. The server can also be extended to use proprietary code to connect to servers that do not support open wire protocol standards.


#Generic Configuration


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


##Adafruit MQTT connection

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
