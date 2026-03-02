---
name: maps-selector-rule-engineer
description: Design and validate MAPS selector logic for routing rules and subscriber selectors. Use when requirements include conditional routing, selector expressions, multi-stage filter chains, selector performance tuning, or conflict resolution across destination routing and subscriber-level selection.
---

# MAPS Selector Rule Engineer

Build clear, testable selector configurations for routing and subscriptions, from simple filters to advanced chained selection logic.

## Workflow

1. Normalize selector contract.
- Extract selector intent, source topics, output destinations, subscriber selector behavior, and expected match/non-match outcomes.
- Identify whether selectors apply at routing layer, subscription layer, or both.

2. Build selector design.
- Read `skills/maps-selector-rule-engineer/references/selector-patterns.md`.
- Create explicit selector evaluation order.
- Define conflict policy (first-match, priority-based, or layered pass).

3. Map to MAPS config surfaces.
- Update manager YAML files relevant to routing and destination/subscription behavior.
- Keep selector expressions and routing paths deterministic and explicit.

4. Validate selector semantics.
- Include positive and negative test vectors.
- Verify no contradictory selector overlap unless intentionally prioritized.
- Include runtime verification commands with correlation payload markers.
- Prefer bundled scripts for repeatable validation:
  - `skills/maps-selector-rule-engineer/scripts/run_selector_skill_smoke.sh`
  - `skills/maps-selector-rule-engineer/scripts/run_maps_selector_rule_engineer_runtime_smoke.sh`
  - `skills/maps-selector-rule-engineer/scripts/run_selector_mqtt_smoke.sh`

5. Return using output contract.
- Follow `skills/maps-selector-rule-engineer/references/output-contract.md`.
- Include selector matrix, deployable config, apply steps, and verification.

## Rules

- Always show selector evaluation order.
- Never claim correctness without both match and non-match evidence.
- Prefer MAPS-native selector/routing mechanisms over external filtering services.
- Include complexity notes for selectors likely to impact throughput.

## Scenario Modes

- `Simple Local Default`:
  - One routing selector and one subscriber selector with clear pass/fail examples.
- `Advanced Combination Matrix`:
  - Provide 3 to 6 variants across routing-level, subscriber-level, and chained selector patterns.
  - Include one recommended operationally simple variant.

## Observability and Architecture Outputs

- Always generate scenario-specific metrics mapped to selector behavior (selector hit rate, miss rate, route divergence, dropped-on-filter count, selector latency).
- Always provide two dashboard options:
  - Grafana-ready panel and query specification (or JSON model when requested).
  - MAPS-hosted dashboard view specification.
- Support both quick and deep observability:
  - Simple mode: one minimal selector health panel and 3 to 6 metrics.
  - Advanced mode: per-selector and per-route breakdown with alerts.
- Always generate C4 diagrams (Context and Container minimum; Component for multi-stage selectors).

## Reference Loading

Load only what is needed:
- Selector design patterns: `skills/maps-selector-rule-engineer/references/selector-patterns.md`
- Final response format: `skills/maps-selector-rule-engineer/references/output-contract.md`
- Example artifacts:
  - `skills/maps-selector-rule-engineer/references/examples/simple-selector-output.md`
  - `skills/maps-selector-rule-engineer/references/examples/advanced-selector-output.md`
