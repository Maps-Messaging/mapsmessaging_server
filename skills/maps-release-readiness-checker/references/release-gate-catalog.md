# Release Gate Catalog

## Gate 1: Build and Test

Core checks:
```bash
cd <repo-root>
./build.sh
```

If scoped checks are needed, include targeted test commands and capture pass/fail.

## Gate 2: Docker Image Availability and Smoke

Image availability checks:
```bash
docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}'
```

Container smoke checks:
```bash
docker run -d --name maps-release-smoke -p 1883:1883/tcp -p 8080:8080/tcp <candidate-image>
docker logs maps-release-smoke 2>&1 | rg -n "Startup aborted|Protocol not available|ERROR"
docker exec maps-release-smoke sh -lc '(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | rg "1883|8080|5672|9001"'
docker rm -f maps-release-smoke
```

## Gate 3: Startup Integrity

Check for startup blockers:
```bash
docker logs <container> 2>&1 | rg -n "Startup aborted|Protocol not available|Consul|license|ERROR"
```

Check listener state:
```bash
docker exec <container> sh -lc '(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | rg "1883|5672|8080|9001"'
```

## Gate 4: Protocol Smoke Coverage

At minimum, run protocol checks required by release scope.
Example MQTT smoke:
```bash
mosquitto_pub -h localhost -p 1883 -t /release/smoke -m ok -d
```

Require destination-side confirmation where applicable.

## Gate 5: Feature-Specific Checks

Use release scope to include targeted checks:
- Aggregator: aggregator startup and output evidence
- Satellite: provider startup, publish boundary checks
- CAN/N2K: endpoint bind and mapped topic evidence
- Schema/Transform: contentType/schemaId alignment and transformed output

## Gate 6: Packaging and Rollback

Confirm deploy artifact completeness and rollback instructions.
For dockerized packaging, include image tag, run command, mounts, and revert command.

## Decision Criteria

- PASS: all blocking gates pass with evidence.
- CONDITIONAL PASS: only non-blocking issues remain with mitigation and owner.
- FAIL: any blocking gate fails or evidence is missing.
