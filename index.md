# Welcome to MapsMessaging

This project is in response to the growing deployment of IoT networks, and the challenge to connect large numbers of low power, low resource devices to large enterprise level applications.
With the evolution of various IoT protocols to handle the different requirements for messaging the integration and message flow to applications that need the data has become complex and in some cases involves multiple servers and protocols just to receive a sensor reading.

### Installation

Installation instructions can be found [here](installation.md)

### Protocols Supported

With the MapsMessaging Daemon it natively supports the following protocols 

* MQTT (v3.1, v3.1.1, v5)
* MQTT-SN (1.2)
* AMQP 1.0 - (JMS over AMQP)
* Stomp (1.1, 1.2)

Independently of where messages arrive from all clients on all protocols and interact with each other.  

All these wire protocols can be run over 

  * tcp
  * ssl
  * ws
  * wss 
  * LoRa hardware - [LoRa Device](LoRaDevice_config.md) or by [LoRa serial Gateway](LoRaSerial_config.md)
  * Serial devices

As well as the standard TCP/UDP connections the server also supports LoRa devices on a raspberryPi enabling direct messaging between a LoRa sensor and the server using MQTT-SN, removing the requirement for another hop between sensor and messaging server.

#### Protocol Extensibility
The protocol library is extendable and if an organisation has an internal proprietary protocol it can be added to the server as a native protocol.

### Inter-server Connections

MapsMessaging can be configured to ingest events from other remote servers or to publish events to other remote servers. This removes the requirement for additional agents to move messages from 1 server to another. The configuration for these can be found in [NetworkConnectionManager.yaml](InterServerConnection_config.md) file.

#### Native Server Connections
The server also supports connections to other servers, such that MapsMessaging can connect to other MQTT, AMQP, Stomp servers and pull data from them and publish to local resources or it can be configured to push data from local resources to remote servers removing any need for another application to enable this message flow.
These connections support the JMS Selector syntax on events coming in or out of the server enabling only events that have interest to be processed.

#### Pluggable Server Connections
With anything within MapsMessaging there is the ability to provide custom connection code to enable the server to connect to other messaging servers that do not comply with open messaging servers. Enabling the events to flow to enterprise messaging servers that other applications are currently bound to. There is an [example](https://github.com/Maps-Messaging/mapsmessaging_server/tree/main/src/examples/java/io/mapsmessaging/network/protocol/impl/apache_pulsar) of this in the code base using the native [Apache Pulsar](https://pulsar.apache.org/) client to send and receive events.   


### Modularisation

As part of the architecture design for the server the approach was taken that if a module could be reused or would be helpful to the OSS eco-system then it would be pulled out of the Daemon, and a new project created for it. To this end we currently have

* [JMS Selector Parser module](https://github.com/Maps-Messaging/jms_selector_parser) 
  Is a standalone JMS Selector 2 pass parser that produces a resultant parser that supports a generic Key/Value object to resolve the selector. It can be used in Java Collection.streams, is thread safe and can be used in parallelStreams or anywhere you need a JMS Selector syntax parser / filter.

* [Non Blocking Task Scheduler](scheduler/overview.md) 
  The MapsMessaging engine is a non locking, non-blocking messaging engine, to achieve this we have implemented a task queue scheduler that facilitates low latency task pass over and internal integrity checks that the task is on the correct queue (Optional).
  
* [Naturally Ordered Long Collections](https://github.com/Maps-Messaging/naturally_ordered_long_collections) 
  The MapsMessaging engine needs to keep track of which message a client has interest in or which message is awaiting a commit from a client. For it to do this fast, recoverable and small in footprint, we have implemented a collection based on the java BitSet that can be read/written to file, can perform bitwise operations for speed when collection APIs like addAll(), removeAll() etc are called making it a very fast, compact naturally ordered collection. It also supports priority based queues.

To access these via maven simply add the following to your pom.xml file.
```xml
    <!-- MapsMessaging jfrog server --> 
    <repository>
      <id>mapsmessaging.io</id>
      <name>artifactory-releases</name>
      <url>https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-mvn-prod</url>
    </repository>
```

The nightly installation builds can be found here

[message_daemon-1.1-SNAPSHOT-install.tar.gz](https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-images-prod/message_daemon-1.1-SNAPSHOT-install.tar.gz) \
[message_daemon-1.1-SNAPSHOT-install.zip](https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-images-prod/message_daemon-1.1-SNAPSHOT-install.zip)


The MapsMessaging repository is [here](https://mapsmessaging.jfrog.io/)



