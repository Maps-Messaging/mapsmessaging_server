# MapsMessaging 4.6.0 release notes

Status: Unreleased. Changes since 4.5.0, including supporting libraries and optional integrations.

This release focuses on connection recovery, stored-message reliability and improved device integration. It is particularly relevant to deployments using unreliable network links, persistent messaging, TAK/CoT or CAN/NMEA data.

## Upgrade considerations

- **Java 25 is required.** Include the runtime upgrade in deployment planning.
- **CAN/NMEA output names have changed.** Review dashboards, mappings and downstream applications that use decoded message or field names.
- **CoAP validation is ongoing.** An intermittent high-rate notification test failure remains under monitoring.

## Changes and fixes

- **Connection recovery:** Restores messaging subscriptions after an inter-server connection returns and reduces repeated switching when a connection is unstable.
- **Stored-message protection:** Fixes cleanup that could incorrectly delete messages intended to be kept across storage partitions or memory tiers.
- **Message delivery:** Improves retained-message replay, delayed delivery and recovery, and restores the full message-priority range.
- **Storage reliability:** Fixes a concurrent-read issue that could cause stored-message retrieval to fail.
- **Configuration preservation:** Prevents lists of configuration values from being silently lost when settings are copied or saved.
- **MQTT connections:** Corrects outbound connections with omitted or partial credentials, subject to the receiving server's authentication policy.
- **CoAP transfers:** Corrects large-message transfers, duplicate handling and connection cleanup.
- **TAK/CoT connections:** Uses the server's common connection management for reconnects, certificates and connection settings.
- **CoT message handling:** Improves handling of fragmented or malformed event streams so subsequent valid events can be recovered.
- **CAN/NMEA coverage:** Expands NMEA 2000 decoding and adds J1939 message encoding and decoding.
- **CAN interface reliability:** Corrects interface stop/restart cleanup and keeps multi-frame messages together during transmission.
- **Shared-memory connections:** Adds an option for messaging between applications on the same computer.
- **Drone tasking, where installed:** Prevents pending tasks for the same drone from racing to execute, allows cancellation after disarming, and improves task status reporting during vehicle-mode changes.
- **Windows operation:** Fixes administrator-password setup and persistent-session saves failing on unsupported file operations.
- **Linux maintenance:** Adds scripts for optional daily automatic Maps package updates.
- **Schema management:** Adds paged schema listings so integrations can retrieve large schema collections in smaller batches.
- **Impact monitoring:** Adds H3LIS200DL sensor support for monitoring shocks and impacts across three axes.
- **NFC readers:** Completes DFRobot PN532 support, improving reader discovery, tag detection and recovery after communication failures.
- **Motion sensing and clock detection:** Adds MPU-6050 six-axis motion sensing and improves identification of motion sensors and DS3231 clocks that use the same device address.
