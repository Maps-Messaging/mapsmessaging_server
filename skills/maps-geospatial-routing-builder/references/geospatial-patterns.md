# Geospatial Patterns

## Pattern A: Geohash Bucket Routing

- Normalize incoming GPS coordinates.
- Compute geohash at configured precision.
- Route into hierarchy paths using geohash segments.

## Pattern B: Distance Threshold Routing

- Compute distance between message coordinate and reference point.
- Route to `near` or `far` destinations based on threshold.

## Pattern C: Hybrid Geohash + Distance

- First route by geohash region.
- Then apply distance threshold within region.

## Input Quality Handling

- Handle invalid lat/lon range.
- Handle missing coordinates.
- Route invalid data to explicit quarantine path.

## Verification Vectors

- Include known coordinate pairs with expected distance order.
- Include expected geohash values for chosen precision.

## Runnable Smoke Commands

Skill smoke gate:

```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_skill_smoke.sh
```

Deterministic vector checks:

```bash
python3 /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_vectors.py
```

Runtime geospatial route smoke:

```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/scripts/run_geospatial_mqtt_smoke.sh --source-topic /geo/in --near-topic /geo/out/near --far-topic /geo/out/far --invalid-topic /geo/out/invalid
```
