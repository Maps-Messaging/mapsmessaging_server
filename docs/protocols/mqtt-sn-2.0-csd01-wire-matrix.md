# MQTT-SN 2.0 CSD01 server wire-format matrix

Normative baseline: OASIS MQTT-SN 2.0 CSD01, August 2026, specification source commit `0dae5066d123d068ddca2ddb861ce8858aafee92`.

Reference-client catalogue: `Maps-Messaging/mqtt-sn-2.0-client/shared/control-packet-types.json`.

This matrix records implemented packet and session paths, not proof of full MQTT-SN 2.0 conformance. The active 2.0 protocol and wire decoder do not inherit MQTT-SN 1.2 semantics. Jenkins #171 successfully compiled and ran the server-wide suite on the refit branch (commit 0b574404), reporting 7,812 tests, 0 failures, 79 skipped. The remaining normative and security gaps below must not be construed as passing conformance requirements.

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
| PROTECTION_ENCAPSULATION | 0xFF | MqttSn2ProtectionCodec + opt-in MqttSn2ProtectionVerifier | MqttSn2ProtectionCodec (envelope only) | HMAC-SHA-256/SHA3-256 inbound verification and signed outbound frames are implemented behind an opt-in endpoint policy; durable key/counter provisioning and AEAD providers remain unconfigured |

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

- MQTT-SN 2.0 Protection Encapsulation supports **opt-in bidirectional HMAC** (schemes 0x00/0x01), authenticating the exact prefix and inner packet, rejecting inbound replay and protecting outbound frames. The endpoint must explicitly supply a key resolver and a **durable, monotonic outbound counter source**. A secured endpoint fails closed on plaintext input and a signing failure. **Production activation is not complete:** no server configuration maps key identities to endpoint policies, no durable inbound replay store is yet wired, and AEAD providers are not integrated. The older UDP HMAC wrapper is not reused. Do not claim full protected-messaging conformance until these provisions are complete.
- Active topic alias handling now separates session and predefined namespaces, avoids session aliases for wildcard SUBSCRIBE filters (CSD01 4.7.2.2-3), prefers predefined aliases in SUBACK and clears session aliases on SLEEPREQ when Retain Topic Aliases is zero. **Session persistence across reconnection, REGISTER conflict reason codes and the remaining normative statement audit still require verification.** Do not claim complete CSD01 conformance.
- `mqtt-sn-2-client` and optional `mqtt-sn-2-client-protection-bc` test artifacts use the **unreleased** `0.1.0-SNAPSHOT` coordinate. No `0.1.0` release has been made.
- The user reported 88 MQTT-SN 2.0 tests passing in IntelliJ and subsequently confirmed SCRAM authentication success. These are user-reported local results. Further HMAC, alias and negative-path tests were committed later and must be rerun.
- The full Jenkins branch run [#171](https://jenkins.mapsmessaging.io/job/mapsmessaging-server-junit/171/) at revision `0b574404af5fefebd307f9ff1f27c402d160f7ee` completed **SUCCESS**: **7,812 total, 0 failed, 79 skipped**. This supersedes the outdated local socket restriction and no-Jenkins status recorded earlier.
- Jenkins SonarCloud analysis was uploaded under the `development` branch identity. This is intentionally accepted for now and must not be presented as a distinct branch analysis.
- The user subsequently requested SonarCloud coverage to guide the remaining improvements. The accepted `development` branch analysis after Jenkins #171 reports **74.7% overall coverage, 78.2% line coverage and 66.4% branch coverage** for the server. Active `MqttSn2Protocol` is **60.5% overall coverage**; `MqttSn2Lifecycle` and `MqttSn2OutgoingDeliveryManager` are approximately 88.8% and 88.9%. Many obsolete 2.0 implementation classes have 0% coverage and must not be confused with the active protocol path. These metrics precede the latest protection and topic-alias changes.

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

## Validation gate and disposition

**Automated build/test gate: PASSED** on Jenkins #171 at the revision above. This establishes compilation and suite execution for that revision, not independent certification against all CSD01 MUST requirements.

**Full normative conformance: NOT SIGNED OFF.** Key provisioning, durable replay state, AEAD schemes, persistent alias semantics and clause-level negative-behaviour verification still require explicit implementation or documented unsupported-feature policy.

Coverage metrics are available in SonarCloud and inform test prioritisation. No merge, formal release or Jira Done transition has been authorised.
