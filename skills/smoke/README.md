# Skill Smoke Tests

Run smoke coverage for:

- all skill definitions (`SKILL.md` + `agents/openai.yaml` + `quick_validate`)
- all generated artifact fixtures (validated against each skill's `references/output-contract.md`)

## Usage

```bash
./skills/smoke/run.sh
```

Generate or refresh fixtures first:

```bash
./skills/smoke/run.sh --bootstrap-fixtures
```

Run any skill combination (contract smoke):

```bash
./skills/smoke/run_skill_combination.sh --skills maps-selector-rule-engineer,maps-geospatial-routing-builder
```

Run any skill combination with deep per-skill smoke (when scripts exist):

```bash
./skills/smoke/run_skill_combination.sh --skills maps-selector-rule-engineer,maps-geospatial-routing-builder,maps-ml-model-lifecycle-playbook --deep --bootstrap-fixtures
```
