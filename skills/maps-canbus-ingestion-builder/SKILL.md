---
name: maps-canbus-ingestion-builder
description: Build deployable MAPS CAN bus ingestion configurations for canbus endpoints and N2K protocol processing. Use when requirements include SocketCAN or vcan device setup, J1939 or N2K parsing behavior, topic template mapping, JSON conversion settings, raw frame forwarding, and protocol fan-out verification.
---

# MAPS CAN Bus Ingestion Builder

Convert CAN ingestion requirements into deployable MAPS configs with deterministic verification for vcan and native interfaces.

## Workflow

1. Normalize CAN contract.
- Extract interface mode (vcan or native), device name (`can0`, `vcan0`, etc.), protocol behavior (`n2k` decode vs raw canbus), topic naming requirements, parse-to-JSON expectations, and schema/database source.

2. Map to config surfaces.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-canbus-ingestion-builder/references/canbus-mapping-guide.md`.
- Update `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml` for endpoint and protocol binding.
- Update destination/schema mappings only when requested by pipeline behavior.

3. Build deployable entities.
- Prefer minimal diffs.
- Keep endpoint and protocol alignment explicit (`endPointConfig.type: canbus`, `protocolConfigs[].type: n2k` when decoding N2K).
- Use deterministic interface naming (`canbus-<device>-<protocol>`).

4. Validate before finalizing.
- Verify endpoint/protocol consistency and topic templates.
- Prefer bundled scripts for repeatable validation:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-canbus-ingestion-builder/scripts/run_maps_canbus_ingestion_builder_skill_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-canbus-ingestion-builder/scripts/run_maps_canbus_ingestion_builder_runtime_smoke.sh`
- Include runtime checks:
```bash
rg -n "type: canbus|type: n2k|deviceName|topicNameTemplate|parseToJson" /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
rg -n "N2K_PROTOCOL|canbus" /Users/krital/dev/starsense/mapsmessaging_server/src/main/java/io/mapsmessaging/network/protocol/impl/n2k /Users/krital/dev/starsense/mapsmessaging_server/src/main/java/io/mapsmessaging/network/io/impl/canbus
```

5. Return using output contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-canbus-ingestion-builder/references/output-contract.md`.
- Include assumptions, deployable YAML, apply steps, and vcan/native verification.

## CAN-Specific Rules

- Treat vcan and native CAN as separate operational profiles.
- Include `deviceName` and protocol mode explicitly for every generated endpoint.
- Keep parser mode explicit: decoded N2K JSON vs raw canbus frame flow.
- If N2K database path/content is required, state source and fallback behavior.
- Do not claim hardware parity from vcan-only validation.

## Scenario Modes

- `Simple Local Default`:
  - Use a vcan profile with one device and one decode mode for fast local validation.
  - Include one injection path and one mapped-topic assertion.
- `Advanced Combination Matrix`:
  - Provide 3 to 5 variants across vcan/native, raw/decode mode, topic templates, and downstream routing targets.
  - Include hardware-specific caveats and recommended rollout order.

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

Load only what is needed:
- Mapping and runtime checks: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-canbus-ingestion-builder/references/canbus-mapping-guide.md`
- Response format: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-canbus-ingestion-builder/references/output-contract.md`
