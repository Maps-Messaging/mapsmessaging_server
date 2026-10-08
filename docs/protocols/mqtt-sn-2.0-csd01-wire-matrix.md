# MQTT-SN 2.0 CSD01 server wire-format matrix

Normative baseline: OASIS MQTT-SN 2.0 CSD01, August 2026, specification source commit `0dae5066d123d068ddca2ddb861ce8858aafee92`.

Reference-client catalogue: `Maps-Messaging/mqtt-sn-2.0-client/shared/control-packet-types.json`.

This matrix records implemented packet and session paths, not proof of full MQTT-SN 2.0 conformance. The focused test suite has not yet been executed because Maven and JDK 21 are unavailable in this checkout. No inheritance from MQTT-SN 1.2 is used by the active 2.0 protocol and wire decoder.

| Wire packet | CSD01 type | Decoder | Encoder | Notes |
|---|---:|---|---|---|
| CONNECT | 0x01 | MqttSn2ConnectCodec | MqttSn2ConnectCodec | Independent 2.0 flags and authentication fields |
| CONNACK | 0x02 | MqttSn2ConnAckCodec | MqttSn2ConnAckCodec | Optional expiry, keepalive and auth |
| PUBLISH | 0x03 | MqttSn2PublishCodec | MqttSn2PublishCodec | QoS 0–2; independent inbound/outbound QoS 1/2 tracking |
| PUBACK | 0x04 | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| PUBREC | 0x05 | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| PUBREL | 0x06 | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| PUBCOMP | 0x07 | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| SUBSCRIBE | 0x08 | MqttSn2SubscriptionCodec | MqttSn2SubscriptionCodec | CSD01 topic types |
| SUBACK | 0x09 | MqttSn2ReplyCodec | MqttSn2ReplyCodec | Alias optional |
| UNSUBSCRIBE | 0x0A | MqttSn2SubscriptionCodec | MqttSn2SubscriptionCodec | CSD01 topic types |
| UNSUBACK | 0x0B | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| PINGREQ | 0x0C | MqttSn2ControlCodec | MqttSn2ControlCodec | Packet Identifier |
| PINGRESP | 0x0D | MqttSn2ControlCodec | MqttSn2ControlCodec | Optional 16-bit remaining count, including 0xFFFF sentinel |
| DISCONNECT | 0x0E | MqttSn2DisconnectCodec | MqttSn2DisconnectCodec | Optional reason/expiry |
| AUTH | 0x0F | MqttSn2ControlCodec | MqttSn2ControlCodec | Existing SASL mechanism exchange |
| REGISTER | 0x10 | MqttSn2RegisterCodec | MqttSn2RegisterCodec | Topic Name/Packet Identifier |
| REGACK | 0x11 | MqttSn2RegAckCodec | MqttSn2RegAckCodec | Optional alias |
| PUBWOS | 0x12 | MqttSn2PublishCodec | MqttSn2PublishCodec | Sessionless QoS 0 |
| SLEEPREQ | 0x13 | MqttSn2SleepCodec | MqttSn2SleepCodec | Duration, buffered delivery and PINGREQ wake |
| SLEEPRESP | 0x14 | MqttSn2SleepCodec | MqttSn2SleepCodec | Optional duration/reason |
| WAKEUP | 0x15 | MqttSn2SleepCodec | MqttSn2SleepCodec | Empty control packet |
| ADVERTISE | 0x16 | MqttSn2GatewayCodec | MqttSn2GatewayCodec | Gateway information |
| SEARCHGW | 0x17 | MqttSn2GatewayCodec | MqttSn2GatewayCodec | Gateway discovery |
| GWINFO | 0x18 | MqttSn2GatewayCodec | MqttSn2GatewayCodec | Gateway address |
| FORWARDER_ENCAPSULATION | 0xFC | MqttSn2EncapsulationCodec | MqttSn2EncapsulationCodec | Embedded frame validation |
| CONNECTION_ENCAPSULATION | 0xFE | MqttSn2EncapsulationCodec | MqttSn2EncapsulationCodec | Embedded frame validation/allowlist |
| PROTECTION_ENCAPSULATION | 0xFF | MqttSn2ProtectionCodec | MqttSn2ProtectionCodec | Requires provider-defined tag size and authenticated encryption |

## Version and integration boundary

`MqttSnVersionDetector` recognises 1.2 and 2.0 CONNECT without assuming compatible packet numbering. `MQTTSNInterfaceManager` selects the standalone `MqttSn2Protocol` for new 2.0 sessions and retains the v1.2 manager path for 1.2 clients.

`MqttSn2PacketDecoder` never dispatches 2.0 packets through `v1_2.packet.PacketFactory`. `MqttSn2OutboundPacket` adapts encoded frames to the existing UDP write pipeline. Previous `MQTT_SNProtocolV2` and `PacketFactoryV2` classes remain in the tree temporarily as legacy implementation, but are not used for new 2.0 connections.

## Block 2 lifecycle implementation

- CONNECT AUTH uses the configured SASL mechanism. Session creation waits for successful AUTH; the same method is returned in CONNACK.
- CONNECT Will Name and configured predefined topics are installed in broker session state. Normal DISCONNECT clears the Will; abnormal close leaves the broker Will task active.
- Clean Start and Session Expiry configure persistent session restoration through the existing SessionManager path.
- QoS 1/2 inbound acknowledgements are sent after storage; inbound QoS 2 stages and commits a broker transaction on PUBREL. Outbound QoS 1/2 is serialized per client, acknowledged through PUBACK or PUBREC/PUBREL/PUBCOMP, and retransmitted with the appropriate duplicate behavior. Retry exhaustion closes the virtual connection.
- SLEEPREQ buffers subscription events while asleep. PINGREQ wakes the client, delivers within Default Awake Messages, returns PINGRESP Remaining Messages, then returns to sleep. The sleep timer uses 1.5 times the requested interval.
- MQTT-SN 1.2 continues through the existing v1.2 manager and protocol implementation.

## Remaining scope and validation

- Short-form CONNECT Will topic aliases that are not configured predefined topics are rejected. Full alias ownership/retention semantics and protection encapsulation require additional work.
- Complete response reason-code applicability and a full normative statement audit remain future conformance work.
- The released `0.1.0` client artifact is not currently present in `maps_releases`; `release-0.1.0` still publishes `0.1.0-SNAPSHOT`. The test dependency defaults to the requested release coordinate. Local compilation and tests used the published snapshot with `-DmqttSn2ClientVersion=0.1.0-SNAPSHOT`.
- The BC protection artifact is a regular test dependency, and its provider test runs in the normal test suite. It passes against the published snapshot with the same version override used for the client artifact.
- The focused client/server suite compiled, but UDP socket creation is denied by the local execution environment (`SocketException: Operation not permitted`). Its live protocol assertions have not passed locally. The retry unit test caught and fixed a DUP flag defect; 87 non-network MQTT-SN unit tests passed. The partial JaCoCo report shows 67% line coverage in the active protocol package and 72% in the packet codec package; 80% is the target, not a measured result.
- Jenkins is intentionally not run.

## Block 3 conformance test trace

The test suite identifies the August 2026 CSD01 source commit pinned above. The legacy third-party test client is now restricted to MQTT-SN 1.2; the released CSD01 client is the MQTT-SN 2.0 test peer.

| Requirement area | Test coverage | Normative reference |
|---|---|---|
| Version selection, including WILL CONNECT optional fields | `MqttSnVersionDetectorTest` | MQTT-SN 2.1.2, 2.1.3, 3.1.2 |
| CONNECT / CONNACK and inline WILL | `MqttSn2ConnectCodecTest`, `MqttSn2ClientServerConformanceTest.connectCarriesWillConfigurationInTheCsd01SessionRequest` | MQTT-SN 3.1.2, 3.2.2 |
| SASL AUTH challenge/response | `MqttSnAuthTest.scramAuthenticationUsesTheCsd01AuthExchange` | MQTT-SN 3.1.2.3, 3.3.2, 3.3.3, 4.11.1 |
| Topic names, subscriptions, delivery, unsubscribe | `MqttSn2ClientServerConformanceTest.subscribePublishAndUnsubscribeUseNameTopics` | MQTT-SN 3.7.2, 3.7.3, 3.8.2, 3.9.2, 3.9.3 |
| QoS 0, 1 and 2 acknowledgement flows | `MqttSn2ClientServerConformanceTest.connectPublishQos1AndQos2AndDisconnect`, `MqttSn2OutgoingDeliveryManagerTest` | MQTT-SN 4.3.2, 4.3.3, 4.3.4 |
| Retry identifiers and duplicate handling | `MqttSn2OutgoingDeliveryManagerTest.qos_one_retry_sets_dup_and_reuses_packet_identifier`, `qos_two_retries_pubrel_without_retransmitting_publish_after_pubrec` | MQTT-SN 4.3.3, 4.3.4, 4.4.2 |
| Session reconnect, sleeping and wake | `MqttSn2ClientServerConformanceTest.pingAndReconnectKeepTheSameClientIdentifier`, `sleepingClientWakesWithPingReq`, `MqttSn2LifecycleTest` | MQTT-SN 3.1.2, 3.2.2, 4.14.1, 4.14.2 |
| Gateway discovery and malformed traffic | `MqttSn2ClientServerConformanceTest.searchGatewayReturnsGatewayInfo`, `reservedPacketTypeIsDroppedWithoutCorruptingTheGateway`, packet codec tests | MQTT-SN 2.1.2, 2.1.3, 6.1.2, 6.1.3 |
| MQTT 3.1.1 and MQTT 5 bridging | `MqttSn2ClientServerConformanceTest.qosOneAndTwoPublicationsBridgeToMqtt311AndMqtt5` | MQTT-SN 5, MQTT 3.1.1, MQTT 5.0 |
| MQTT-SN 1.2 regression | `src/test/java/io/mapsmessaging/network/protocol/impl/mqtt_sn/v1_2` and retained MQTT-SN 1.2/Paho tests | MQTT-SN 1.2, 14 November 2013 |
| BC protection provider | `MqttSn2ProtectionArtifactTest` in the normal test suite | MQTT-SN 2.1.3, 4.13 |

The target is at least 80% coverage of the active MQTT-SN v2 protocol and codecs. The partial local report is below target (67% protocol package, 72% packet codec package); state/listener test paths and live protocol assertions remain unmeasured because the integration suite cannot open local sockets here.

## Validation gate

This file records implementation and test traceability, **not passing conformance tests**. Do not mark Jira MSG-324 Done or describe the server as CSD01 compliant until the released client dependency resolves, the full local test suite passes, and coverage is measured against the 80% target. Jenkins is intentionally not triggered for this block.
