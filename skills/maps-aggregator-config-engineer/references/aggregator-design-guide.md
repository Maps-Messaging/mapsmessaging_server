# Aggregator Design Guide

## Core Config Surface

Primary file:
- `/Users/krital/dev/starsense/mapsmessaging_server/AggregatorManager.yaml`

Important root controls:
- `maxAggregators`
- `maxBatchPerAggregator`
- `mailboxCapacity`
- `idleSleepMs`
- `stripeCount`

Per-aggregator controls (`aggregatorConfigList[]`):
- `enabled`
- `name`
- `inputs[]`
- `outputTopic`
- `windowDurationMs`
- `timeoutMs`
- `maxEventsPerTopic`
- `outputTransformers[]`

Per-input controls (`inputs[]`):
- `topicName`
- `selector`
- `contributionMode` (`FIRST` or `LAST`)
- `transformer[]`

## Design Patterns

1. Multi-sensor window join
- Use one input per topic.
- Set `contributionMode: LAST` for latest-value semantics.
- Set `windowDurationMs` to expected telemetry cadence bucket.

2. Strict first-event capture
- Use `contributionMode: FIRST` for race-free first-observation behavior.
- Keep `maxEventsPerTopic` low to bound memory.

3. Partial-window tolerance
- Use `timeoutMs` to close windows when not all inputs arrive.
- Document how missing inputs should be interpreted downstream.

4. Output shaping
- Use `outputTransformers` when aggregate envelope needs format adjustments.
- Verify destination contentType/schema expectations separately.

## Validation Heuristics

- `windowDurationMs` and `timeoutMs` must be positive.
- Default safety pattern: `timeoutMs >= windowDurationMs`.
- `maxEventsPerTopic` should scale with burst profile but remain bounded.
- High `stripeCount` without sufficient load may add scheduling overhead.

## Verification Patterns

1. Startup and load checks
```bash
docker logs <container> 2>&1 | rg -n "AGGREGATOR_MANAGER_TASK_CREATED|AGGREGATOR_STARTED|AGGREGATOR_EXCEPTION|AGGREGATOR_EVENT_DROPPED"
```

2. Input publication checks
- Publish to each input topic with same correlation ID and close timestamps.

3. Output assertions
- Confirm output topic receives aggregate payload.
- When stats are required, assert expected fields exist (for example `timeStart`, `timeEnd`, `duration`, `sampleCount`, and per-field aggregates) based on configured pipeline behavior.

## Failure Classes

- Window mismatch: events outside bucket not co-aggregated.
- Timeout too aggressive: windows close before full input set arrives.
- Contribution mismatch: FIRST vs LAST produces wrong representative values.
- Backpressure/drop: `maxEventsPerTopic` too small for ingress bursts.
