# Satellite Pub-Sub Patterns

## Goal

Generate Iridium-style service patterns using MAPS satellite ingress and multi-protocol egress.

## Pattern Templates

1. `orbcomm-uplink-mqtt-amqp`
- Provider profile: Orbcomm
- Ingress: satellite endpoint
- Egress: MQTT and AMQP fan-out
- Good for: command and telemetry consumers with mixed broker clients

2. `orbcomm-bidirectional-mqtt-nats`
- Provider profile: Orbcomm
- Ingress and command return path via satellite
- Egress: MQTT and NATS
- Good for: low-latency command distribution with event-stream consumers

3. `viasat-uplink-mqtt-ws-rest`
- Provider profile: Viasat (profile mapping)
- Ingress: satellite endpoint
- Egress: MQTT plus WS plus REST visibility
- Good for: web clients and operational dashboards

4. `viasat-bidirectional-amqp-stomp-coap`
- Provider profile: Viasat (profile mapping)
- Ingress and command return path
- Egress: AMQP, STOMP, CoAP
- Good for: heterogeneous device and enterprise integrations

5. `orbcomm-cbc-sinmin-mqtt-nats`
- Provider profile: Orbcomm
- Encoding: CBC
- Routing rule: auto-route SIN/MIN values less than `127` into `/{deviceId}/common/in/{sin}/{min}`
- Egress: MQTT and NATS branches subscribed to SIN/MIN hierarchy
- Good for: fine-grained device command and telemetry streams keyed by SIN/MIN

6. `viasat-cbc-sinmin-amqp-ws-rest`
- Provider profile: Viasat (profile mapping)
- Encoding: CBC
- Routing rule: auto-route SIN/MIN values less than `127` into per-SIN/MIN hierarchy
- Egress: AMQP, WS, REST
- Good for: operations platforms needing protocol diversity and SIN/MIN-specific subscriptions

## Variant Selection Heuristics

- Prefer fewer protocols for reliability-first deployments.
- Add WS or REST when operator-facing observability is required.
- Use AMQP or NATS when downstream systems need queue/stream semantics.
- Use CoAP only when constrained-device consumers are explicit.

## Verification Skeleton

For each chosen protocol path:
- verify listener bound
- publish correlated test message at satellite boundary namespace
- consume at each downstream protocol endpoint
- assert correlation ID and expected route tags
- for CBC patterns, include at least one message where SIN and MIN are below `127` and assert arrival on the exact `.../{sin}/{min}` topic path
