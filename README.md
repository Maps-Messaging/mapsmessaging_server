# MAPS Messaging Server

[MAPS(Multi Adapter and Protocol Standards)messaging server](https://www.mapsmessaging.io/)


## Introduction
Wire protocol standardisation for messaging has enabled an unprecidented interoperability for both clients and servers. It has come with the promise of no vendor lock in, healthy competition and the ability to swap out clients and servers as requirements change. While this is a noble approach, and has to some extent been fulfilled, there is an increasing realisation that as protocols evolve or new protocols are ratified the promise of interoperability is not attainable.
Take for example MQTT [V3.1.1](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html) and [V5](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html), while evolution of the same protocol the functionality being offered in V5 far exceeds what was available in V3. Add to this AMQP V1.0, which is gaining ground in IoT, when trying to design a coherent messaging fabric it requires multiple messaging servers with standalone integration systems or messaging servers with plugins that have limited support of the protocol. This results in complex deployments, rigid client and server deployments and not achieving the utopia that open wire protocols were promising to deliver.

## Features
* MapsMessaging supports the MQTT (3.1, 3.1.1, 5.0), MQTT-SN (1.2, 2.0), AMQP (1.0) (JMS over AMQP), STOMP, CoAP. For the complete list
  of [supported protocol versions](https://www.mapsmessaging.io/protocol_support.html).

* It also supports connections to other messaging servers that support MQTT, Stomp and AMQP allowing the server to either publish data to the remote servers or subscribe to data on
  the remote servers. Removing the need to add extra hops in message delivery systems. More information can be
  found [here](https://www.mapsmessaging.io/InterServerConnection_config.html)

* [JMS Selector](https://github.com/Maps-Messaging/jms_selector_parser) based filtering support for AMPQ_JMS, Stomp, MQTT V5.

* Designed to be able to plug in new protocols or add proprietary protocols to enable older closed systems to be able to interact with IoT systems

* Pluggable payload translations can be configured on the server such that in-flight data can be transmuted to other formats. For example data may come in as XML but be translated to [JSON](https://github.com/Maps-Messaging/mapsmessaging_server/tree/main/src/main/java/io/mapsmessaging/api/transformers) as it arrives.

* Support for a partitioned namespace for users and groups

* Support for JMX management, RestAPI via [Jolokia](https://jolokia.org/) as well as web based management via [hawtio](https://hawt.io/)
* Simple Rest API to manage interfaces, destinations and schemas


[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=Maps-Messaging_mapsmessaging_server)



