# Startup Gates and Bind Checks

## Primary Triage Sequence

1. Confirm process/container is actually running.
```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

2. Capture startup errors.
```bash
docker logs <container> 2>&1 | rg -n "ERROR|Exception|Startup aborted|Consul|license|Protocol not available|Address already in use|BindException"
```

3. Verify in-container listeners.
```bash
docker exec <container> sh -lc '(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | rg "1883|5672|8080|9001"'
```

4. Verify host published ports.
```bash
docker ps --format 'table {{.Names}}\t{{.Ports}}' | rg '<container>'
```

5. Run smoke publish/subscribe only if listener is bound.
```bash
mosquitto_pub -h localhost -p 1883 -t /diag/smoke -m ok -d
mosquitto_sub -h localhost -p 1883 -t /diag/smoke -C 1 -W 5 -d
```

## Evidence Patterns

- `Startup aborted ... Consul`: runtime gate failure before full listener init.
- `Protocol not available`: provider missing from build/image; config may reference unsupported protocol.
- `Address already in use` or `BindException`: confirmed port collision.
- `CONNACK (0)` from `mosquitto_pub`: MQTT ingress path is alive.
- Listener only on `8080` but not `1883`: REST came up while messaging listeners failed.

## Known Pitfall Classes

1. Image/runtime mismatch
- Symptom: protocol provider errors after startup.
- Check: image tag compatibility with configuration files.

2. Config gate failures
- Symptom: startup abort before listener bind.
- Check: Consul/file-config expectations, license validity windows, invalid manager YAML.

3. Port publishing confusion
- Symptom: listener bound in container but unreachable from host.
- Check: `docker run -p` mapping and host firewall.

4. False port-collision diagnosis
- Symptom: suspected collision but no bind errors.
- Check: prove with log evidence and socket table before concluding collision.
