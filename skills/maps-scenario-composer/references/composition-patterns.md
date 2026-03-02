# Composition Patterns

## Pattern A: Additive Composition

- Merge independent scenarios with minimal overlap.
- Keep apply steps sequential but isolated by section.
- Use when scenarios share runtime but not data path.

## Pattern B: Staged Composition

- Output of scenario A becomes input of scenario B.
- Requires explicit intermediate topics/destinations.
- Use when chaining transformations/selectors/ML stages.

## Pattern C: Hybrid Composition

- Mix additive and staged sections.
- Group by dependency and failure domain.
- Use when combining deployment/runtime concerns with pipeline concerns.

## Conflict Resolution Guide

Resolve in this order:

1. listener/port conflicts
2. namespace/topic conflicts
3. auth/storage/config-source conflicts
4. selector/route precedence conflicts
5. verification ownership conflicts

## Conflict Policy Profiles

- `strict-fail`
  - Any scalar/type conflict aborts composition.
  - Use for high-integrity release pipelines.
- `override`
  - Later skill in `--skills` order overrides conflicting scalar/type values.
  - Use for fast iteration and deterministic precedence.
- `rename`
  - Conflicting scalar/type keys are preserved by renaming incoming key with source suffix.
  - Use when preserving both variants is required for review.

## Runnable Composer

Generate a unified deliverable with concrete merged YAML:

```bash
python3 skills/maps-scenario-composer/scripts/compose_scenarios.py \
  --skills maps-deployment-packager,maps-selector-rule-engineer,maps-geospatial-routing-builder \
  --mode hybrid \
  --conflict-policy override \
  --artifact maps-selector-rule-engineer=skills/maps-selector-rule-engineer/references/examples/simple-selector-output.md \
  --artifact maps-geospatial-routing-builder=skills/maps-geospatial-routing-builder/references/examples/simple-geospatial-output.md \
  --out-dir /tmp/maps-composed
```
