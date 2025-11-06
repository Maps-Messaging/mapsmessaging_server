# MAPS Messaging Server

[MAPS (Multi Adapter and Protocol Standards) Messaging Server](https://www.mapsmessaging.io/) addresses the complexities of IoT communication by supporting a wide range of protocols, ensuring compatibility and future-proof integration.

## Introduction
Wire protocol standardization has promised interoperability and flexibility in IoT messaging. However, with the evolution of protocols, achieving this seamless interoperability has become challenging. MAPS Messaging Server bridges this gap by providing comprehensive support for multiple protocols and their versions, thus facilitating a unified messaging fabric.

## Features
- **Wide Protocol Support:** For a full list, see [supported protocol versions](https://docs.mapsmessaging.io/docs/intro#supported-protocols).
- **Inter-Server Communication:** Directly publish or subscribe to data on other messaging servers. [Learn more](https://docs.mapsmessaging.io/docs/integrations/server/overview).
- **Advanced Filtering:** Utilize [JMS Selector](https://github.com/Maps-Messaging/jms_selector_parser) for fine-grained message control.
- **Extensibility:** Easily integrate new or proprietary protocols.
- **Payload Transformation:** Configurable translations, such as XML to [JSON](https://github.com/Maps-Messaging/mapsmessaging_server/tree/main/src/main/java/io/mapsmessaging/api/transformers).
- **Namespace Partitioning:** Distinct namespaces for users and groups to enhance security.
- **Comprehensive Management:** JMX, JMX - RestAPI via [Jolokia](https://jolokia.org/)
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

## Building the Server

### Prerequisites

- Java 21 or later
- Maven 3.8+
- Node.js LTS (v20+) - Only required if building with UI support

### Standard Build

To build the server without the admin UI:

```bash
# Using the build script
./build.sh

# Or using Maven directly
mvn clean install
```

### Building with Admin UI

To build the server with the React-based admin UI included:

```bash
# Using the build script (recommended)
./build.sh --with-ui

# Or using Maven directly
mvn clean install -Pui
```

The UI build requires Node.js v20+ to be installed. If Node.js is not available, the build script will provide installation instructions.

### Build Profiles

The server supports several Maven build profiles:

- `-Pui` - Include the admin UI in the build
- `-Pnative` - Build a GraalVM native image
- `-Pml` - Include machine learning features
- `-Pwindows` / `-Punix` - OS-specific configurations (auto-detected)

### Build Output

The build produces:
- `target/maps-{version}.jar` - Main server JAR
- `target/classes/html/admin/` - Admin UI (when built with UI profile)
- `target/site/` - Documentation and dependency libraries

### Manual UI Build

If you prefer to build the UI separately:

```bash
cd ui/maps-admin-ui
npm ci
npm run build -- --mode production
```

The UI will be built to `../../../target/classes/html/admin/` for inclusion in the server JAR.

For detailed UI development instructions, see [ui/README.md](ui/README.md).

## OpenAPI Documentation

The server provides OpenAPI documentation when running:

- Swagger UI: `http://localhost:8080/api/docs`
- OpenAPI JSON: `http://localhost:8080/api/openapi.json`

The admin UI integrates with these APIs for server management functionality.

## Contributing
We welcome contributions! Please refer to our contribution guidelines for how you can participate in making MAPS Messaging Server even better.

## License

The Maps Messaging Server is dual-licensed under the Mozilla Public License Version 2.0 (MPL 2.0) and the Apache License 2.0 with Commons Clause License Condition v1.0.

Under the MPL 2.0 license, the software is provided for use, modification, and distribution under the terms of the MPL 2.0.

Additionally, the "Commons Clause" restricts the selling of the software, which means you may not sell the software or services whose value derives entirely or substantially from the software's functionality.

For full license terms, see the [LICENSE](LICENSE) file in the repository.



## Library and Server Build and Release Status

| Library Name                             | Buildkite Build Status    | SonarQube Quality Status    |
|------------------------------------------|---------------------|---------------------|
| Authentication & Authorisation Framework | [![Build status](https://badge.buildkite.com/4fe7fb40cfdb2f718310fbc030aa1e9f0df618201fa21f9736.svg)](https://buildkite.com/mapsmessaging/040-authentication-and-authorisation-library-snapshot-build)| [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Authentication_Library&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Authentication_Library) |
| Configuration Framework                  | [![Build status](https://badge.buildkite.com/4baaf7dabe5696aa511753a916992c7fb84634991063da5477.svg)](https://buildkite.com/mapsmessaging/030-configuration-library-snapshot-build)| [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Configuration_Library&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Configuration_Library) |
| Device Integration Framework             | [![Build status](https://badge.buildkite.com/47e184de3ea886a8dc79016c4ae5797fddf74713e4f679d6be.svg)](https://buildkite.com/mapsmessaging/040-device-library-snapshot-build) | [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=deviceLibrary&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=deviceLibrary) |
| Dynamic Storage Framework                | [![Build status](https://badge.buildkite.com/dc6145d667ab8f1ff9822dc81cda4eca34016f715950478bf6.svg)](https://buildkite.com/mapsmessaging/040-dynamic-storage-snapshot-build) | [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dynamic_storage&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=dynamic_storage) |
| JMS Selector                             | [![Build status](https://badge.buildkite.com/f583bc25c29d7d49b1d4566b07f06eda241d3de9c2cff056c0.svg)](https://buildkite.com/mapsmessaging/010-jms-selector-library-snapshot-build) | [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Maps-Messaging_jms_selector&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Maps-Messaging_jms_selector) |
| Logging Framework                        | [![Build status](https://badge.buildkite.com/ae632d6e5e09714b9746e1a38649b73f3843fb3aa9265b64de.svg)](https://buildkite.com/mapsmessaging/010-logging-framework-snapshot-build) | [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Simple_Logging&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Simple_Logging) |
| Naturally Ordered Structures Framework   | [![Build status](https://badge.buildkite.com/1a4f1cc90a99b5e19366acfbda446389d6a7597028360adeca.svg)](https://buildkite.com/mapsmessaging/010-natural-ordered-long-collection-snapshot-build) | [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Naturally_Ordered_Long_Collections&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Naturally_Ordered_Long_Collections) |
| Schema Framework                         | [![Build status](https://badge.buildkite.com/de2b96ee355ffe630a56381a08714000250a2b6b6aaa2b5777.svg)](https://buildkite.com/mapsmessaging/020-schema-library-snapshot-build) | [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Schemas&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Schemas) |
| Task Scheduler                           | [![Build status](https://badge.buildkite.com/ffcaa8c3475900a5a71cbc6a8e68ba12646f05de4fd3da1fb6.svg)](https://buildkite.com/mapsmessaging/020-non-blocking-task-scheduler-snapshot-build) | [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Non_Blocking_Task_Scheduler&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Non_Blocking_Task_Scheduler) |
| Messaging Server                         | [![Build status](https://badge.buildkite.com/5ae49cac0606f85c59688101fbcf49824f4dcf53b7b7c5e63f.svg)](https://buildkite.com/mapsmessaging/090-server-snapshot-build)| [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Maps-Messaging_mapsmessaging_server&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Maps-Messaging_mapsmessaging_server)|
| Web Admin Client                         | [![Build status](https://badge.buildkite.com/7cc9381cb4e32048a4978e91f483113a47217238b29461534e.svg)](https://buildkite.com/mapsmessaging/060-maps-web-client)| [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=web-admin-client&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=web-admin-client)|


[![Mutable.ai Auto Wiki](https://img.shields.io/badge/Auto_Wiki-Mutable.ai-blue)](https://wiki.mutable.ai/Maps-Messaging/mapsmessaging_server)




