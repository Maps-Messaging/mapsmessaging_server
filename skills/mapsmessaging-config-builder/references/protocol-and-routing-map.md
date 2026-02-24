# Protocol and Routing Map

## Config Surfaces

- `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml`: listener URLs, transport (`tcp`/`udp`/`ssl`/`dtls`/`hmac`/`satellite`), protocol adapters, auth realms, protocol defaults.
- `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml`: namespace storage layout, message override defaults (`contentType`, QoS, retain, `schemaId`).
- `/Users/krital/dev/starsense/mapsmessaging_server/routing.yaml`: inter-server topology and predefined peers.
- Other manager files (`AggregatorManager.yaml`, `MLModelManager.yaml`, `SchemaManager.yaml`) when request explicitly includes those capabilities.

## Supported Protocol Targets

When requirements mention a protocol, map to `protocolConfigs[].type` in `NetworkManager.yaml` using the closest supported adapter in this deployment:
- MQTT v3/v5 -> `mqtt`
- MQTT-SN -> `mqtt-sn`
- AMQP -> `amqp`
- STOMP -> `stomp` (if provider present in build)
- NATS / JetStream ingress-egress paths -> `nats` where provider is available
- CoAP -> `coap`
- LoRa local integration -> LoRa-specific endpoint/protocol entries present in current config set
- Satellite (Orbcomm/Inmarsat) -> `satellite` endpoint/protocol entries where enabled
- CAN bus (J1939/N2K) -> `canbus` endpoint entries where enabled
- WebSocket encapsulation for MQTT/AMQP paths -> `ws`

If provider is absent in runtime image, output valid config plus a limitation note and startup check command.

## Requirement to Config Mapping

1. Listener and protocol ingress
- Request terms: "expose MQTT on 1883", "add AMQP endpoint", "enable CoAP/DTLS", "bridge NATS".
- Primary target: `NetworkManager.endPointServerConfigList[]`.
- Required fields:
  - `name`
  - `authenticationRealm`
  - `url` (example `tcp://0.0.0.0:1883/`)
  - `endPointConfig.type`
  - `protocolConfigs[].type`

2. Encoding, schema, and payload behavior
- Request terms: "JSON output", "XML payload", "Protobuf schema", "set QoS 1", "retain".
- Primary targets:
  - `NetworkManager...protocolConfigs[].messageDefaults`
  - `DestinationManager...messageOverride`
- Common fields:
  - `contentType`
  - `qualityOfService`
  - `retain`
  - `schemaId`

3. Namespace and destination routing
- Request terms: "map /edge to /core", "route /telemetry to memory/file".
- Primary target: `DestinationManager.data[]`.
- Required fields:
  - `namespace`
  - `namespaceMapping`
  - destination `type`
  - `storageConfig.type`
  - `directory` for file-backed storage

4. Inter-server topology
- Request terms: "federate with site-broker", "bridge to remote MAPS".
- Primary target: `routing.predefinedServers[]`.
- Feature flags:
  - `routing.enabled`
  - `routing.autoDiscovery`

5. Ordered transformation behavior
- Request terms: "apply CloudEvent wrapper then JSON mutation", "enrich with geohash".
- Target: transformation config sections in the relevant manager file.
- Rule: preserve configured order; deterministic and non-throwing behavior only.

## Startup and Bind Diagnostics

Include these checks in verification steps when deploying:
```bash
docker logs <container> 2>&1 | rg -n "Address already in use|BindException|Protocol not available|Startup aborted|Consul|license"
docker exec <container> sh -lc '(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | rg "1883|5672|8080|9001"'
```

## Practical Search Patterns

```bash
rg -n "endPointServerConfigList|protocolConfigs|messageDefaults|url:" /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
rg -n "namespace: |namespaceMapping: |messageOverride|schemaId" /Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml
rg -n "routing:|predefinedServers|autoDiscovery|enabled" /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml
rg -n "window|timeout|maxEventsPerTopic|contribution" /Users/krital/dev/starsense/mapsmessaging_server/AggregatorManager.yaml
```
