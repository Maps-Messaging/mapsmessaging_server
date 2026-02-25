---
name: maps-protocol-bridge-tester
description: Build and execute MAPS Messaging protocol bridge test plans. Use when validating protocol ingress and egress behavior, cross-protocol routing (for example MQTT to AMQP, MQTT to WS, CoAP to MQTT), namespace mapping correctness, and end-to-end delivery with reproducible producer and consumer commands and pass-fail criteria.
---

# MAPS Protocol Bridge Tester

Create deterministic bridge tests for MAPS protocol flows and verify end-to-end delivery with explicit evidence.

## Workflow

1. Define bridge matrix.
- Identify source protocol, destination protocol, namespace/topic mapping, payload format, QoS/retain expectations.
- Resolve missing details only when they change verification behavior.

2. Validate runtime readiness first.
- Confirm listener binds and provider availability before generating traffic.
- Use `skills/maps-protocol-bridge-tester/references/bridge-test-catalog.md` startup checks.

3. Execute layered tests.
- Layer 1: source ingress test (producer -> source listener).
- Layer 2: destination egress visibility test (consumer on destination side).
- Layer 3: full bridge test with correlation marker in payload.
- For executable runtime smoke, use:
  - `skills/maps-protocol-bridge-tester/scripts/run_maps_protocol_bridge_tester_runtime_smoke.sh`

4. Evaluate with strict pass/fail markers.
- Pass only when destination receives expected payload on mapped namespace within timeout.
- Separate failures into ingress, routing, transform, or egress categories.

5. Return using output contract.
- Follow `skills/maps-protocol-bridge-tester/references/output-contract.md`.
- Include exact commands, expected outputs, and remediation path for each failed layer.

## Rules

- Do not run end-to-end bridge tests before listener and startup diagnostics are clean.
- Use MAPS-native protocol paths; avoid introducing external brokers unless already in target architecture.
- Prefer reproducible CLI commands with bounded timeouts.
- Always include correlation IDs in payload to prove message continuity across protocol boundaries.

## Scenario Modes

- `Simple Local Default`:
  - Run one baseline bridge path with minimal setup (single ingress, single egress, one correlation ID).
  - Provide one pass/fail assertion set.
- `Advanced Combination Matrix`:
  - Generate 3 to 6 bridge variants across protocol pairs and namespace mappings.
  - Include per-variant ingress, routing, transform/schema, and egress test gates.

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
- Test patterns and commands: `skills/maps-protocol-bridge-tester/references/bridge-test-catalog.md`
- Final response format: `skills/maps-protocol-bridge-tester/references/output-contract.md`
