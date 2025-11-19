# Release Notes for development
_Generated: 2025-11-14T00:04:02.126Z_

## Overview of new features

- MQTT V5 and transactional communication support with remote servers
- GeoHash routing
- Any configured schema can be converted to any other schema ( limitations apply )
- Inter server fall back routing, allows the server to move to a different server if a connection fails


### [MSG-10](https://mapsmessaging.atlassian.net/browse/MSG-10) — Complete the MQTT-V5 connection logic

The server can now support client side MQTT V5 protocol messages to establish links with other servers


### [MSG-11](https://mapsmessaging.atlassian.net/browse/MSG-11) — Add config to select v3, v4, or v5

By specifying mqtt-v3 or mqtt-v5 for the protocol on the inter server links the user can select which protocol version to use. By specifying just mqtt then the server will attempt to connect using either of them and once connected will use that version


```
      -
          name: zeropi04
          url: tcp://10.140.62.208:1883/
          protocol: mqtt  # will use both
          transformation: "Message-Raw"
          remote:
            sessionId: local-Pull2-mqtt5
            username: matthew
            password: doesntmatter
          links:
            -
              direction: pull
              local_namespace: /zeropi04/
              remote_namespace: "/device/#"
              include_schema: true


      -
          name: zeropi04
          url: tcp://10.140.62.208:1883/
          protocol: mqtt-v3 # will only use MQTT V3.1(.1)
          transformation: "Message-Raw"
          remote:
            sessionId: local-Pull2-mqtt5
            username: matthew
            password: doesntmatter
          links:
            -
              direction: pull
              local_namespace: /zeropi04/
              remote_namespace: "/device/#"
              include_schema: true              


      -
          name: zeropi04
          url: tcp://10.140.62.208:1883/
          protocol: mqtt-v5  # will only use MQTT V5
          transformation: "Message-Raw"
          remote:
            sessionId: local-Pull2-mqtt5
            username: matthew
            password: doesntmatter
          links:
            -
              direction: pull
              local_namespace: /zeropi04/
              remote_namespace: "/device/#"
              include_schema: true              
```


Also adding, that now we have discovery working the plan is to integrate the discovery manager and the Network Connection Manager so that when the server discovers another messaging server it will pass the details to the Network Connection Manager. If it is configured to be able to auto connect then the server will use the config to establish a connection and either pull/push data depending on the config.
 
So you can have a default config like



```
   name: {{hostname}}
   url: {{remote_url}}
   sessionId: {{auto-generate}}
   protocol: {{protocol_defined_in_discovery}}
   username: global_username
   password: global_token
   links:
     -
       direction: pull
       remote_namespace: #
       local_namespace: /{{hostname}}/
```


This would automatically connect to the discovered host, use the protocol that was being advertised with the username/password being that of the global one for the server. It would then subscribe to all topics and map them to the local namespace under the hostname.
On a maps server it would also get the schemas for the topics and map them locally. Obviously this is a simple example of mapping everything but we can also add selectors, or break up the namespace. If we know its a maps server we can get a list of topics without actually subscribing to the server allowing the server to manage an “has interest” subscription.


### [MSG-12](https://mapsmessaging.atlassian.net/browse/MSG-12) — Add support for transactional event passing

QoS can now be set on each “link” configuration in the NetworkConnectionManager.yaml

If the protocol doesn’t support the 3 different QoS then anything above 0 will be treated as transactional
```
        links:
          -
            direction: pull
            local_namespace: /zeropi05/
            remote_namespace: "/device/#"
            include_schema: true
            qos: 2  # 0, 1 or 2
```



### [MSG-124](https://mapsmessaging.atlassian.net/browse/MSG-124) — Implement a geoHash message router

**Description**
Used for inter server connections to enable events to be routed to a geoHash topic name


### [MSG-134](https://mapsmessaging.atlassian.net/browse/MSG-134) — Event looping occurs when multiple joins to remote and local loop configured

The issue is that the message being delivered via the loop mechanism is being sent back into the engine without a clone being made, this causes the original topic/message to reset and send again, causing a messaging storm.

The loop has been fixed to stop this and a fatal error induced if it happens again


### [MSG-135](https://mapsmessaging.atlassian.net/browse/MSG-135) — Docker script fails to start the server

The change to the lib layout resulted in the startDocker.sh unable to load the main Maps jar. The start script is now inline with the main start.sh script

Do not move the 'www' directory to data, leave in MAPS_HOME


### [MSG-142](https://mapsmessaging.atlassian.net/browse/MSG-142) — Configure a Schema Repository for the server

The server schema support has been modified to remove the storage of the physical schema with the destination storage and into a Schema Repository. Currently only file is supported, in future this will encompase XRegistry and other Maps rest servers.


Configurable schema repo ( currently file only supported ) when we get XRegistry we can add it or others
Upgrade from previous version of schema
Support for schema to schema data conversion
regex topic name mapping for schemas, ( avoids adding a schema for each topic can map topics to a single schema )
More validation and tests of data coming in to the schema
More validation and tests in the server around schemas
New SchemaManager.yaml file to configure the schema repository


### [MSG-40](https://mapsmessaging.atlassian.net/browse/MSG-40) — Improve storage behavior when running OOM (literally or via corrupt file)

Fixed issues with the storage layer when changing configuration resulted in corruption of existing stores for that destination
Add better exception handling in low memory environments
Rename corrupted files for later processing


### [MSG-73](https://mapsmessaging.atlassian.net/browse/MSG-73) — Concurrent modification logged

The concurrent exception arose while the restapi attempted to get the number of events for a specific destination. This was a long running function. The function is now very lightweight and avoids the same paths as an active server


### [MSG-95](https://mapsmessaging.atlassian.net/browse/MSG-95) — Create release notes automatically

Release notes are not auto generated from github and jira




## Miscellaneous (Fixes - 3rd party library updates)
- `f7670fab` keep the www as part of MAPS_HOME —  (2025-11-14)
- `43ee86be` Fix initial boot issue for JWT Token secret generation —  (2025-11-13)
- `c1656f97` check and do no throw exceptions if no hardware supported —  (2025-11-13)
- `a226d846` ensure the Schema getAll returns all —  (2025-11-13)
- `42d90c18` update snapshot library versions, for initial testing —  (2025-11-12)
- `13378814` Bump logback.version from 1.5.20 to 1.5.21 — dependabot[bot] (2025-11-10)
- `7655e4ab` Bump software.amazon.awssdk:bom from 2.37.1 to 2.38.2 — dependabot[bot] (2025-11-10)
- `15601b7a` Bump io.nats:jnats from 2.23.0 to 2.24.0 — dependabot[bot] (2025-11-10)
- `91faaa6f` Bump org.springframework:spring-websocket from 6.2.11 to 6.2.12 — dependabot[bot] (2025-11-10)
- `aa9ebe5d` Include the release notes in the installation —  (2025-11-10)
- `947ac80d` code cleanup —  (2025-11-10)
- `df053571` Wild card subscription on changing destinations —  (2025-11-10)
- `985d1f6f` Bump software.amazon.awssdk:cognitoidentityprovider — dependabot[bot] (2025-11-07)
- `daa41aee` Bump com.fasterxml.jackson.core:jackson-databind from 2.20.0 to 2.20.1 — dependabot[bot] (2025-11-07)
- `4cf6a837` Bump org.springframework.boot:spring-boot-starter-websocket — dependabot[bot] (2025-11-07)
- `303022f3` Bump org.apache.maven.plugins:maven-dependency-plugin — dependabot[bot] (2025-11-07)
- `4065b73e` Bump org.graalvm.buildtools:graalvm-reachability-metadata — dependabot[bot] (2025-11-05)
- `82663e40` Bump com.fazecast:jSerialComm from 2.11.2 to 2.11.4 — dependabot[bot] (2025-11-05)
- `404624b2` Bump software.amazon.awssdk:cognitoidentity from 2.37.4 to 2.37.5 — dependabot[bot] (2025-11-05)
- `a2a52b7d` Bump io.swagger.core.v3:swagger-jaxrs2-servlet-initializer-v2-jakarta — dependabot[bot] (2025-11-04)
- `110cf560` Bump jersey.version from 3.1.11 to 4.0.0 — dependabot[bot] (2025-11-04)
- `15d24d74` Bump software.amazon.awssdk:cognitoidentity from 2.37.2 to 2.37.4 — dependabot[bot] (2025-11-04)
- `0b4ed26c` Bump software.amazon.awssdk:cognitoidentityprovider — dependabot[bot] (2025-11-04)
- `85714343` Rollback schema changes and fix JUnit library issues —  (2025-11-04)
- `171235c5` Bump org.glassfish.tyrus.bundles:tyrus-standalone-client — dependabot[bot] (2025-11-03)
- `6db50857` Bump com.fazecast:jSerialComm from 2.11.2 to 2.11.3 — dependabot[bot] (2025-11-03)
- `1bac9cfe` Bump com.amazonaws:aws-java-sdk-cognitoidp from 1.12.791 to 1.12.793 — dependabot[bot] (2025-11-03)
- `a4e3bc84` Bump software.amazon.awssdk:cognitoidentityprovider — dependabot[bot] (2025-11-03)
- `54d83556` Bump software.amazon.awssdk:cognitoidentity from 2.34.5 to 2.37.2 — dependabot[bot] (2025-10-31)
- `824d5a11` Bump software.amazon.awssdk:cognitoidentityprovider — dependabot[bot] (2025-10-31)
- `1df63609` Bump org.quartz-scheduler:quartz from 2.5.0 to 2.5.1 — dependabot[bot] (2025-10-31)
- `37d08137` Bump com.fasterxml.jackson.dataformat:jackson-dataformat-xml — dependabot[bot] (2025-10-31)
- `c511cb01` Bump org.jacoco:jacoco-maven-plugin from 0.8.13 to 0.8.14 — dependabot[bot] (2025-10-30)
- `0b1d87a4` Bump software.amazon.awssdk:bom from 2.36.3 to 2.37.1 — dependabot[bot] (2025-10-30)
- `5cdb546e` Bump io.swagger.core.v3:swagger-jaxrs2-jakarta from 2.2.39 to 2.2.40 — dependabot[bot] (2025-10-30)
- `c08821b6` Bump logback.version from 1.5.19 to 1.5.20 — dependabot[bot] (2025-10-30)
- `e37af91e` allow the config to set the default schema on destinations —  (2025-10-30)
- `c6592be7` Bump software.amazon.awssdk:bom from 2.34.5 to 2.36.3 — dependabot[bot] (2025-10-28)
- `3caf527f` Bump com.hivemq:hivemq-mqtt-client from 1.3.9 to 1.3.10 — dependabot[bot] (2025-10-24)
- `5530a04e` Bump org.junit.jupiter:junit-jupiter-engine from 5.13.4 to 6.0.0 — dependabot[bot] (2025-10-24)
- `7bdfd6a7` Bump org.owasp:dependency-check-maven from 12.1.6 to 12.1.8 — dependabot[bot] (2025-10-24)
- `2c3b7de1` Loop connection failed —  (2025-10-24)
- `52db3cb7` Move release notes to it's own repo —  (2025-10-21)
- `688c5688` Release note autogeneration will look for Jira comments with —  (2025-10-21)
- `b6d6d66e` Bump org.codehaus.mojo:exec-maven-plugin from 3.5.1 to 3.6.2 — dependabot[bot] (2025-10-20)
- `3d6960ff` Fix publish destination name lookup —  (2025-10-14)
- `d0eca7a8` Disable status updates —  (2025-10-14)
- `b14174ec` Bump io.swagger.core.v3:swagger-jaxrs2-servlet-initializer-v2-jakarta — dependabot[bot] (2025-10-13)
- `137c3ff0` Bump io.swagger.core.v3:swagger-jaxrs2-jakarta from 2.2.37 to 2.2.39 — dependabot[bot] (2025-10-13)
- `db55a83c` remove doc files —  (2025-10-13)
- `2f7aa449` Server Link Manager —  (2025-10-10)
- `5ff18cdc` Server Link Manager —  (2025-10-10)
- `e1cbe5cd` Contribution document —  (2025-10-10)
- `629ee795` Configure the orchistrator correctly to monitor and change links —  (2025-10-10)
- `eb16d5a2` adjust for jdk 21 / jre 21 —  (2025-10-10)
- `e5b2450d` wire in the use of the orchestrator to manage the switching —  (2025-10-10)
- `dafabdf3` posthog monitoring additions —  (2025-10-09)
- `5fa5cae2` posthog monitoring additions —  (2025-10-09)
- `9aeb469e` force updates —  (2025-10-09)
- `cb24a3aa` fix tests —  (2025-10-09)
- `84c9e92a` support for full CloudEvent schame / url and encoding new rest api to get the raw schema config ( no auth on this required ) additional work on link management. Still more to come —  (2025-10-09)
- `6dc253ec` initial cloudEvent support —  (2025-10-09)
- `55e19214` initial route management and selection —  (2025-10-08)
- `6d719259` initial version of posthog installation tracking —  (2025-10-06)
- `3f68d864` Bump logback.version from 1.5.18 to 1.5.19 — dependabot[bot] (2025-09-30)
- `ae974cf5` make the arm version the same —  (2025-09-30)
- `ad8adf1d` docker does not seem to support IPv6 so use IPv4 —  (2025-09-30)
- `015cb08b` fix docker —  (2025-09-30)
- `73eeeb6a` fix docker —  (2025-09-30)
- `d656b185` load all jars —  (2025-09-30)
- `dade72b5` Bump org.projectlombok:lombok from 1.18.40 to 1.18.42 — dependabot[bot] (2025-09-29)
- `67501067` Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.11.3 to 3.12.0 — dependabot[bot] (2025-09-29)
- `cf2efb77` Bump software.amazon.awssdk:cognitoidentity from 2.34.3 to 2.34.5 — dependabot[bot] (2025-09-29)
- `a76b168f` Bump software.amazon.awssdk:cognitoidentityprovider — dependabot[bot] (2025-09-29)
- `8e412386` Bump org.springframework:spring-websocket from 6.2.10 to 6.2.11 — dependabot[bot] (2025-09-29)
- `fc6078ee` Bump org.apache.maven.plugins:maven-compiler-plugin — dependabot[bot] (2025-09-29)
- `c6734ed4` Bump org.owasp:dependency-check-maven from 12.1.3 to 12.1.6 — dependabot[bot] (2025-09-29)
- `e0e36b91` Bump com.amazonaws:aws-java-sdk-cognitoidp from 1.12.788 to 1.12.791 — dependabot[bot] (2025-09-29)
- `fb78a465` Bump org.apache.qpid:qpid-jms-client from 2.8.0 to 2.9.0 — dependabot[bot] (2025-09-29)
- `a2a8018c` Bump software.amazon.awssdk:bom from 2.32.31 to 2.34.5 — dependabot[bot] (2025-09-29)
- `da8538fe` Bump io.swagger.core.v3:swagger-jaxrs2-servlet-initializer-v2-jakarta — dependabot[bot] (2025-09-29)
- `fea88669` Bump org.graalvm.buildtools:graalvm-reachability-metadata — dependabot[bot] (2025-09-29)
- `8d05f223` fix broken links in read me —  (2025-09-29)
- `877d82ec` Bump io.swagger.core.v3:swagger-jaxrs2-jakarta from 2.2.36 to 2.2.37 — dependabot[bot] (2025-09-26)
- `20896b72` Bump org.glassfish.jaxb:jaxb-runtime from 4.0.5 to 4.0.6 — dependabot[bot] (2025-09-26)
- `95569454` Bump org.springframework.boot:spring-boot-starter-websocket — dependabot[bot] (2025-09-26)
- `1f42a4ab` Bump org.graalvm.buildtools:native-maven-plugin from 0.11.0 to 0.11.1 — dependabot[bot] (2025-09-26)
- `a2a13b0b` Bump jakarta.xml.bind:jakarta.xml.bind-api from 4.0.2 to 4.0.4 — dependabot[bot] (2025-09-26)
- `ccfc6300` Bump io.nats:jnats from 2.21.5 to 2.23.0 — dependabot[bot] (2025-09-25)
- `66cbad57` Bump software.amazon.awssdk:cognitoidentity from 2.33.8 to 2.34.3 — dependabot[bot] (2025-09-25)
- `f6530b70` Bump com.google.code.gson:gson from 2.13.1 to 2.13.2 — dependabot[bot] (2025-09-15)
- `a1e5c00f` Bump org.apache.maven.plugins:maven-surefire-plugin from 3.5.3 to 3.5.4 — dependabot[bot] (2025-09-15)
- `7152d5b5` Bump jakarta.activation:jakarta.activation-api from 2.1.2 to 2.1.4 — dependabot[bot] (2025-09-15)
