---
name: maps-geospatial-routing-builder
description: Build MAPS geospatial routing configurations using geohash, GPS coordinates, and distance-based decision logic. Use when requirements include geohash topic partitioning, geofence-like routing, GPS payload normalization, proximity thresholds, or distance-function-driven route selection.
---

# MAPS Geospatial Routing Builder

Translate location-aware requirements into deployable MAPS routing and transformation configurations with deterministic verification.

## Workflow

1. Normalize geospatial contract.
- Extract coordinate format, geohash precision requirements, distance thresholds, target destinations, and fallback rules.
- Identify whether routing is geohash-bucketed, distance-driven, or both.

2. Design geospatial routing model.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/references/geospatial-patterns.md`.
- Define coordinate normalization and unit assumptions.
- Define precision strategy (coarse vs fine geohash) and route fan-out behavior.

3. Map to MAPS config surfaces.
- Update relevant manager YAML for routing, destination mapping, and transformations.
- Keep geospatial computations deterministic and explicit.

4. Validate semantics.
- Include known-coordinate test vectors.
- Validate geohash output paths and distance threshold transitions.
- Verify destination evidence for in-range and out-of-range cases.
- Prefer bundled scripts for repeatable validation:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_skill_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_maps_geospatial_routing_builder_runtime_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_vectors.py`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_mqtt_smoke.sh`

5. Return using output contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/references/output-contract.md`.
- Include deployable config, apply steps, and geospatial verification commands.

## Rules

- Always declare coordinate reference assumptions and units.
- Always include at least one known-distance sanity check.
- Prefer MAPS-native transform/routing capabilities (for example GeoHash resolver behavior where available).
- Never omit fallback path for invalid or missing GPS fields.

## Scenario Modes

- `Simple Local Default`:
  - One geohash route and one proximity threshold route with minimal test vectors.
- `Advanced Combination Matrix`:
  - Provide 3 to 6 variants across geohash precision, distance thresholds, and multi-zone routing.
  - Include one recommended variant balancing accuracy and operational simplicity.

## Observability and Architecture Outputs

- Always generate scenario-specific geospatial metrics (geohash distribution, distance-eval count, in-range/out-of-range split, invalid-coordinate rate, routing latency).
- Always provide two dashboard options:
  - Grafana-ready panel and query specification (or JSON model when requested).
  - MAPS-hosted dashboard view specification.
- Support both quick and deep observability:
  - Simple mode: one geospatial health panel and 3 to 6 metrics.
  - Advanced mode: per-zone and per-threshold drill-down with alerts.
- Always generate C4 diagrams (Context and Container minimum; Component when multiple geo stages are used).

## Reference Loading

Load only what is needed:
- Geospatial patterns: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/references/geospatial-patterns.md`
- Final response format: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/references/output-contract.md`
- Example artifacts:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/references/examples/simple-geospatial-output.md`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/references/examples/advanced-geospatial-output.md`
