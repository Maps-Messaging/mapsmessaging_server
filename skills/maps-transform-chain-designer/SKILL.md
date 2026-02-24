---
name: maps-transform-chain-designer
description: Design and validate deterministic MAPS transformation chains. Use when requests require ordered payload transformation (CloudEvent wrapping, JSON mutation, format conversion, enrichment), content-type transitions, schema compatibility checks, and deployable configuration updates with runtime verification.
---

# MAPS Transform Chain Designer

Translate transformation requirements into ordered, deterministic MAPS configuration and verification steps.

## Workflow

1. Normalize transform contract.
- Extract source format, target format, required enrichment, CloudEvent wrapping needs, namespace routing, and schema constraints.
- Resolve ambiguity only when ordering or output format would differ.

2. Design ordered chain.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-transform-chain-designer/references/transform-chain-patterns.md`.
- Produce explicit stage order with input/output contentType at each step.
- Enforce deterministic, non-throwing behavior.

3. Map chain to MAPS config.
- Apply relevant updates to manager YAML files in this repo.
- Ensure stage configuration is consistent with destination `schemaId` and `contentType` expectations.

4. Validate chain compatibility.
- Check format handoffs between consecutive stages.
- Verify no stage expects fields absent from prior stage output.
- Include static checks and runtime smoke checks.

5. Return using output contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-transform-chain-designer/references/output-contract.md`.
- Include ordered stages, deployable config, apply steps, and test commands.

## Rules

- Never reorder user-required stages unless impossible; if impossible, explain and provide nearest valid order.
- Keep chain minimal: no redundant transforms.
- Preserve MAPS-native transformer behavior and avoid external processing dependencies.
- Always declare expected `contentType` progression across stages.

## Scenario Modes

- `Simple Local Default`:
  - Build one 2 to 3 stage chain with clear input/output content types and one verification flow.
  - Use minimal transformations to validate baseline behavior quickly.
- `Advanced Combination Matrix`:
  - Provide 3 to 6 chain variants with differing stage order constraints and format transitions.
  - Include stage compatibility risk notes and recommended variant selection.

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
- Chain patterns and checks: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-transform-chain-designer/references/transform-chain-patterns.md`
- Final response format: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-transform-chain-designer/references/output-contract.md`
