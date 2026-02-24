---
name: mapsmessaging-config-builder
description: Translate protocol, encoding, topology, schema, and transformation requirements into deployable MAPS Messaging Server configuration entities. Use when a request describes protocol listeners, protocol bridging, namespace routing, schema registration/selection, transformer chains, aggregator/event-stream behavior, or link routing and needs exact MAPS YAML edits or deployable config manifests.
---

# MAPS Messaging Config Builder

Build production-grade MAPS configuration output from requirements. Convert narrative protocol and topology intent into exact YAML changes for this repository layout.

## Workflow

1. Normalize requirements into a contract.
- Extract protocol(s), transport, bind URL/port, auth realm, namespace mapping, payload encoding, schema IDs/types, transformation order, and routing topology.
- Resolve ambiguity with one concise clarification question only when a missing field changes runtime behavior.
- Default safely when unspecified: `authenticationRealm: anon`, no payload overrides, `proxyProtocol: false`.

2. Map contract to config surfaces.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/mapsmessaging-config-builder/references/protocol-and-routing-map.md`.
- Map listeners/adapters to `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml`.
- Map namespace storage and message overrides to `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml`.
- Map federation/topology to `/Users/krital/dev/starsense/mapsmessaging_server/routing.yaml`.
- If request includes aggregator, ML, or schema behavior, target corresponding manager YAML files in this repo and keep changes minimal.

3. Build deployable entities.
- Prefer minimal diffs against existing files.
- For deployment requests, emit full file bodies or Kubernetes `ConfigMap` manifests.
- Use deterministic names: `<protocol>-<transport>-<role>-<port>`.

4. Validate before finalizing.
- Re-open touched YAML files and match existing key casing and style.
- Run targeted checks:
```bash
rg -n "type: (mqtt|mqtt-sn|amqp|stomp|coap|ws|nats|satellite|canbus)" /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
rg -n "namespace: |namespaceMapping: |contentType:|schemaId:" /Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml
rg -n "predefinedServers|autoDiscovery|enabled" /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml
```
- For runtime checks, include startup diagnostics first, then protocol smoke checks.
- For executable runtime smoke, use:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/mapsmessaging-config-builder/scripts/run_mapsmessaging_config_builder_runtime_smoke.sh`

5. Return output using the output contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/mapsmessaging-config-builder/references/output-contract.md`.
- Always include assumptions, file-by-file deployable content, apply steps, startup diagnostics, and protocol verification commands.

## Platform Rules

- Preserve existing architecture and DTO naming/casing (`BaseConfigDTO`, discriminator `type`, `protocolConfigs`, `messageDefaults`).
- Keep outputs compatible with Java 21 runtime and current MAPS managers.
- Prefer MAPS-native bridging/routing over introducing external brokers.
- Treat transformer chains as deterministic and ordered; never emit unordered transform behavior.
- Prefer Gson-compatible JSON handling assumptions and avoid Jackson-specific recommendations.
- If a requested feature requires unavailable plugins/providers, state the gap and output the nearest valid MAPS config plus explicit limitation notes.

## Scenario Modes

- `Simple Local Default`:
  - Produce one minimal, locally testable configuration path (single ingress protocol, one namespace mapping, one verification pair of commands).
  - Prefer existing default listener ports and anon realm unless explicitly overridden.
- `Advanced Combination Matrix`:
  - Provide 3 to 6 viable variants across protocol, encoding, routing topology, and transformation/schema behavior.
  - Include tradeoffs and select one recommended variant with deployable output.

## Observability and Architecture Outputs

- Always generate scenario-specific metrics mapped to the deployed flow (throughput, latency, error and drop counters, backlog/queue depth, and protocol- or feature-specific KPIs).
- Always provide two dashboard options:
  - Grafana-ready panel and query specification (or JSON model when requested).
  - MAPS-hosted dashboard view specification (REST/WS backed) for local-first operation without external dependencies.
- Support both quick and deep modes for observability:
  - Simple local mode: one minimal dashboard and 3 to 6 core metrics.
  - Advanced mode: multi-pane dashboard with per-protocol/component metrics, alerts, and drill-down views.
- Always generate C4 architecture diagrams (Context and Container minimum; Component when useful) that visualize the exact deployed flow, protocol boundaries, and data movement paths.
- Include one diagram suited for local test topology and one diagram for advanced/production topology when both are discussed.

## Reference Loading

Load only what is needed for the task:
- Protocol/link mapping: `/Users/krital/dev/starsense/mapsmessaging_server/skills/mapsmessaging-config-builder/references/protocol-and-routing-map.md`
- Final response shape: `/Users/krital/dev/starsense/mapsmessaging_server/skills/mapsmessaging-config-builder/references/output-contract.md`
