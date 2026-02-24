---
name: maps-satellite-gateway-config
description: Build deployable MAPS satellite gateway configurations for Iridium-style pub-sub service patterns using Orbcomm or Viasat provider profiles and flexible protocol ingress and egress options. Use when requirements include satellite endpoint setup, polling lifecycle, fragmentation and reassembly, priority handling, namespace publish boundaries, and multi-protocol delivery choices with verification and diagnostics.
---

# MAPS Satellite Gateway Config

Generate Iridium-like pub-sub service patterns using MAPS satellite capabilities, with Orbcomm and Viasat-oriented profiles plus selectable protocol endpoints.

## Workflow

1. Normalize satellite service contract.
- Extract provider profile (Orbcomm or Viasat), traffic direction (uplink, downlink, bidirectional), polling intervals, message lifetime, priority policy, payload size assumptions, and namespace boundary.
- Capture required ingress and egress protocol options (MQTT, MQTT-SN, AMQP, STOMP, NATS, CoAP, WS, REST).
- Capture encoding mode. If encoding is CBC, enable SIN/MIN hierarchy routing policy.

2. Build protocol option matrix.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/references/satellite-pubsub-patterns.md`.
- Produce at least 2 viable pattern variants when user asks for "more choices".
- For each variant, specify source and destination protocol path, mapping namespace, and operational tradeoffs.

3. Map selected variant to MAPS config surfaces.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/references/satellite-config-map.md`.
- Update `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml` satellite endpoint and protocol blocks.
- Update `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml` and `/Users/krital/dev/starsense/mapsmessaging_server/routing.yaml` only when required by chosen pattern.

4. Build deployable entities.
- Prefer minimal diffs.
- Keep endpoint and protocol `type: satellite` aligned.
- Use deterministic interface naming (`sat-<provider>-<direction>-<protocol>`).

5. Validate before finalizing.
- Confirm provider-profile assumptions, poll cadence bounds, and protocol listener compatibility.
- Prefer bundled scripts for repeatable validation:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/scripts/run_maps_satellite_gateway_config_skill_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/scripts/run_maps_satellite_gateway_config_runtime_smoke.sh`
- Include startup/provider checks:
```bash
rg -n "type: satellite|satellite://|Inmarsat|Orbcomm|Viasat" /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
docker logs <container> 2>&1 | rg -n "satellite|OGWS|Inmarsat|Viasat|Protocol not available|Startup aborted"
```

6. Return with output contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/references/output-contract.md`.
- Include pattern matrix, selected profile, deployable YAML, diagnostics, and verification.

## Satellite-Specific Rules

- Treat satellite as a publish boundary into MAPS namespaces.
- Keep polling lifecycle explicit (incoming and outgoing cadence).
- Preserve fragmentation and reassembly expectations; do not claim large-payload success without configured support.
- Include priority-path costs/tradeoffs where high-priority bypass behavior is enabled.
- If requested provider profile is not runtime-supported, emit nearest valid MAPS config with explicit fallback note.
- Prefer MAPS-native bridging over external message brokers.
- Enforce CBC auto-routing policy: when CBC-encoded messages carry unsigned SIN or MIN values under `127`, route to individual SIN/MIN topic hierarchies (for example `.../{sin}/{min}` paths) instead of generic shared topics.
- Include explicit namespace path templates for CBC routing outputs and corresponding verification commands.

## Scenario Modes

- `Simple Local Default`:
  - Produce one minimal satellite profile with one downstream protocol path and one publish-boundary verification.
  - Use conservative polling and queue defaults suitable for local validation.
- `Advanced Combination Matrix`:
  - Provide 3 to 6 variants across provider profile, directionality, encoding (including CBC), and downstream protocol fan-out options.
  - Include fallback behavior per variant when provider/protocol support is unavailable.

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
- Service-pattern variants: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/references/satellite-pubsub-patterns.md`
- Config mapping and diagnostics: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/references/satellite-config-map.md`
- Output format: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/references/output-contract.md`
