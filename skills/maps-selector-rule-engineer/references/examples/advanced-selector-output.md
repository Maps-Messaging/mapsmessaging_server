# Advanced Selector Output Example

## Selector Requirement Mapping
- Stage 1 route by `vehicleType` to intermediate topics.
- Stage 2 subscriber selectors split high-priority alerts and normal events.
- File targets:
  - `NetworkManager.yaml`
  - `DestinationManager.yaml`
  - `routing.yaml`

## Selector Evaluation Model
- Routing selector order:
  1. `vehicleType == "emergency"` -> `/selector/intermediate/emergency`
  2. `vehicleType == "public"` -> `/selector/intermediate/public`
  3. fallback -> `/selector/intermediate/other`
- Subscriber selector order on each intermediate topic:
  1. `priority >= 8` -> `/selector/final/high`
  2. `priority < 8` -> `/selector/final/normal`
- Conflict policy: strict stage order, first-match per stage.

## Assumptions
- Payload fields: `vehicleType`, `priority`.
- Unknown `vehicleType` uses fallback path.

## Deployable Config Entity
```yaml
# example staged selector routes
routes:
  - source: /selector/in
    selector: "vehicleType == 'emergency'"
    destination: /selector/intermediate/emergency
  - source: /selector/in
    selector: "vehicleType == 'public'"
    destination: /selector/intermediate/public
  - source: /selector/in
    selector: "true"
    destination: /selector/intermediate/other
```

## Apply Steps
```bash
cp routing.yaml routing.yaml.bak
# apply staged selector patch and restart runtime
```

## Verification
- Positive vectors:
  - `{ "vehicleType":"emergency", "priority":9 }` -> `/selector/final/high`
  - `{ "vehicleType":"public", "priority":6 }` -> `/selector/final/normal`
- Negative vectors:
  - `{ "vehicleType":"other", "priority":9 }` should not appear in emergency-only subscriber stream.
  - `{ "vehicleType":"public", "priority":2 }` should not appear in high-priority stream.
```bash
bash skills/maps-selector-rule-engineer/scripts/run_selector_mqtt_smoke.sh --source-topic /selector/in --match-topic /selector/final/high --nonmatch-topic /selector/final/normal
```

## Performance and Risk Notes
- Two-stage selection increases evaluation count.
- Risk: overlapping selector terms can cause unintended fan-out if precedence is altered.
- Mitigation: keep explicit stage boundaries and fallback routes.

## Scenario Metrics and Dashboard
- Metrics:
  - per-stage selector hit/miss
  - stage-1 to stage-2 transfer latency
  - dropped-on-filter count
  - selector conflict count
- Grafana: stage heatmap + selector latency percentiles.
- MAPS-hosted dashboard: per-stage throughput and conflict alerts.

## C4 Architecture Diagram
```mermaid
graph LR
  A[Publisher] --> B[Stage1 routing selectors]
  B --> C[/selector/intermediate/emergency]
  B --> D[/selector/intermediate/public]
  B --> E[/selector/intermediate/other]
  C --> F[Stage2 subscriber selectors]
  D --> F
  E --> F
  F --> G[/selector/final/high]
  F --> H[/selector/final/normal]
```
