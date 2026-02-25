#!/usr/bin/env bash
set -euo pipefail

ROOT="/Users/krital/dev/starsense/mapsmessaging_server"
IMAGE="${IMAGE:-}"
PLATFORM="${PLATFORM:-}"
IMAGE_CHANNEL="${IMAGE_CHANNEL:-auto}"
IMAGE_VERSION="${IMAGE_VERSION:-4.3.1-snapshot}"
ARCH="${ARCH:-auto}"
CONTAINER_NAME="${CONTAINER_NAME:-maps-agg-scenario-e2e}"
MQTT_PORT="${MQTT_PORT:-49883}"
HTTP_PORT="${HTTP_PORT:-56080}"
INPUT_A="${INPUT_A:-/veh/a}"
INPUT_B="${INPUT_B:-/veh/b}"
OUTPUT_TOPIC="${OUTPUT_TOPIC:-/veh/agg}"
TOPIC_NAMESPACE="${TOPIC_NAMESPACE:-/local/maps-aggregator-scenario-e2e}"
TEST_RUN_ID="${TEST_RUN_ID:-$(date +%s)-$$}"
SMOKE_TOPIC="${SMOKE_TOPIC:-${TOPIC_NAMESPACE}/${TEST_RUN_ID}/smoke}"
WAIT_SECS="${WAIT_SECS:-18}"

detect_arch() {
  local machine
  machine="$(uname -m || true)"
  case "${machine}" in
    arm64|aarch64) echo "arm64" ;;
    x86_64|amd64) echo "amd64" ;;
    *) echo "amd64" ;;
  esac
}

resolve_image() {
  local selected_arch="$1"
  local selected_channel="$2"
  if [[ "${selected_channel}" == "snapshot" ]]; then
    if [[ "${selected_arch}" == "arm64" ]]; then
      echo "mapsmessaging/server_daemon_arm_${IMAGE_VERSION}:latest"
    else
      echo "mapsmessaging/server_daemon_${IMAGE_VERSION}:latest"
    fi
    return
  fi
  echo "mapsmessaging/server_daemon:latest"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

search_cmd() {
  if command -v rg >/dev/null 2>&1; then
    rg -n "$1" "$2"
  else
    grep -nE "$1" "$2"
  fi
}

port_in_use() {
  local p="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"${p}" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  if command -v ss >/dev/null 2>&1; then
    ss -lnt | awk '{print $4}' | grep -E "[:.]${p}$" >/dev/null 2>&1
    return $?
  fi
  if command -v netstat >/dev/null 2>&1; then
    netstat -lnt 2>/dev/null | awk '{print $4}' | grep -E "[:.]${p}$" >/dev/null 2>&1
    return $?
  fi
  return 1
}

require_cmd docker
require_cmd mosquitto_pub
require_cmd mosquitto_sub
require_cmd python3

if [[ "${ARCH}" == "auto" ]]; then
  ARCH="$(detect_arch)"
fi

if [[ -z "${IMAGE}" ]]; then
  EFFECTIVE_CHANNEL="${IMAGE_CHANNEL}"
  if [[ "${EFFECTIVE_CHANNEL}" == "auto" ]]; then
    if [[ "${ARCH}" == "arm64" ]]; then
      EFFECTIVE_CHANNEL="snapshot"
    else
      EFFECTIVE_CHANNEL="release"
    fi
  fi
  IMAGE="$(resolve_image "${ARCH}" "${EFFECTIVE_CHANNEL}")"
fi

if [[ -z "${PLATFORM}" ]]; then
  PLATFORM="linux/${ARCH}"
fi

TMP_DIR="$(mktemp -d /tmp/maps-agg-scenario-e2e.XXXXXX)"
cleanup() {
  docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

for f in AuthManager.yaml DestinationManager.yaml DeviceManager.yaml DiscoveryManager.yaml License.yaml LoRaDevice.yaml MessageDaemon.yaml NetworkConnectionManager.yaml RestApi.yaml SchemaManager.yaml SecurityManager.yaml TenantManagement.yaml jolokia.yaml routing.yaml; do
  if [[ -f "${ROOT}/${f}" ]]; then
    cp "${ROOT}/${f}" "${TMP_DIR}/${f}"
  fi
done

cat > "${TMP_DIR}/AggregatorManager.yaml" <<YAML
AggregatorManager:
  data:
    schemaLoadingVersion: 0
    maxAggregators: 0
    maxBatchPerAggregator: 128
    mailboxCapacity: 8192
    idleSleepMs: 1
    stripeCount: 0
    aggregatorConfigList:
      - enabled: true
        name: veh-agg-simple
        inputs:
          - topicName: ${INPUT_A}
            selector: ""
            contributionMode: LAST
            transformer: []
          - topicName: ${INPUT_B}
            selector: ""
            contributionMode: LAST
            transformer: []
        outputTopic: ${OUTPUT_TOPIC}
        windowDurationMs: 5000
        timeoutMs: 7000
        maxEventsPerTopic: 100
        outputTransformers: []
YAML

cat > "${TMP_DIR}/NetworkManager.yaml" <<'YAML'
---
NetworkManager:
  data:
    - name: "Runtime MQTT 1883"
      url: tcp://0.0.0.0:1883/
      protocol: mqtt
YAML

if python3 -c 'import yaml' >/dev/null 2>&1; then
  python3 "${ROOT}/skills/smoke/ensure_mqtt_listener.py" "${TMP_DIR}/NetworkManager.yaml"
else
  # Fallback for local environments without PyYAML; keep canonical MQTT listener present.
  if ! grep -Eq 'url:[[:space:]]*tcp://0\.0\.0\.0:1883/?' "${TMP_DIR}/NetworkManager.yaml"; then
    cat >> "${TMP_DIR}/NetworkManager.yaml" <<'YAML'
    - name: "Runtime MQTT 1883 (fallback)"
      url: tcp://0.0.0.0:1883/
      protocol: mqtt
YAML
  fi
fi

if [[ -f "${TMP_DIR}/DiscoveryManager.yaml" ]]; then
  sed -i.bak -E 's/hostnames:[[:space:]]*::/hostnames: "::"/g' "${TMP_DIR}/DiscoveryManager.yaml"
  rm -f "${TMP_DIR}/DiscoveryManager.yaml.bak"
fi
if [[ -f "${TMP_DIR}/routing.yaml" ]]; then
  sed -i.bak -E 's/autoDiscovery:[[:space:]]*true/autoDiscovery: false/g' "${TMP_DIR}/routing.yaml"
  rm -f "${TMP_DIR}/routing.yaml.bak"
fi

if port_in_use "${MQTT_PORT}"; then
  echo "MQTT host port ${MQTT_PORT} is already in use" >&2
  exit 1
fi
if port_in_use "${HTTP_PORT}"; then
  echo "HTTP host port ${HTTP_PORT} is already in use" >&2
  exit 1
fi

RUN_ARGS=(
  run -d
  --name "${CONTAINER_NAME}"
  -p "${MQTT_PORT}:1883/tcp"
  -p "${HTTP_PORT}:8080/tcp"
)
if [[ -n "${PLATFORM}" ]]; then
  RUN_ARGS+=( --platform "${PLATFORM}" )
fi
for cfg in AggregatorManager.yaml AuthManager.yaml DestinationManager.yaml DeviceManager.yaml DiscoveryManager.yaml License.yaml LoRaDevice.yaml MessageDaemon.yaml NetworkConnectionManager.yaml NetworkManager.yaml RestApi.yaml SchemaManager.yaml SecurityManager.yaml TenantManagement.yaml jolokia.yaml routing.yaml; do
  if [[ -f "${TMP_DIR}/${cfg}" ]]; then
    RUN_ARGS+=( -v "${TMP_DIR}/${cfg}:/message_daemon-3.3.0/conf/${cfg}:ro" )
  fi
done
RUN_ARGS+=("${IMAGE}")

docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true

echo "Starting ${CONTAINER_NAME} (${IMAGE})"
docker "${RUN_ARGS[@]}" >/dev/null

sleep 4
if [[ "$(docker inspect -f '{{.State.Running}}' "${CONTAINER_NAME}")" != "true" ]]; then
  echo "Container exited during startup" >&2
  docker logs "${CONTAINER_NAME}" 2>&1 | sed -n '1,220p' >&2
  exit 1
fi

LOG_ERR="/tmp/${CONTAINER_NAME}-errors.log"
if docker logs "${CONTAINER_NAME}" 2>&1 | grep -nE "Address already in use|BindException|Failed to bind|Cannot assign requested address" >"${LOG_ERR}"; then
  echo "Startup blockers detected" >&2
  sed -n '1,120p' "${LOG_ERR}" >&2
  exit 1
fi
if docker logs "${CONTAINER_NAME}" 2>&1 | grep -nE "Protocol not available" >/tmp/${CONTAINER_NAME}-protocol.warn; then
  echo "Non-blocking warning: some optional protocols are unavailable in this image." >&2
  sed -n '1,80p' /tmp/${CONTAINER_NAME}-protocol.warn >&2 || true
fi

LISTENERS="/tmp/${CONTAINER_NAME}-listeners.log"
attempt=0
while true; do
  docker exec "${CONTAINER_NAME}" sh -lc '(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | sed -n "1,120p"' >"${LISTENERS}" || true
  if search_cmd ':1883' "${LISTENERS}" >/dev/null 2>&1 && search_cmd ':8080' "${LISTENERS}" >/dev/null 2>&1; then
    break
  fi
  attempt=$((attempt + 1))
  if [[ "${attempt}" -ge 30 ]]; then
    echo "Listener verification failed: expected 1883/8080 inside container" >&2
    echo "Listener snapshot:" >&2
    sed -n '1,120p' "${LISTENERS}" >&2 || true
    echo "Recent server logs:" >&2
    docker logs "${CONTAINER_NAME}" 2>&1 | tail -n 120 >&2 || true
    exit 1
  fi
  sleep 1
done

# Wait for aggregator workers to become active before scenario publish.
agg_ready=0
for _ in $(seq 1 40); do
  if docker logs "${CONTAINER_NAME}" 2>&1 | grep -Eq "AGGREGATOR_MANAGER_TASK_CREATED|AGGREGATOR_STARTED"; then
    agg_ready=1
    break
  fi
  sleep 1
done
if [[ "${agg_ready}" -ne 1 ]]; then
  echo "Warning: aggregator startup marker not observed; continuing with scenario publish." >&2
fi

# Smoke topic check to ensure MQTT path is alive.
OUT_SMOKE="/tmp/${CONTAINER_NAME}-smoke.out"
: >"${OUT_SMOKE}"

timeout 12 mosquitto_sub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${SMOKE_TOPIC}" -C 1 >"${OUT_SMOKE}" 2>/tmp/${CONTAINER_NAME}-smoke.err &
SPID=$!
sleep 1
mosquitto_pub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${SMOKE_TOPIC}" -m "smoke-${TEST_RUN_ID}"
wait "${SPID}"

# Aggregator scenario: subscribe before publishing both inputs.
OUT_AGG="/tmp/${CONTAINER_NAME}-agg.out"
ERR_AGG="/tmp/${CONTAINER_NAME}-agg.err"
: >"${OUT_AGG}"; : >"${ERR_AGG}"

scenario_ok=0
for attempt in 1 2 3; do
  cid="${TEST_RUN_ID}-a${attempt}"
  : >"${OUT_AGG}"; : >"${ERR_AGG}"
  timeout "${WAIT_SECS}" mosquitto_sub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${OUTPUT_TOPIC}" -C 1 >"${OUT_AGG}" 2>"${ERR_AGG}" &
  APID=$!
  sleep 1
  mosquitto_pub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${INPUT_A}" -m "{\"src\":\"a\",\"v\":11,\"cid\":\"${cid}\"}"
  mosquitto_pub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${INPUT_B}" -m "{\"src\":\"b\",\"v\":22,\"cid\":\"${cid}\"}"
  if wait "${APID}"; then
    scenario_ok=1
    break
  fi
  sleep 1
done

if [[ "${scenario_ok}" -ne 1 ]]; then
  echo "Aggregator scenario failed: no message on ${OUTPUT_TOPIC} within ${WAIT_SECS}s (3 attempts)" >&2
  echo "Subscriber stderr:" >&2
  sed -n '1,120p' "${ERR_AGG}" >&2 || true
  echo "Aggregator log markers:" >&2
  docker logs "${CONTAINER_NAME}" 2>&1 | grep -nE "AGGREGATOR_MANAGER_TASK_CREATED|AGGREGATOR_STARTED|AGGREGATOR_EXCEPTION|AGGREGATOR_EVENT_DROPPED|/veh/a|/veh/b|/veh/agg" | tail -n 120 >&2 || true
  echo "Recent server logs:" >&2
  docker logs "${CONTAINER_NAME}" 2>&1 | tail -n 120 >&2 || true
  exit 1
fi

if [[ ! -s "${OUT_AGG}" ]]; then
  echo "Aggregator scenario failed: output payload is empty on ${OUTPUT_TOPIC}" >&2
  exit 1
fi

echo "--- ${OUTPUT_TOPIC} sample ---"
sed -n '1,6p' "${OUT_AGG}"
echo "maps-aggregator-config-engineer scenario e2e PASS"
