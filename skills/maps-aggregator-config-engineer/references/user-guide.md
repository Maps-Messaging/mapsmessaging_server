# MAPS Aggregator Config Engineer - User Guide

## What this skill does

`maps-aggregator-config-engineer` generates and validates deployable `AggregatorManager.yaml` entities for MAPS windowed aggregation flows.

Use it when you need:
- multiple input topics aggregated into one output topic
- explicit `windowDurationMs` / `timeoutMs` behavior
- per-input `contributionMode` (`FIRST` or `LAST`)
- verification commands, metrics, dashboards, and C4 diagrams

## Installation

### Option A: Install this one skill

```bash
python3 ~/.codex/skills/.system/skill-installer/scripts/install-skill-from-github.py \
  --repo <owner>/<repo> \
  --path skills/maps-aggregator-config-engineer
```

### Option B: Install multiple skills in one command

You do not need to install one at a time. The installer accepts multiple `--path` entries:

```bash
python3 ~/.codex/skills/.system/skill-installer/scripts/install-skill-from-github.py \
  --repo <owner>/<repo> \
  --path skills/maps-aggregator-config-engineer \
  --path skills/maps-runtime-diagnostics \
  --path skills/maps-release-readiness-checker
```

### Option C: Install a full skill pack

If your repository groups many skills under `skills/`, users can install each included skill path in one installer run (as in Option B). This is the cleanest "bundle" approach.

After install: restart Codex so newly installed skills are available.

## Usage pattern

Prompt format that works well:

```text
Use maps-aggregator-config-engineer in <mode>.
Goal: <aggregation goal>
Inputs: <topics + timing + contribution mode + limits>
Output requirements: deployable YAML, apply commands, runtime verification, metrics/dashboard, C4.
```

Example:

```text
Use maps-aggregator-config-engineer in Simple Local Default mode.
Goal: Aggregate telemetry from /veh/a and /veh/b to /veh/agg every 5 seconds.
windowDurationMs: 5000
timeoutMs: 7000
contributionMode: LAST
maxEventsPerTopic: 100
```

## Deployment model: individual skills vs a "master" skill

Short answer:
- You can install skills individually.
- You can also deploy many skills together in one installer command.
- A "master" skill does not auto-install other skills by itself.

Details:
- Skills are discovered as separate directories under `~/.codex/skills/<skill-name>`.
- A master/orchestrator skill can document and coordinate workflows, but Codex still needs each dependent skill directory installed to trigger those skills directly.
- For team rollout, prefer a single repository with all skills plus one install command containing multiple `--path` arguments.

## Operational checks

1. Validate skill files exist:

```bash
test -f ~/.codex/skills/maps-aggregator-config-engineer/SKILL.md
```

2. Validate skill definition:

```bash
bash ~/.codex/skills/maps-aggregator-config-engineer/scripts/run_maps_aggregator_config_engineer_skill_smoke.sh
```

3. In Codex, run a small Simple Local Default request and confirm it returns:
- `Aggregation Requirement Mapping`
- `Deployable Config Entity`
- `Apply Steps`
- `Verification Plan`
- `Scenario Metrics and Dashboard`
- `C4 Architecture Diagram`

## Troubleshooting

- Skill not appearing:
  - Confirm folder path is `~/.codex/skills/maps-aggregator-config-engineer`.
  - Restart Codex.
- Install script fails on private repo:
  - set `GITHUB_TOKEN`/`GH_TOKEN` or ensure git credentials are configured.
- Runtime checks fail:
  - verify MAPS container name, mounted config path, and MQTT listener ports.
