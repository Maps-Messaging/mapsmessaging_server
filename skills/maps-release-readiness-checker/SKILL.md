---
name: maps-release-readiness-checker
description: Assess MAPS Messaging release readiness using evidence-based operational gates. Use when preparing a release candidate, validating snapshot or release artifacts, confirming Docker image availability, running Docker image smoke tests, checking protocol and manager startup health, and producing pass-fail findings with remediation and rerun criteria.
---

# MAPS Release Readiness Checker

Run a structured release gate review and return a defensible release decision with concrete evidence.

## Workflow

1. Define release scope.
- Identify candidate type: snapshot, RC, or final release.
- Identify deployment targets and mandatory protocol/feature coverage.

2. Execute build and test gates.
- Validate build pipeline commands and unit/integration test expectations.
- Capture failing gates with exact evidence.

3. Execute image availability and container smoke gates.
- Confirm required Docker image tags are available (local or registry-accessible per release scope).
- Start a container from the candidate image and run smoke checks for startup logs and listener binds.
- Capture image-level failures separately from application configuration failures.

4. Execute runtime startup gates.
- Check startup abort blockers (license, Consul/config gating, provider availability).
- Check listener binds and core protocol availability.
- Prefer bundled scripts for repeatable validation:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-release-readiness-checker/scripts/run_maps_release_readiness_checker_skill_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-release-readiness-checker/scripts/run_maps_release_readiness_checker_runtime_smoke.sh`

5. Execute feature-specific operational gates.
- Run checks for configured unique features (aggregator, satellite, schema/transform, CAN, etc.) based on release scope.
- Require destination-side evidence for protocol paths.

6. Evaluate deployment readiness.
- Confirm packaging artifacts and deployment commands are complete and reproducible.
- Confirm rollback procedure is present and minimal.

7. Return readiness decision.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-release-readiness-checker/references/output-contract.md`.
- Provide PASS, CONDITIONAL PASS, or FAIL with blocking/non-blocking findings.

## Rules

- Never mark PASS without evidence from build, Docker image smoke, startup, and runtime checks.
- Separate blockers from warnings.
- Prefer reproducible command evidence over narrative claims.
- Include explicit rerun criteria after remediation.

## Scenario Modes

- `Simple Local Default`:
  - Run one compact readiness sweep (build command, one Docker smoke container, one protocol smoke check).
  - Return a concise PASS/FAIL with minimal rerun set.
- `Advanced Combination Matrix`:
  - Run 3 to 5 readiness profiles by release scope (snapshot/RC/final, protocol sets, feature gates).
  - Provide comparative gate outcomes and recommended release decision.

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
- Gate catalog and commands: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-release-readiness-checker/references/release-gate-catalog.md`
- Final response format: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-release-readiness-checker/references/output-contract.md`
