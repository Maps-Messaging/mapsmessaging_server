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
    # Global configuration for SSL
    # ---------------------------------------------------------------------------------------------------------

    ssl_keyStoreFile: my-keystore.jks
    ssl_keyStorePassphrase: password
    ssl_trustStoreFile: my-keystore.jks
    ssl_trustStorePassphrase: password
    ssl_keyManagerFactory: SunX509
    ssl_trustManagerFactory: SunX509
    ssl_SSLContext: TLS
    ssl_keyStore: JKS
    ssl_trustStore: JKS

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
```

Any configuration specified in the global section is used for all interfaces, unless overwritten by the individual configuration. 
This enables a default value that is useful for a specific site that the built-in defaults are not suited to. 


## Data Section

The data section contains a list of configurations used to build up individual interfaces/ports and protocols as well as any specific interface configuration.
The example below binds ALL address on the machine to port 1883 and offers MQTT only. The authentication to use is 'public'

```yaml
  data:
    -
      url: tcp://0.0.0.0:1883/
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

### SSL Configuration parameters

To configure the SSL interfaces the server needs to know the location, type, passwords of the relevant key stores that it meant to use

```yaml
    ssl_keyStoreFile: <required> - Full path and name of the key store to use
    ssl_keyStorePassphrase:  <required> - Passphrase of the keystore file
    ssl_trustStoreFile:  <required> - Full path and name of the trust store to use
    ssl_trustStorePassphrase:  <required> - Passphrase of the truststore file
    ssl_keyManagerFactory:  <required> - Factory type to use for the key management
    ssl_trustManagerFactory:  <required> - Factory type to use for the trust store
    ssl_SSLContext:  <required> - Context that the SSLEngine is to use this for (TLS/SSL)
    ssl_keyStore:  <required> - The type of keystore (x509, PKCS12 etc)
    ssl_trustStore:  <required> - The type of trust store (x509, PKCS12 etc)
```

These configuration parameters can be set on an individual interface or in the global section