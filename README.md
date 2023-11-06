# MAPS Messaging Server

[MAPS (Multi Adapter and Protocol Standards) Messaging Server](https://www.mapsmessaging.io/) addresses the complexities of IoT communication by supporting a wide range of protocols, ensuring compatibility and future-proof integration.

## Introduction
Wire protocol standardization has promised interoperability and flexibility in IoT messaging. However, with the evolution of protocols, achieving this seamless interoperability has become challenging. MAPS Messaging Server bridges this gap by providing comprehensive support for multiple protocols and their versions, thus facilitating a unified messaging fabric.

## Features
- **Wide Protocol Support:** For a full list, see [supported protocol versions](https://www.mapsmessaging.io/protocol_support.html).
- **Inter-Server Communication:** Directly publish or subscribe to data on other messaging servers. [Learn more](https://www.mapsmessaging.io/InterServerConnection_config.html).
- **Advanced Filtering:** Utilize [JMS Selector](https://github.com/Maps-Messaging/jms_selector_parser) for fine-grained message control.
- **Extensibility:** Easily integrate new or proprietary protocols.
- **Payload Transformation:** Configurable translations, such as XML to [JSON](https://github.com/Maps-Messaging/mapsmessaging_server/tree/main/src/main/java/io/mapsmessaging/api/transformers).
- **Namespace Partitioning:** Distinct namespaces for users and groups to enhance security.
- **Comprehensive Management:** JMX, JMX - RestAPI via [Jolokia](https://jolokia.org/), and web-based management with [hawtio](https://hawt.io/).
- **RestAPI Management:** Simple interface to manage connections, destinations, and schemas.

## Advanced Features

- **Schema Repository:** Centralized management for data integrity and format consistency. [More info](https://github.com/Maps-Messaging/schemas).
- **Direct Device Integration:** Offers direct communication with I2C, 1-Wire, and SPI devices, mapping interactions to topic namespaces. [More info](https://github.com/Maps-Messaging/device_integration)
- **Versatile Authentication:** Supports a variety of mechanisms, including `.htpasswd`, LDAP, JWT, Auth0, and more. [More info](https://github.com/Maps-Messaging/authentication_library)
- **Cloud-Native Event Storage:** Event data at rest can be pushed to S3 buckets for long-term storage. [More info](https://github.com/Maps-Messaging/dynamic_storage)
- **Event Filtering:** Advanced filtering between MAPS-servers or other servers with filtering capabilities, ensuring only relevant data is transmitted. [More info](https://github.com/Maps-Messaging/jms_selector_parser)
- **Robust Logging:** Utilizes Logback for comprehensive and customizable logging configurations.  [More info](https://github.com/Maps-Messaging/simple_logging)
- **Non-Blocking Async Engine:** Employs a highly efficient, non-blocking asynchronous engine and network layers, ensuring minimal locking and maximal concurrency for superior performance. [Learn more](https://github.com/Maps-Messaging/non_block_task_scheduler).
- **LoRa Support:** Native support for LoRa devices, enabling direct communication without requiring a LoRaWAN network.
- **Semtech Gateway Compatibility:** Acts as a LoRaWAN gateway in conjunction with Semtech's technology, expanding the server's IoT ecosystem.
- **mDNS Discovery:** Provides service discovery via mDNS, with future enhancements planned for automatic connection and namespace/schema propagation.
- **Security Domains:** Configurable security domains allow for tailored authentication and authorization on a per-adapter/protocol basis.
- **Flexible Configuration:** Supports configuration through both Consul and file-based setups, catering to various deployment environments.

## Getting Started: "Hello World" Example

This example demonstrates a simple publish/subscribe scenario using the MQTT protocol.

### Starting the Server

Make sure the server is running and listening for MQTT connections on port 1883 (default for MQTT).

### Producer: Sending a Message

```java
// Java-based pseudocode for an MQTT producer
MqttClient producer = new MqttClient("tcp://localhost:1883", "producer");
producer.connect();
MqttMessage message = new MqttMessage("Hello, World!".getBytes());
producer.publish("test/topic", message);
producer.disconnect();
```

### Consumer: Receiving a Message
```java
// Java-based pseudocode for an MQTT consumer
MqttClient consumer = new MqttClient("tcp://localhost:1883", "consumer");
consumer.connect();
consumer.subscribe("test/topic");
consumer.setMessageCallback((topic, msg) -> System.out.println(new String(msg.getPayload())));
// Keep the consumer running to listen for messages
```
Replace localhost with the server's IP address if running remotely. Ensure the topic names match in both producer and consumer.

## Contributing
We welcome contributions! Please refer to our contribution guidelines for how you can participate in making MAPS Messaging Server even better.

## License

The Maps Messaging Server is dual-licensed under the Mozilla Public License Version 2.0 (MPL 2.0) and the Apache License 2.0 with Commons Clause License Condition v1.0.

Under the MPL 2.0 license, the software is provided for use, modification, and distribution under the terms of the MPL 2.0.

Additionally, the "Commons Clause" restricts the selling of the software, which means you may not sell the software or services whose value derives entirely or substantially from the software's functionality.

For full license terms, see the [LICENSE](LICENSE) file in the repository.


[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=Maps-Messaging_mapsmessaging_server)




