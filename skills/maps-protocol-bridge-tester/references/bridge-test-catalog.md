# Bridge Test Catalog

## Preconditions

1. Container/process healthy and listeners bound.
```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
docker logs <container> 2>&1 | rg -n "ERROR|Startup aborted|Protocol not available|BindException|Address already in use"
docker exec <container> sh -lc '(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | rg "1883|5672|8080|9001"'
```

2. Namespace mapping and routing present in config.
```bash
rg -n "namespace: |namespaceMapping:" DestinationManager.yaml
rg -n "predefinedServers|enabled|autoDiscovery" routing.yaml
```

## Core Test Patterns

### MQTT -> MQTT (baseline)
```bash
mosquitto_sub -h localhost -p 1883 -t /bridge/test/# -C 1 -W 8 -d
mosquitto_pub -h localhost -p 1883 -t /bridge/test/in -m '{"cid":"b1","v":1}' -d
```

### MQTT -> WS (encapsulated path readiness)
```bash
timeout 8 mosquitto_sub -L ws://127.0.0.1:9001/ -t /bridge/ws/# -C 1 -d -W 5
mosquitto_pub -h localhost -p 1883 -t /bridge/ws/in -m '{"cid":"b2","v":2}' -d
```

### MQTT -> AMQP (bridge verification scaffold)
- Use MQTT producer command as ingress.
- Use AMQP consumer matching deployment tooling (qpid-proton, rhea, or project test harness).
- Require correlation ID match between source and destination payloads.

### CoAP -> MQTT (gateway path)
- Publish CoAP payload to configured CoAP endpoint.
- Consume expected mapped MQTT namespace.
- Validate contentType/schema handling where configured.

## Failure Classification

- Ingress failure: source producer cannot connect (`CONNACK` missing, timeout, refused).
- Routing failure: ingress accepted but no delivery to mapped namespace.
- Transform/schema failure: destination receives payload with invalid shape/contentType/schemaId.
- Egress failure: destination protocol consumer cannot connect or receive.

## Recommended Markers

- Always include:
  - `cid` (correlation id)
  - `ts` (epoch millis)
  - `route` (expected namespace or bridge path)
