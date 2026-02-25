---
name: maps-aggregator-config-engineer
description: Design and validate MAPS AggregatorManager configurations for windowed multi-input aggregation. Use when requirements involve windowDurationMs, timeoutMs, maxEventsPerTopic, contribution modes (FIRST or LAST), topic selectors, output transformers, scheduler controls, or aggregate summary payload behavior and require deployable YAML with verification steps.
---

# MAPS Aggregator Config Engineer

Convert aggregation requirements into deployable `AggregatorManager.yaml` entities with deterministic window behavior and measurable validation.

## Workflow

1. Normalize aggregator contract.
- Extract input topics, output topic, window duration, timeout, contribution mode per input, selector rules, and expected aggregate output shape.
- Capture throughput constraints that drive `maxEventsPerTopic`, `maxBatchPerAggregator`, `mailboxCapacity`, and `stripeCount`.

2. Map to concrete AggregatorManager fields.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-aggregator-config-engineer/references/aggregator-design-guide.md`.
- Update `/Users/krital/dev/starsense/mapsmessaging_server/AggregatorManager.yaml` as primary source.
- Include related destination/schema/transform config only when aggregation output requires it.

3. Build deployable config.
- Prefer minimal diffs and deterministic naming for aggregators.
- Preserve field casing and DTO-compatible keys exactly.
- Keep contribution policy explicit for each input.

4. Validate semantics before finalizing.
- Verify each aggregator has non-empty inputs and output topic.
- Check timing consistency: `timeoutMs >= windowDurationMs` unless explicitly justified.
- Check resource and fairness guardrails: `maxEventsPerTopic`, `maxBatchPerAggregator`, `mailboxCapacity`, `stripeCount`.
- Prefer bundled scripts for repeatable validation:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-aggregator-config-engineer/scripts/run_maps_aggregator_config_engineer_skill_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-aggregator-config-engineer/scripts/run_maps_aggregator_config_engineer_runtime_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-aggregator-config-engineer/scripts/run_maps_aggregator_scenario_e2e.sh`
- Run checks:
```bash
rg -n "aggregatorConfigList|windowDurationMs|timeoutMs|maxEventsPerTopic|contributionMode|outputTopic" /Users/krital/dev/starsense/mapsmessaging_server/AggregatorManager.yaml
```

5. Return output using contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-aggregator-config-engineer/references/output-contract.md`.
- Always include assumptions, deployable YAML, apply steps, and verification with pass/fail criteria.

## Aggregator-Specific Rules

- Treat this as a unique MAPS capability, not a generic stream processor template.
- Do not simplify away per-input contribution modes.
- Keep aggregator window model explicit: arrival-time window + timeout closure behavior.
- Include expected summary payload assertions when requirements mention statistics (`timeStart`, `timeEnd`, `duration`, `sampleCount`, per-field stats).
- If requested behavior relies on unavailable stats/transform providers, state the limitation and provide nearest valid config.

## Scenario Modes

- `Simple Local Default`:
  - Create one aggregator with 2 inputs, `LAST` contribution mode, and conservative window/timeout defaults.
  - Include a minimal publish/consume verification for a single completed window.
- `Advanced Combination Matrix`:
  - Generate 3 to 5 aggregator variants across contribution policies, window sizes, timeout sensitivity, and output transformer usage.
  - Include operational tradeoffs for drop risk, latency, and fairness settings.

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
- Aggregator semantics and patterns: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-aggregator-config-engineer/references/aggregator-design-guide.md`
- Final response contract: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-aggregator-config-engineer/references/output-contract.md`
