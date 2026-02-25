---
name: maps-schema-pipeline-builder
description: Build schema-aware MAPS Messaging configuration pipelines. Use when requirements include schema registration, schemaId assignment, validation and transformation behavior, content-type normalization, or format conversion across JSON, Protobuf, Avro, CSV, raw binary, and raw text with deployable manager YAML changes.
---

# MAPS Schema Pipeline Builder

Translate schema and format requirements into deployable MAPS configuration entities with deterministic verification.

## Workflow

1. Normalize schema contract.
- Extract payload formats, schema source, namespace mapping, required schemaId behavior, validation strictness, and transformation order.
- Resolve missing details only when schema behavior would change.

2. Map contract to configuration surfaces.
- Read `skills/maps-schema-pipeline-builder/references/schema-mapping-guide.md`.
- Map schema-aware message behavior to:
  - `SchemaManager.yaml`
  - `DestinationManager.yaml`
  - `NetworkManager.yaml`
- If required, include relevant transformation manager config paths.

3. Build deployable entities.
- Prefer minimal file diffs.
- For deployment packaging requests, emit full YAML bodies or ConfigMap manifests.
- Keep deterministic IDs and names for schema entities.

4. Validate before finalizing.
- Check key fields and linkage consistency (`schemaId`, `contentType`, namespace mapping).
- Run targeted checks:
```bash
rg -n "schemaId|contentType|messageOverride|qualityOfService" DestinationManager.yaml
rg -n "SchemaManager|schema|type:" SchemaManager.yaml
rg -n "protocolConfigs|messageDefaults|contentType" NetworkManager.yaml
```

5. Return output using output contract.
- Follow `skills/maps-schema-pipeline-builder/references/output-contract.md`.
- Always include assumptions, deployable YAML, apply steps, and validation commands.

## Rules

- Keep transformations deterministic and ordered.
- Preserve existing DTO naming/casing and manager structure.
- Use MAPS-native schema and transformation paths before suggesting external systems.
- Explicitly state unsupported or unavailable schema providers and output nearest valid config.

## Scenario Modes

- `Simple Local Default`:
  - Produce one schema registration plus one namespace schemaId/contentType mapping.
  - Include minimal producer/consumer verification.
- `Advanced Combination Matrix`:
  - Provide 3 to 5 schema pipeline variants across JSON/Protobuf/Avro/CSV/raw pathways.
  - Include compatibility notes for schema linkage, transformation order, and destination expectations.

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
- Schema mapping details: `skills/maps-schema-pipeline-builder/references/schema-mapping-guide.md`
- Response format: `skills/maps-schema-pipeline-builder/references/output-contract.md`
