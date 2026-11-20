# MapsMessaging 4.6.0 release notes

Status: Unreleased. Changes since 4.5.0, including supporting libraries and optional integrations.

This release focuses on connection recovery, stored-message reliability and improved device integration. It is particularly relevant to deployments using unreliable network links, persistent messaging, TAK/CoT or CAN/NMEA data.

## Upgrade considerations

- **Java 25 is required.** Include the runtime upgrade in deployment planning.
- **CAN/NMEA output names have changed.** Review dashboards, mappings and downstream applications that use decoded message or field names.
- **CoAP validation is ongoing.** An intermittent high-rate notification test failure remains under monitoring.
- **Shared-subscription group identity has changed ([MSG-444](https://mapsmessaging.atlassian.net/browse/MSG-444)).** Shared subscribers now use the destination and named shared group, with separate selector partitions, rather than a destination-only identity. This changes the keys used to locate persisted shared-subscription message state. No automatic migration of earlier shared-group state has been verified. Before upgrading, drain pending and in-flight shared-group messages or plan an explicit migration/replay from a backup. Do not assume old shared-group messages will be recovered under the new keys. Ordinary queue persistence is not intended to change.
- **MQTT-SN 2.0 uses an evolving draft specification.** Clients should be checked against the supported OASIS MQTT-SN 2.0 draft; it is not yet a final standard. MQTT-SN 1.2 is supported separately.
- **Optional drone-twin and STANAG components must be version-aligned.** MAVLink command/mission execution responsibilities have moved into the server protocol layer; upgrade and verify the corresponding digital-twin/STANAG integrations together.

## Changes and fixes

- **Connection recovery:** Restores messaging subscriptions after an inter-server connection returns and reduces repeated switching when a connection is unstable.
- **Stored-message protection:** Fixes cleanup that could incorrectly delete messages intended to be kept across storage partitions or memory tiers.
- **Message delivery:** Improves retained-message replay, delayed delivery and recovery, and restores the full message-priority range.
- **Shared subscriptions ([MSG-444](https://mapsmessaging.atlassian.net/browse/MSG-444)):** Keeps distinct named shared groups independent so each group can receive its own delivery while its members share the work. Expands isolated engine regression coverage for shared groups, wildcard matching and subscription lifecycle behaviour.
- **Storage reliability:** Fixes a concurrent-read issue that could cause stored-message retrieval to fail.
- **Configuration preservation:** Prevents lists of configuration values from being silently lost when settings are copied or saved.
- **MQTT connections:** Corrects outbound connections with omitted or partial credentials, subject to the receiving server's authentication policy.
- **MQTT protocol interoperability:** Improves MQTT 3.1.1 and MQTT 5 validation for variable-length encodings, UTF-8 properties, subscription acknowledgements, topic aliases and invalid packet handling.
- **MQTT session recovery:** Improves persistent-session redelivery, retained-message flags, packet identifier handling and QoS flow control on reconnect.
- **MQTT-SN interoperability:** Improves MQTT-SN 1.2 packet handling and updates MQTT-SN 2.0 wire encoding and session behaviour against the supported OASIS draft.
- **NATS, JetStream and STOMP:** Improves wire-protocol compliance, frame processing, consumer handling and subscription cancellation.
- **AMQP and JMS compatibility:** Improves AMQP connection and authentication handling, error responses, JMS producer behaviour, delivery delay and message-expiration handling.
- **WebSocket connections:** Hardens HTTP upgrade and close handling, including invalid or fragmented input.
- **CoAP transfers:** Corrects large-message transfers, duplicate handling and connection cleanup.
- **TAK/CoT connections:** Uses the server's common connection management for reconnects, certificates and connection settings.
- **CoT message handling:** Improves handling of fragmented or malformed event streams so subsequent valid events can be recovered.
- **CAN/NMEA coverage:** Expands NMEA 2000 decoding and adds J1939 message encoding and decoding.
- **CANboat definitions:** Uses registry-based CAN/NMEA message definitions and PGN transport metadata to select the appropriate transport encoding.
- **CAN interface reliability:** Corrects interface stop/restart cleanup and keeps multi-frame messages together during transmission.
- **Shared-memory connections:** Adds an option for messaging between applications on the same computer. Supporting RDMA transport integration is available where the hardware and runtime support it.
- **Drone tasking, where installed:** Prevents pending tasks for the same drone from racing to execute, allows cancellation after disarming, and improves task status reporting during vehicle-mode changes.
- **MAVLink and digital twins:** Moves MAVLink command and mission transaction handling into the server protocol implementation and separates reusable digital-twin functionality into a supporting library.
- **STANAG/TAK integration, where installed:** Improves mission execution, detection-event publication and the operational display of contacts and task progress.
- **Windows operation:** Fixes administrator-password setup and persistent-session saves failing on unsupported file operations.
- **Linux maintenance:** Adds scripts for optional daily automatic Maps package updates.
- **Security improvements:** Strengthens JWT identity validation, certificate authentication, anonymous-session policy and permissions on generated password files.
- **Connection security:** Improves handling of stalled TLS connections and cleanup of inactive inter-server endpoints.
- **Schema management:** Adds paged schema listings so integrations can retrieve large schema collections in smaller batches.
- **Schema precision:** Preserves unsigned 64-bit numeric values in schema formatters without narrowing them to signed integers.
- **Impact monitoring:** Adds H3LIS200DL sensor support for monitoring shocks and impacts across three axes.
- **NFC readers:** Completes DFRobot PN532 support, improving reader discovery, tag detection and recovery after communication failures.
- **Motion sensing and clock detection:** Adds MPU-6050 six-axis motion sensing and improves identification of motion sensors and DS3231 clocks that use the same device address.

- **Direct-connected device reliability:** Improves accuracy and recovery for a broad range of I²C sensors, real-time clocks, displays and other attached peripherals. Fixes potential data errors in measurements, clock functions, device settings and memory operations.
- **More trustworthy device data:** Aligns device-generated JSON schemas with the information actually published, making it easier for applications to interpret measurements and distinguish read-only data from device commands.
- **Expanded hardware coverage:** Improves operation of LED and LCD displays, GPIO and PWM controllers, non-volatile memory, 1-Wire temperature sensors and SPI analogue-to-digital converters.
- **Secondary computed devices:** Supports logical output devices derived from shared I²C and serial hardware without opening duplicate hardware connections.
- **Device integration guidance:** Adds Linux installation, device-specific setup instructions and working examples to the supporting device library, including details of permissions, address conflicts and special hardware requirements.