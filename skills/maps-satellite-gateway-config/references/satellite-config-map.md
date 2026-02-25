# Satellite Config Map

## Primary Surface

- `NetworkManager.yaml`
  - satellite endpoint entries under `endPointServerConfigList[]`
  - protocol block `protocolConfigs[].type: satellite`

Secondary surfaces for pub-sub pattern realization:
- `DestinationManager.yaml`
- `routing.yaml`

## Provider Profiles

1. Orbcomm profile
- Map to OGWS-oriented runtime paths.
- Emphasize queue and polling reliability settings.

2. Viasat profile
- Treat as provider profile requirements mapped into available satellite DTO fields.
- If a dedicated Viasat adapter is unavailable in runtime, emit a fallback profile note and configure the nearest supported satellite path.

## Protocol Choice Mapping

Satellite publish boundary may fan out to one or more protocols depending on request:
- MQTT / MQTT-SN
- AMQP
- STOMP
- NATS
- CoAP
- WS encapsulation
- REST-facing access paths

For each selected protocol path:
- ensure listener exists in `NetworkManager.yaml`
- ensure namespace mapping exists in `DestinationManager.yaml`
- ensure routing entry exists when cross-server delivery is required

## CBC Encoding and SIN/MIN Auto-Routing

When payload encoding mode is CBC, apply this routing rule in generated patterns:
- Treat SIN and MIN as unsigned bytes.
- If SIN or MIN is less than `127`, auto-route to individual SIN/MIN hierarchy paths.
- Preferred inbound topic template:
  - `/{deviceId}/common/in/{sin}/{min}`
- Keep per-SIN/MIN routing explicit in namespace mapping output so downstream protocol fan-out can subscribe to deterministic branches.

For non-CBC or unsupported payload encodings:
- keep fallback to configured generic inbound namespace and note the limitation.

## Validation Commands

```bash
rg -n "type: satellite|satellite://|Orbcomm|Viasat|Inmarsat" NetworkManager.yaml
rg -n "type: mqtt|type: mqtt-sn|type: amqp|type: stomp|type: nats|type: coap|type: ws" NetworkManager.yaml
rg -n "namespace: |namespaceMapping:" DestinationManager.yaml
rg -n "\\{sin\\}|\\{min\\}|common/in" src/main/java/io/mapsmessaging/network/protocol/impl/satellite
```

## Failure Classes

- Provider profile mismatch against available runtime adapters.
- Protocol fan-out configured without corresponding listeners.
- Polling cadence too aggressive causing queue churn.
- Fragmentation and reassembly assumptions not aligned with payload profile.
