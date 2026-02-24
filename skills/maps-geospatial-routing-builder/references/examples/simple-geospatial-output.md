# Simple Geospatial Output Example

## Geospatial Requirement Mapping
- Route incoming GPS messages by geohash prefix and proximity threshold.
- Route invalid coordinates to quarantine path.
- File targets:
  - `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml`
  - `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml`
  - `/Users/krital/dev/starsense/mapsmessaging_server/routing.yaml`

## Geo Model
- Coordinate system: WGS84 latitude/longitude.
- Units: kilometers.
- Geohash precision: 5.
- Distance threshold: 20 km from reference point.

## Assumptions
- Payload fields: `lat`, `lon`.
- Invalid coordinate ranges route to `/geo/out/invalid`.

## Deployable Config Entity
```yaml
routes:
  - source: /geo/in
    selector: "isValidGps(lat, lon) && distanceKm(lat, lon, 51.5074, -0.1278) <= 20"
    destination: /geo/out/near
  - source: /geo/in
    selector: "isValidGps(lat, lon) && distanceKm(lat, lon, 51.5074, -0.1278) > 20"
    destination: /geo/out/far
  - source: /geo/in
    selector: "!isValidGps(lat, lon)"
    destination: /geo/out/invalid
```

## Apply Steps
```bash
cp /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml.bak
# apply geospatial route patch and reload runtime
```

## Verification
- Distance vector checks (known-distance sanity):
  - London->Paris near known value.
- Geohash checks:
  - known geohash prefix for London/NYC at precision 5.
- Invalid GPS checks:
  - out-of-range latitude and longitude values route to invalid path.
```bash
python3 /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_vectors.py
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_mqtt_smoke.sh --source-topic /geo/in --near-topic /geo/out/near --far-topic /geo/out/far --invalid-topic /geo/out/invalid
```

## Risk Notes
- Precision tradeoff: geohash-5 may be coarse near boundary edges.
- GPS jitter may cause near/far oscillation close to threshold.

## Scenario Metrics and Dashboard
- Metrics:
  - geohash bucket distribution
  - distance evaluation count
  - near/far split rate
  - invalid-coordinate count
  - geospatial route latency
- Grafana: geohash heat bucket + near/far ratio panel.
- MAPS-hosted dashboard: invalid-GPS trend and threshold crossing counters.

## C4 Architecture Diagram
```mermaid
graph LR
  A[GPS Publisher] --> B[MAPS geospatial route]
  B --> C[/geo/out/near]
  B --> D[/geo/out/far]
  B --> E[/geo/out/invalid]
```
