# Simple Selector Output Example

## Selector Requirement Mapping
- Route `priority >= 5` messages to `/selector/match` and others to `/selector/nonmatch`.
- File targets:
  - `NetworkManager.yaml`
  - `DestinationManager.yaml`
  - `routing.yaml`

## Selector Evaluation Model
- Routing selector order:
  1. `priority >= 5` -> `/selector/match`
  2. fallback -> `/selector/nonmatch`
- Subscriber selector order:
  1. subscriber A: `priority >= 5`
  2. subscriber B: `priority < 5`
- Conflict policy: first-match then fallback.

## Assumptions
- JSON payload has numeric `priority`.
- Missing `priority` treated as non-match.

## Deployable Config Entity
```yaml
# example patch fragment
routes:
  - source: /selector/in
    selector: "priority >= 5"
    destination: /selector/match
  - source: /selector/in
    selector: "true"
    destination: /selector/nonmatch
```

## Apply Steps
```bash
cp routing.yaml routing.yaml.bak
# apply selector route patch
```

## Verification
- Positive vector:
  - payload: `{ "priority": 9 }` should route to `/selector/match`.
- Negative vector:
  - payload: `{ "priority": 1 }` should route to `/selector/nonmatch`.
```bash
bash skills/maps-selector-rule-engineer/scripts/run_selector_mqtt_smoke.sh --source-topic /selector/in --match-topic /selector/match --nonmatch-topic /selector/nonmatch
```

## Performance and Risk Notes
- Simple expression has low selector latency.
- Risk: malformed payloads without `priority` route to fallback.

## Scenario Metrics and Dashboard
- Metrics:
  - selector match rate
  - selector miss rate
  - route divergence count
  - selector evaluation latency
- Grafana panel: selector hit/miss time series.
- MAPS-hosted dashboard: per-selector counters and last-evaluation errors.

## C4 Architecture Diagram
```mermaid
graph LR
  A[Publisher] --> B[MAPS selector route]
  B --> C[/selector/match]
  B --> D[/selector/nonmatch]
```
