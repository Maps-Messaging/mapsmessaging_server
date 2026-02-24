---
name: maps-skill-suite-orchestrator
description: Master index and invocation orchestrator for the MAPS skill suite. Use when selecting which MAPS skill to run, composing multi-skill workflows, or generating standardized quick-start and advanced prompts that map requirements to deployable outputs with diagnostics, metrics dashboards, and C4 diagrams.
---

# MAPS Skill Suite Orchestrator

Use this as the entrypoint index for all MAPS skills in this repository.

## Purpose

- Route a request to the correct skill quickly.
- Provide consistent prompt templates for `Quick Start` and `Advanced` usage.
- Keep outputs aligned with deployment readiness: deployable config artifacts, verification commands, scenario metrics/dashboard specs, and C4 diagrams.

## Skill Index

1. `mapsmessaging-config-builder`
- Build deployable MAPS manager YAML from protocol, encoding, topology, schema, and routing intent.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/mapsmessaging-config-builder/SKILL.md`

2. `maps-runtime-diagnostics`
- Diagnose startup/runtime failures, bind issues, provider/config blockers, and smoke-test failures.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-runtime-diagnostics/SKILL.md`

3. `maps-protocol-bridge-tester`
- Design and run layered ingress/egress/end-to-end bridge tests with strict pass/fail markers.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-protocol-bridge-tester/SKILL.md`

4. `maps-satellite-gateway-config`
- Generate satellite pub/sub patterns using Orbcomm or Viasat with broad protocol fan-out and CBC SIN/MIN routing.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config/SKILL.md`

5. `maps-schema-pipeline-builder`
- Build schema-aware pipelines across JSON/Protobuf/Avro/CSV/raw with schemaId/contentType mapping.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-schema-pipeline-builder/SKILL.md`

6. `maps-transform-chain-designer`
- Design deterministic ordered transform chains (CloudEvent, mutate, convert, enrich).
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-transform-chain-designer/SKILL.md`

7. `maps-aggregator-config-engineer`
- Configure unique MAPS windowed aggregation behavior and validate window/timeout/fairness semantics.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-aggregator-config-engineer/SKILL.md`

8. `maps-canbus-ingestion-builder`
- Build CAN/N2K ingestion profiles for `vcan` and native interfaces with deterministic topic mapping.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-canbus-ingestion-builder/SKILL.md`

9. `maps-deployment-packager`
- Package deployables for local, Docker, Kubernetes-style bundles, Fly.io, AWS, GCP, and Azure.
- Includes auth, storage, file-vs-Consul mode, Fly KV options, optional Consul (HCP/self-managed), and object storage (S3/R2).
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-deployment-packager/SKILL.md`

10. `maps-release-readiness-checker`
- Run evidence-based release gates with Docker image availability and smoke checks.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-release-readiness-checker/SKILL.md`

11. `maps-ml-stream-configurator`
- Build simple-to-advanced ML selector stream pipelines with staged intermediate destinations and model chaining.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-stream-configurator/SKILL.md`

12. `maps-artifact-execution-smoke-harness`
- Execute generated artifacts end-to-end and return startup, listener, and traffic smoke evidence.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/SKILL.md`

13. `maps-selector-rule-engineer`
- Design selector logic for routing rules and subscriber selectors with deterministic precedence and verification.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-selector-rule-engineer/SKILL.md`

14. `maps-geospatial-routing-builder`
- Build geohash and GPS/distance-driven routing behavior with deployable config and geospatial validation.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/SKILL.md`

15. `maps-ml-model-lifecycle-playbook`
- Provide MAPS-native model lifecycle guidance from stream training to external model ingestion for inference.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/SKILL.md`

16. `maps-scenario-composer`
- Combine scenarios from multiple skills into one merged deployable with unified apply and integrated verification.
- Link: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer/SKILL.md`

## Selection Guide

Use this mapping to choose a starting skill:

- Protocol/topology to YAML: `mapsmessaging-config-builder`
- Startup failures or bind/smoke issues: `maps-runtime-diagnostics`
- Cross-protocol delivery verification: `maps-protocol-bridge-tester`
- Satellite service patterns: `maps-satellite-gateway-config`
- Schema registration and conversion: `maps-schema-pipeline-builder`
- Ordered transform design: `maps-transform-chain-designer`
- Windowed aggregate logic: `maps-aggregator-config-engineer`
- CAN/N2K ingestion: `maps-canbus-ingestion-builder`
- Deployment packaging: `maps-deployment-packager`
- Release gate decision: `maps-release-readiness-checker`
- ML selector stream orchestration: `maps-ml-stream-configurator`
- Artifact execution smoke validation: `maps-artifact-execution-smoke-harness`
- Routing/subscriber selector engineering: `maps-selector-rule-engineer`
- Geospatial (geohash/GPS/distance) routing: `maps-geospatial-routing-builder`
- Stream-trained and external-model ML lifecycle: `maps-ml-model-lifecycle-playbook`
- Multi-skill scenario composition into one deliverable: `maps-scenario-composer`

## Standard Prompt Templates

Use these exact templates and replace placeholders.

### Quick Start Template

```text
Use <skill-name> in Simple Local Default mode.
Goal: <one-sentence objective>.
Inputs:
- protocols/features: <...>
- config files in scope: <...>
- constraints: <...>
Output requirements:
- deployable config/artifacts
- apply commands
- startup + listener/smoke verification
- scenario metrics + minimal dashboard
- C4 Context + Container diagrams
Keep it minimal and locally testable.
```

### Advanced Template

```text
Use <skill-name> in Advanced Combination Matrix mode.
Goal: <objective>.
Generate <N> viable variants and recommend one.
Inputs:
- protocol/topology and feature scope: <...>
- auth modes to support: <...>
- storage/volume and config source modes: <...>
- deployment targets: <local|docker|k8s-style|fly|aws|gcp|azure>
- connectivity profile: <connected|air-gapped|degraded|delay-tolerant>
Output requirements:
- per-variant deployable config/artifacts
- tradeoffs and recommended option
- apply + diagnostics + smoke tests
- scenario-specific metrics + dashboards (Grafana + MAPS-hosted)
- C4 diagrams (Context, Container, Component if multi-stage)
```

## Composed Workflow Templates

### Build then Test

```text
1) Use mapsmessaging-config-builder in Simple Local Default mode to create deployable config.
2) Use maps-protocol-bridge-tester to verify ingress/egress and end-to-end delivery.
3) Use maps-runtime-diagnostics only if any stage fails.
```

### Deploy then Gate

```text
1) Use maps-deployment-packager in Advanced Combination Matrix mode for <targets>.
2) Use maps-release-readiness-checker to produce PASS/CONDITIONAL PASS/FAIL with evidence.
3) Use maps-artifact-execution-smoke-harness to execute generated artifacts and collect runtime smoke evidence.
```

### Advanced Data Pipeline

```text
1) Use maps-schema-pipeline-builder for schema linkage.
2) Use maps-transform-chain-designer for deterministic stage order.
3) Use maps-aggregator-config-engineer for windowed summary outputs.
4) Use maps-ml-stream-configurator last for selector-based multi-pass decisions.
5) Use maps-ml-model-lifecycle-playbook for stream-trained and external-model lifecycle controls.
```

### Selector and Geospatial Flow

```text
1) Use maps-selector-rule-engineer for routing and subscriber selectors.
2) Use maps-geospatial-routing-builder for geohash and GPS distance-based decisions.
3) Use maps-protocol-bridge-tester for end-to-end route evidence.
```

### Multi-Skill Unified Deliverable

```text
1) Build scenario slices with relevant source skills.
2) Use maps-scenario-composer to merge slices into one unified deployable.
3) Use maps-artifact-execution-smoke-harness for integrated runtime evidence.
```

## Output Baseline Requirements

Every skill output should include:

- Explicit assumptions and selected variant/profile.
- Deployable artifacts or full YAML blocks.
- Apply commands and rollback path.
- Startup diagnostics and listener/protocol verification.
- Scenario metrics + dashboard definition.
- C4 diagram source in Mermaid.

## Validation Scripts

- `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-skill-suite-orchestrator/scripts/run_maps_skill_suite_orchestrator_skill_smoke.sh`
- `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-skill-suite-orchestrator/scripts/run_maps_skill_suite_orchestrator_runtime_smoke.sh`

## Reference Loading

Read only required skill files:

- `/Users/krital/dev/starsense/mapsmessaging_server/skills/<target-skill>/SKILL.md`

Load reference docs only for the selected target skill and scenario mode.
