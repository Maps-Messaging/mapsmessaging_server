# NetworkManager.yaml

This yaml configuration file sets up the servers interface, ports, protocols and security that the server is to offer. The configuration is broken down into multiple sections

The configuration has 2 main sections, Global and Data. The Global section is reserved to define defaults for the specific configuration supplied in the Data section.


## Global Section

The global section is just a list of key/value pairs that can be used to
* Override the servers defaults
* Specify site specific configuration for all interfaces

In the example below we can see that the SSL keystores are set up, this configuration will be used for ALL SSL based interfaces unless they are overwritten in the specific SSL configuration.

```yaml
  global:
    # ---------------------------------------------------------------------------------------------------------
    # tcp layer configuration
    # ---------------------------------------------------------------------------------------------------------
    receiveBufferSize: 128000
    sendBufferSize: 128000
    timeout: 60000
    backlog: 100
    soLingerDelaySec: 10
    readDelayOnFragmentation: 100
    enableReadDelayOnFragmentation: true

    # ---------------------------------------------------------------------------------------------------------
    # Global configuration for SSL
    # ---------------------------------------------------------------------------------------------------------
    security:
      tls:
        clientCertificateRequired: false
        clientCertificateWanted: false
        keyStore:
          type: JKS
          path: my-keystore.jks
          passphrase: password
          managerFactory: SunX509
        trustStore:
          type: JKS
          path: my-truststore.jks
          passphrase: password
          managerFactory: SunX509
```

Any configuration specified in the global section is used for all interfaces, unless overwritten by the individual configuration. 
This enables a default value that is useful for a specific site that the built-in defaults are not suited to. 


## Data Section

The data section contains a list of configurations used to build up individual interfaces/ports and protocols as well as any specific interface configuration.
The example below binds ALL address on the machine to port 1883 and offers MQTT only. The authentication to use is 'public'

```yaml
  data:
    -
      url: ws://:::1883/
      name: "MQTT WebSocket Interface"
      protocol : mqtt
      selectorThreadCount : 2
      auth : public
      receiveBufferSize : 1200000
      sendBufferSize    : 1200000
```

Any configuration specified in the global section is used for all interfaces, unless overwritten by the individual configuration.
This enables a default value that is useful for a specific site that the built-in defaults are not suited to. 


### Interface Configuration

Each unique interface that you want to set up needs to have 
* url
* name
* protocol

The other parameters are optional and will be filled in by the servers defaults or any values specified in the 'global' section

```yaml
    -
      url:  <required>
      name:  <required>
      protocol:  <required>

    -
      url: ssl://192.168.1.12:8443/
      name: public facing MQTTS / AMQPS interface
      protocol: mqtt, amqp
```

In the example above, the server will bind to 192.168.1.12 port 8443 and listen for connections from MQTT and AMQP clients.

### TCP Configuration parameters

```yaml
    receiveBufferSize: 128000
    sendBufferSize: 128000
    timeout: 60000
    backlog: 100
    soLingerDelaySec: 10
    readDelayOnFragmentation: 100
    enableReadDelayOnFragmentation: true
```


### Example Configuration

Below is an example configuration snippet for the SSL/TLS setup:

```YAML
security:
   tls:
        clientCertificateRequired: false
        clientCertificateWanted: false
        keyStore:
            type: JKS
            path: my-keystore.jks
            passphrase: password
            managerFactory: SunX509
            store: vault
            vaultAddress: "https://localhost:8200"
            vaultToken: "xxxxxxxxxxxxxxxxxxxxxx" # your token
            secretEngine: "myCertStore"
        trustStore:
            type: JKS
            path: my-truststore.jks
            passphrase: password
            managerFactory: SunX509
```

Similar configuration applies for `dtls`, adjusting the `keyStore` and `trustStore` paths and passwords as necessary.

These configuration parameters can be set on an individual interface or in the global section