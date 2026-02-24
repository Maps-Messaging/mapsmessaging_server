---
name: maps-runtime-diagnostics
description: Diagnose MAPS Messaging Server runtime failures and degraded startup behavior. Use when listeners do not bind, protocol endpoints are unavailable, MQTT/AMQP publish-subscribe tests fail, container startup aborts, licensing or provider issues appear, or configuration changes need root-cause analysis with exact remediation and verification steps.
---

# MAPS Runtime Diagnostics

Diagnose MAPS instance health from startup to protocol smoke tests. Produce actionable root-cause findings and exact commands to verify the fix.

## Workflow

1. Capture runtime context.
- Identify deployment mode (local JVM, Docker, Kubernetes).
- Collect target config files and expected listeners (for example `1883`, `5672`, `8080`).
- Record the precise failing operation and timestamp.

2. Run startup-gate checks first.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-runtime-diagnostics/references/startup-gates-and-bind-checks.md`.
- Detect hard blockers: license validation, Consul bootstrap abort, missing protocol providers, malformed YAML.
- Detect bind blockers: address in use, unsupported endpoint transport, invalid URL scheme.

3. Verify listener state.
- Compare expected listener matrix vs actual bound sockets.
- Validate container port publishing and in-container bind simultaneously.
- Confirm protocol adapter availability before smoke tests.

4. Execute protocol smoke tests.
- Run producer/consumer checks only after startup gates are clean.
- For cross-protocol paths, test ingress and egress independently before end-to-end routing.
- Prefer bundled scripts for repeatable validation:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-runtime-diagnostics/scripts/run_maps_runtime_diagnostics_skill_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-runtime-diagnostics/scripts/run_maps_runtime_diagnostics_runtime_smoke.sh`

5. Return diagnostics using output contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-runtime-diagnostics/references/output-contract.md`.
- Include root cause, evidence commands, exact remediation, and post-fix verification.

## Diagnostic Rules

- Prioritize root-cause evidence from logs and bind state over assumptions.
- Do not attribute failures to port collisions unless logs or socket tables prove it.
- Separate startup failures from protocol-data-path failures.
- Prefer MAPS-native fixes (config/runtime flags/image compatibility) over external system substitutions.
- Include concrete success criteria (listener bound, CONNACK received, end-to-end route observed).

## Scenario Modes

- `Simple Local Default`:
  - Run minimal triage: startup logs, listener bind check, one protocol smoke command.
  - Return one likely fix path plus one rerun command set.
- `Advanced Combination Matrix`:
  - Evaluate 3 to 5 possible failure domains (startup gate, provider mismatch, bind, routing, transform).
  - Rank by confidence and provide targeted diagnostics/remediation per domain.

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
- Startup and bind triage: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-runtime-diagnostics/references/startup-gates-and-bind-checks.md`
- Final response shape: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-runtime-diagnostics/references/output-contract.md`
