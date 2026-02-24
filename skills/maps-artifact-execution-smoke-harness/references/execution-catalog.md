# Execution Catalog

## Phase 1: Setup

- Validate target files exist and paths are absolute.
- Confirm expected ports are not occupied before startup.
- For Docker mode, remove stale test containers by explicit name.

Example checks:

```bash
rg -n "NetworkManager|DestinationManager|routing|SchemaManager" /Users/krital/dev/starsense/mapsmessaging_server/*.yaml
(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | rg "1883|5672|8080|9001" || true
```

## Phase 2: Apply

- Apply generated files exactly as delivered.
- For Docker, mount generated files under `/opt/maps/conf`.

## Phase 3: Startup

- Start process/container and capture logs.
- Detect blockers: license, missing provider, Consul mismatch, malformed YAML, bind failure.

## Phase 4: Listener Verification

- Validate all required protocol listeners are bound.
- For Docker mode, verify both host publish and in-container bind state.

## Phase 5: Traffic Verification

- Run ingress/egress checks with correlation IDs.
- Always subscribe before publish for MQTT checks; wait for subscriber readiness before sending test payload.
- For bridging paths, verify destination-side evidence only marks PASS.

## Phase 6: Teardown

- Stop and remove test process/container.
- Keep log snippets and command outputs for evidence.

## Failure Classes

- `apply_failure`
- `startup_gate_failure`
- `listener_bind_failure`
- `traffic_path_failure`
- `environment_failure`

## Runnable Scripts

- Single scenario runner:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/scripts/run_artifact_smoke.sh`
- Two-scenario matrix runner:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/scripts/run_matrix.sh`

Example:

```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/scripts/run_matrix.sh --channel release --artifact-dir /Users/krital/dev/starsense/mapsmessaging_server --base-mqtt-port 2883 --base-http-port 28080
```

Snapshot example (latest requested snapshot version):

```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/scripts/run_matrix.sh --channel snapshot --version 4.3.1-snapshot --arch arm64 --platform linux/arm64 --artifact-dir /Users/krital/dev/starsense/mapsmessaging_server --base-mqtt-port 3883 --base-http-port 38080
```

x86 release example:

```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/scripts/run_matrix.sh --channel release --arch amd64 --platform linux/amd64 --artifact-dir /Users/krital/dev/starsense/mapsmessaging_server --base-mqtt-port 2883 --base-http-port 28080
```

Auth-enabled MQTT example:

```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/scripts/run_matrix.sh --channel snapshot --version 4.3.1-snapshot --arch arm64 --platform linux/arm64 --artifact-dir /Users/krital/dev/starsense/mapsmessaging_server --mqtt-username maps-user --mqtt-password maps-pass --base-mqtt-port 4883 --base-http-port 48080
```

Listener-only example for artifacts with non-MQTT flows:

```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/scripts/run_artifact_smoke.sh --image mapsmessaging/server_daemon:latest --artifact-dir /Users/krital/dev/starsense/mapsmessaging_server --skip-mqtt --required-listeners 8080 --platform linux/amd64 --mqtt-port 5883 --http-port 58080 --force-clean
```
