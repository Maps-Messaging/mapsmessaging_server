---
name: maps-artifact-execution-smoke-harness
description: Execute generated MAPS artifacts end-to-end as smoke gates. Use when validating that skill-generated deployable YAML, deployment bundles, and command sets actually boot, bind listeners, route data, and satisfy runtime assertions in local or Docker environments.
---

# MAPS Artifact Execution Smoke Harness

Run executable smoke gates against generated artifacts, not only document-level contract checks.

## Workflow

1. Normalize smoke target contract.
- Extract artifact source (skill output, file bundle, generated patch set).
- Identify runtime mode (local JVM or Docker).
- Identify expected listeners, namespaces, routes, and success markers.

2. Build execution plan.
- Read `skills/maps-artifact-execution-smoke-harness/references/execution-catalog.md`.
- Define phases: setup, apply, startup, listener verification, traffic verification, teardown.
- Require explicit timeout and fail-fast conditions per phase.
- Prefer bundled scripts when direct execution is requested:
  - `skills/maps-artifact-execution-smoke-harness/scripts/run_artifact_smoke.sh`
  - `skills/maps-artifact-execution-smoke-harness/scripts/run_matrix.sh`
  - `skills/maps-artifact-execution-smoke-harness/scripts/run_maps_artifact_execution_smoke_harness_skill_smoke.sh`
  - `skills/maps-artifact-execution-smoke-harness/scripts/run_maps_artifact_execution_smoke_harness_runtime_smoke.sh`

3. Execute smoke phases.
- Setup: ensure ports are free and prior containers/processes are cleaned up safely.
- Apply: write or mount artifact set exactly as generated.
- Startup: launch MAPS and capture startup logs.
- Listener verification: prove required listeners are bound.
- Traffic verification: run protocol publish/consume or route assertions.
- Teardown: stop containers/processes and collect logs/artifacts.

4. Evaluate and classify failures.
- Classify as apply, startup gate, bind, routing, transform/schema, or protocol egress failures.
- Separate environment issues from artifact defects.

5. Return using output contract.
- Follow `skills/maps-artifact-execution-smoke-harness/references/output-contract.md`.
- Include exact commands, observed outcomes, and minimal rerun command set.

## Rules

- Never mark smoke PASS without startup plus listener plus traffic evidence.
- Never skip teardown commands.
- Prefer deterministic command blocks with bounded timeouts.
- Include absolute paths for all applied artifacts.

## Scenario Modes

- `Simple Local Default`:
  - Run one local or Docker artifact with one protocol verification path.
  - Return one PASS/FAIL decision and minimal remediation path.
- `Advanced Combination Matrix`:
  - Run 3 to 6 artifact variants across protocol/deployment/connectivity profiles.
  - Provide comparative outcome matrix and recommended promotion candidate.

## Observability and Architecture Outputs

- Always generate scenario-specific metrics mapped to smoke execution (startup time, bind success rate, route success rate, error counts, retry/recovery timings).
- Always provide two dashboard options:
  - Grafana-ready panel and query specification (or JSON model when requested).
  - MAPS-hosted dashboard view specification (REST/WS backed) for local-first operation without external dependencies.
- Support both quick and deep modes for observability:
  - Simple local mode: one minimal dashboard and 3 to 6 core metrics.
  - Advanced mode: multi-pane dashboard with per-phase and per-protocol smoke metrics.
- Always generate C4 architecture diagrams (Context and Container minimum; Component when useful) that visualize the executed flow and test harness boundaries.

## Reference Loading

Load only what is needed:
- Execution phases and commands: `skills/maps-artifact-execution-smoke-harness/references/execution-catalog.md`
- Final response format: `skills/maps-artifact-execution-smoke-harness/references/output-contract.md`
