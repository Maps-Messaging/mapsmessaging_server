# Skill Combo Smoke Usage

Optional Buildkite step: `:test_tube: Skill Combo Smoke (Optional)`

This step runs only when `SKILL_COMBO` is set.

## Environment Variables

- `SKILL_COMBO`
  - Required to activate combo smoke.
  - Comma-separated skill names.
  - Example:
    - `maps-selector-rule-engineer,maps-geospatial-routing-builder,maps-ml-model-lifecycle-playbook`

- `SKILL_COMBO_DEEP`
  - Optional.
  - `1` to run per-skill deep smoke scripts when available.
  - `0` or unset for contract-only combination smoke.

- `SKILL_COMBO_BOOTSTRAP`
  - Optional.
  - `1` to regenerate fixtures before combo smoke.
  - `0` or unset to use existing fixtures.

## Common Examples

Contract-only combo smoke:

```bash
SKILL_COMBO="maps-deployment-packager,maps-selector-rule-engineer,maps-geospatial-routing-builder"
```

Deep combo smoke:

```bash
SKILL_COMBO="maps-selector-rule-engineer,maps-geospatial-routing-builder,maps-ml-model-lifecycle-playbook,maps-scenario-composer"
SKILL_COMBO_DEEP=1
```

Deep combo smoke with fixture refresh:

```bash
SKILL_COMBO="mapsmessaging-config-builder,maps-runtime-diagnostics,maps-protocol-bridge-tester,maps-satellite-gateway-config,maps-schema-pipeline-builder,maps-transform-chain-designer,maps-aggregator-config-engineer,maps-canbus-ingestion-builder,maps-deployment-packager,maps-release-readiness-checker,maps-ml-stream-configurator,maps-artifact-execution-smoke-harness,maps-selector-rule-engineer,maps-geospatial-routing-builder,maps-ml-model-lifecycle-playbook,maps-scenario-composer,maps-skill-suite-orchestrator"
SKILL_COMBO_DEEP=1
SKILL_COMBO_BOOTSTRAP=1
```

## Notes

- The combo step is optional and does not replace the per-skill required smoke gates.
- Skill names must match folder names under `/Users/krital/dev/starsense/mapsmessaging_server/skills`.
