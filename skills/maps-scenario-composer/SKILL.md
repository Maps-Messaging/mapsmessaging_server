---
name: maps-scenario-composer
description: Combine scenarios from multiple MAPS skills into one unified deliverable. Use when requirements span multiple domains (for example deployment + selectors + geospatial + ML + satellite) and need one coherent deployable entity with merged apply steps, verification, metrics/dashboard outputs, and C4 diagrams.
---

# MAPS Scenario Composer

Compose multi-skill scenario outputs into one deployable, testable, and conflict-resolved delivery package.

## Workflow

1. Normalize composition contract.
- Extract requested scenario blocks and source skills.
- Capture dependencies, ordering constraints, and shared config surfaces.
- Identify conflicts (port reuse, topic collisions, incompatible auth/storage assumptions, duplicate routes).

2. Build composition plan.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer/references/composition-patterns.md`.
- Define per-skill scenario slices and merge strategy.
- Decide composition mode:
  - additive (independent scenarios merged)
  - staged (scenario outputs feed downstream scenarios)
  - hybrid (subset staged, subset additive)

3. Merge deployable entities.
- Produce one unified artifact manifest and merged deployable config blocks.
- Keep source ownership annotations per merged section.
- Resolve collisions deterministically (explicit precedence and fallback paths).
- When concrete artifacts are available, use composer script inputs:
  - `--artifact <skill>=<path>` for each source scenario output.
  - `--out-dir <path>` to emit concrete merged YAML files.
- Select conflict policy explicitly for deterministic behavior:
  - `--conflict-policy strict-fail`
  - `--conflict-policy override`
  - `--conflict-policy rename`

4. Build unified execution plan.
- One apply sequence covering all merged artifacts.
- One integrated startup/listener verification sequence.
- One integrated runtime smoke and cross-scenario verification sequence.
- Prefer bundled scripts for repeatable validation:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer/scripts/run_scenario_composer_skill_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer/scripts/run_maps_scenario_composer_runtime_smoke.sh`

5. Return using output contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer/references/output-contract.md`.
- Include composition matrix, merged deployable entity, and consolidated verification.

## Rules

- Never return disconnected per-skill outputs; always produce one coherent deliverable.
- Always declare conflict-resolution decisions explicitly.
- Always preserve traceability: merged section -> source skill(s).
- Always include cross-scenario verification, not only per-scenario checks.

## Scenario Modes

- `Simple Local Default`:
  - Combine 2 scenarios from different skills into one local deployable profile.
  - Include minimal unified apply and smoke sequence.
- `Advanced Combination Matrix`:
  - Combine 3 to 8 scenario slices across skills.
  - Provide 2 to 4 composition variants and recommend one.

## Observability and Architecture Outputs

- Always generate combined scenario metrics across all included scenario slices.
- Always provide two dashboard options:
  - Grafana-ready combined dashboard definition.
  - MAPS-hosted combined dashboard definition.
- Support both quick and deep observability:
  - Simple mode: one combined health dashboard and 5 to 8 core metrics.
  - Advanced mode: per-scenario panes plus cross-scenario dependency health.
- Always generate C4 diagrams (Context + Container required, Component required for staged/hybrid composition).

## Reference Loading

Load only what is needed:
- Composition design patterns: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer/references/composition-patterns.md`
- Final response structure: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer/references/output-contract.md`
- Optional generator script: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer/scripts/compose_scenarios.py`
