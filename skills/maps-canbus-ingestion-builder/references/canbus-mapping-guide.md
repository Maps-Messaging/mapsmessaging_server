# CAN Bus Mapping Guide

## Primary Config Surface

- `NetworkManager.yaml`
  - `endPointConfig.type: canbus`
  - `endPointConfig.deviceName`
  - `protocolConfigs[].type: n2k` for decoded N2K workflows

## Protocol/Endpoint Alignment

1. Raw CAN ingestion
- Endpoint type: `canbus`
- Protocol: can remain raw path if no N2K decode required
- Content type typically `canbus`

2. N2K decode path
- Endpoint type: `canbus`
- Protocol config type: `n2k`
- Configure N2K options:
  - `topicNameTemplate`
  - `unknownPacketTopic`
  - `parseToJson`
  - database source (`databasePath` or embedded base64)

## Environment Profiles

1. vcan profile
- Use `vcan0` (or similar) for reproducible local tests.
- Suitable for parser and routing validation.

2. Native CAN profile
- Use physical interface (`can0`, `can1`) and hardware-specific setup.
- Validate throughput and timing characteristics separately from vcan.

## Validation Commands

```bash
rg -n "type: canbus|type: n2k|deviceName|topicNameTemplate|unknownPacketTopic|parseToJson" NetworkManager.yaml
rg -n "N2K_PROTOCOL_CREATED_AND_BOUND|N2K_PROTOCOL_PARSING_PACKET|N2K_PROTOCOL_PARSED" src/main/java/io/mapsmessaging/logging/ServerLogMessages.java
```

## Verification Skeleton

- Start with listener/startup diagnostics.
- Inject CAN frames (vcan or hardware).
- Verify expected mapped MAPS topics receive decoded or raw events.
- Confirm metadata markers (for example `protocol: n2k`) when decode path is enabled.

## Failure Classes

- Endpoint bound to wrong `deviceName`.
- N2K config references missing database definitions.
- parseToJson mismatch with expected downstream content type.
- Raw and decoded topic templates conflicting.
