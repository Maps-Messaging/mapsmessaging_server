# Advanced Geospatial Output Example

## Geospatial Requirement Mapping
- Stage 1 geohash region bucketing.
- Stage 2 per-region distance thresholds for routing.
- Invalid coordinate quarantine and fallback delivery path.
- File targets:
  - `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml`
  - `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml`
  - `/Users/krital/dev/starsense/mapsmessaging_server/routing.yaml`

## Geo Model
- Coordinate system: WGS84.
- Units: kilometers.
- Geohash precision: 6 (region split) with fallback to precision 4 for sparse regions.
- Thresholds:
  - region A: 10 km
  - region B: 25 km

## Assumptions
- Payload fields: `lat`, `lon`, `regionHint`.
- Unknown regionHint defaults to global threshold profile.

## Deployable Config Entity
```yaml
routes:
  - source: /geo/in
    selector: "isValidGps(lat, lon)"
    destination: /geo/intermediate/${geohash(lat, lon, 6)}
  - source: /geo/in
    selector: "!isValidGps(lat, lon)"
    destination: /geo/out/invalid
# stage-2 selectors apply per intermediate region with distance thresholds
```

## Apply Steps
```bash
cp /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml.bak
# apply staged geospatial routing patch and restart runtime
```

## Verification
- Distance checks:
  - known-distance vectors and threshold transition assertions.
- Geohash checks:
  - precision 6 and fallback precision 4 behavior.
- Invalid checks:
  - missing lat/lon and out-of-range values go to quarantine.
```bash
python3 /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_vectors.py
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_mqtt_smoke.sh --source-topic /geo/in --near-topic /geo/out/near --far-topic /geo/out/far --invalid-topic /geo/out/invalid
```

## Risk Notes
- Higher precision geohash increases route cardinality.
- Region-specific thresholds add operational complexity.
- GPS quality variance may increase fallback usage.

## Scenario Metrics and Dashboard
- Metrics:
  - per-region geohash cardinality
  - distance-threshold crossing count
  - invalid and missing GPS rate
  - fallback profile usage
  - per-stage routing latency
- Grafana: region map + threshold crossing alert panels.
- MAPS-hosted dashboard: per-zone throughput and invalid-route stream.

## C4 Architecture Diagram
```mermaid
graph LR
  A[GPS Publisher] --> B[Stage1 geohash bucket]
  B --> C[/geo/intermediate/<geohash>]
  C --> D[Stage2 distance selectors]
  D --> E[/geo/out/near]
  D --> F[/geo/out/far]
  B --> G[/geo/out/invalid]
```
