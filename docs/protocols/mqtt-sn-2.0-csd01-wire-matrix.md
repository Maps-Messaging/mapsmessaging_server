# MQTT-SN 2.0 CSD01 server wire-format matrix

Normative baseline: OASIS MQTT-SN 2.0 CSD01, August 2026, specification source commit `0dae5066d123d068ddca2ddb861ce8858aafee92`.

Reference-client catalogue: `Maps-Messaging/mqtt-sn-2.0-client/shared/control-packet-types.json`.

This matrix is **wire-codec scope**, not proof of operational MQTT-SN 2.0 conformance. The codecs are committed but their test suite has not yet been executed in Maven/Jenkins. No inheritance from MQTT-SN 1.2 is used by the *active* 2.0 protocol and wire decoder.

| Wire packet | CSD01 type | Decoder | Encoder | Notes |
|---|---:|---|---|---|
| CONNECT | 0x01 | MqttSn2ConnectCodec | MqttSn2ConnectCodec | Independent 2.0 flags and authentication fields |
| CONNACK | 0x02 | MqttSn2ConnAckCodec | MqttSn2ConnAckCodec | Optional expiry, keepalive and auth |
| PUBLISH | 0x03 | MqttSn2PublishCodec | MqttSn2PublishCodec | QoS 0–2 wire payloads; QoS2 state machine pending |
| PUBACK | 0x04 | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| PUBREC | 0x05 | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| PUBREL | 0x06 | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| PUBCOMP | 0x07 | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| SUBSCRIBE | 0x08 | MqttSn2SubscriptionCodec | MqttSn2SubscriptionCodec | CSD01 topic types |
| SUBACK | 0x09 | MqttSn2ReplyCodec | MqttSn2ReplyCodec | Alias optional |
| UNSUBSCRIBE | 0x0A | MqttSn2SubscriptionCodec | MqttSn2SubscriptionCodec | CSD01 topic types |
| UNSUBACK | 0x0B | MqttSn2AckCodec | MqttSn2AckCodec | Identifier/optional reason |
| PINGREQ | 0x0C | MqttSn2ControlCodec | MqttSn2ControlCodec | Packet Identifier |
| PINGRESP | 0x0D | MqttSn2ControlCodec | MqttSn2ControlCodec | Optional remaining count |
| DISCONNECT | 0x0E | MqttSn2DisconnectCodec | MqttSn2DisconnectCodec | Optional reason/expiry |
| AUTH | 0x0F | MqttSn2ControlCodec | MqttSn2ControlCodec | SASL exchange pending |
| REGISTER | 0x10 | MqttSn2RegisterCodec | MqttSn2RegisterCodec | Topic Name/Packet Identifier |
| REGACK | 0x11 | MqttSn2RegAckCodec | MqttSn2RegAckCodec | Optional alias |
| PUBWOS | 0x12 | MqttSn2PublishCodec | MqttSn2PublishCodec | Sessionless QoS 0 |
| SLEEPREQ | 0x13 | MqttSn2SleepCodec | MqttSn2SleepCodec | Duration/alias retention |
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

## Deferred to lifecycle/authentication (Block 2)

- Full SASL AUTH state negotiation and explicit identity authorisation.
- WILL negotiation, clean start, session expiry, reconnect and recovery.
- Complete session-alias/predefined-alias ownership and registration semantics.
- QoS 1/2 retransmission, inflight ordering, PUBREC/PUBREL/PUBCOMP transaction state and outbound delivery callbacks.
- Sleeping clients and wakeup flow control.
- Cryptographic Protection Provider integration and authenticated decryption.
- Complete response reason-code applicability and draft normative statement audit.
- Rejection/error packet policy on invalid transitions and malformed transport input.

## Validation gate

This file records code coverage, **not passing conformance tests**. Do not mark Jira MSG-324 Done or describe the server as CSD01 compliant until the branch compiles, unit and integration tests pass, and wire fixtures are compared to the exact August draft. Jenkins was intentionally not triggered during Block 1.
