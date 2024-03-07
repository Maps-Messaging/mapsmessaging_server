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
## SSL/TLS and DTLS Configuration Guide

This document provides an overview of the configuration settings available for setting up SSL/TLS and DTLS security for your application. The configuration is defined within a `config.yaml` file, which outlines the necessary parameters for both `keyStore` and `trustStore`, among other settings.

### Configuration Structure

The configuration file is structured under the `security` key, with separate subsections for `tls` (SSL/TLS configuration) and `dtls` (DTLS configuration). Each section allows you to define parameters related to the `keyStore` and `trustStore`, as well as settings specific to client certificate requirements.

#### Common Configuration Parameters

Both SSL/TLS and DTLS configurations share a common set of parameters, detailed below:

```YAML
  - clientCertificateRequired: Boolean indicating whether a client certificate is required for connection. Set to `false` by default.
  - clientCertificateWanted: Boolean indicating whether a client certificate is requested but not required. Set to `false` by default.
```

##### CRL Specific (Optional)
```YAML
- crlUrl: URL to the Certificate Revocation List (CRL). Optional, but if supplied, will load the CRL to confirm that the certificates are not revoked.
- crlInterval: Time in seconds to reload the CRL. Optional.
```

#### KeyStore Configuration

The `keyStore` section allows configuration of the keystore used for SSL/TLS or DTLS. Available parameters include:

```YAML
  - type: Type of the KeyStore (e.g., `JKS`, `PKCS11`, `PKCS12`, `JCEKS`, `BKS`, `UBER`). Required.
  - path: File path to the keystore file. Required for standard KeyStore types except `PKCS11`.
  - passphrase: Password for accessing the keystore. Required.
  - managerFactory: The `KeyManagerFactory` algorithm name (e.g., `SunX509`). Required.
  - providerName: Security provider name (e.g., `SunJSSE`). Typically not required for standard keystores.
  - alias: Alias name of the server certificate in the keystore. Optional.
```
##### PKCS11 Specific Settings (Optional)
```YAML
    - provider: Specifies the security provider, e.g., `SunPKCS11` for PKCS11 keystores.
    - config: File path to the PKCS11 configuration file.
```

#### TrustStore Configuration

The `trustStore` section configures the truststore for SSL/TLS or DTLS. Parameters mirror those of the `keyStore`, with the addition of `provider` and `config` for PKCS11 specific settings.

#### Note on Using JVM Defaults

When configuring both the `keyStore` and `trustStore`, if only the `passphrase` is supplied without specifying a `path`, the configuration utilizes the JVM's default keystore and truststore. This behavior allows for a simplified configuration when the defaults provided by the JVM are sufficient for the application's security requirements.

For the `trustStore`, this means that if the `path` is not specified and a `passphrase` is provided, the JVM will use its default truststore (typically located at `<JAVA_HOME>/lib/security/cacerts` or a similar path depending on the JVM version and vendor) with the supplied passphrase. If no passphrase is supplied, the default truststore passphrase will be attempted.

Similarly, for the `keyStore`, omitting the `path` but providing a `passphrase` instructs the application to attempt to use the JVM's default keystore with the provided passphrase. This approach is particularly useful for applications that require minimal deviation from the JVM's built-in security settings.

It's essential to ensure that the default JVM keystore and truststore configurations meet your application's security requirements when relying on this behavior.


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
        trustStore:
            type: JKS
            path: my-truststore.jks
            passphrase: password
            managerFactory: SunX509
```

Similar configuration applies for `dtls`, adjusting the `keyStore` and `trustStore` paths and passwords as necessary.

These configuration parameters can be set on an individual interface or in the global section