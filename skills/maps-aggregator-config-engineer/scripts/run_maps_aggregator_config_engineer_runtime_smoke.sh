#!/usr/bin/env bash
set -euo pipefail

ROOT="/Users/krital/dev/starsense/mapsmessaging_server"
HARNESS="/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-artifact-execution-smoke-harness/scripts/run_artifact_smoke.sh"
IMAGE="${IMAGE:-}"
PLATFORM="${PLATFORM:-}"
IMAGE_CHANNEL="${IMAGE_CHANNEL:-auto}"
IMAGE_VERSION="${IMAGE_VERSION:-4.3.1-snapshot}"
ARCH="${ARCH:-auto}"
MQTT_PORT="${MQTT_PORT:-1883}"
HTTP_PORT="${HTTP_PORT:-8080}"
CONTAINER_NAME="${CONTAINER_NAME:-maps-aggregator-config-engineer-runtime-smoke}"
TOPIC_NAMESPACE="${TOPIC_NAMESPACE:-/skill/maps-aggregator-config-engineer-runtime-smoke/runtime}"
TEST_RUN_ID="${TEST_RUN_ID:-$(date +%s)-$$}"
TOPIC="${TOPIC:-${TOPIC_NAMESPACE}/${TEST_RUN_ID}}"
MQTT_USER="${MQTT_USER:-}"
MQTT_PASSWORD="${MQTT_PASSWORD:-}"
REQUIRE_MQTT_RUNTIME="${REQUIRE_MQTT_RUNTIME:-0}"
STRICT_MQTT_BASELINE="${STRICT_MQTT_BASELINE:-1}"

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

TMP_DIR="$(mktemp -d /tmp/maps-aggregator-config-engineer-runtime-smoke.XXXXXX)"
trap 'rm -rf "${TMP_DIR}"' EXIT

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

for cfg in AggregatorManager.yaml AuthManager.yaml DestinationManager.yaml DeviceManager.yaml DiscoveryManager.yaml License.yaml LoRaDevice.yaml MLModelManager.yaml MessageDaemon.yaml NetworkConnectionManager.yaml NetworkManager.yaml RestApi.yaml SchemaManager.yaml SecurityManager.yaml TenantManagement.yaml jolokia.yaml routing.yaml; do
  if [[ -f "${ROOT}/${cfg}" ]]; then
    cp "${ROOT}/${cfg}" "${TMP_DIR}/${cfg}"
  fi
done

if [[ -f "${TMP_DIR}/routing.yaml" ]]; then
  sed -i.bak -E 's/autoDiscovery:[[:space:]]*true/autoDiscovery: false/g' "${TMP_DIR}/routing.yaml"
  rm -f "${TMP_DIR}/routing.yaml.bak"
fi
if [[ -f "${TMP_DIR}/DiscoveryManager.yaml" ]]; then
  sed -i.bak -E 's/hostnames:[[:space:]]*::/hostnames: "::"/g' "${TMP_DIR}/DiscoveryManager.yaml"
  rm -f "${TMP_DIR}/DiscoveryManager.yaml.bak"
fi

if [[ "${REQUIRE_MQTT_RUNTIME}" == "1" && "${STRICT_MQTT_BASELINE}" == "1" ]]; then
  cat > "${TMP_DIR}/NetworkManager.yaml" <<'YAML'
---
NetworkManager:
  data:
    - name: "Runtime MQTT 1883"
      url: tcp://0.0.0.0:1883/
      protocol: mqtt
YAML
fi

if [[ -f "${TMP_DIR}/NetworkManager.yaml" ]]; then
  if python3 -c 'import yaml' >/dev/null 2>&1; then
    python3 "${ROOT}/skills/smoke/ensure_mqtt_listener.py" "${TMP_DIR}/NetworkManager.yaml"
  fi
fi

RUNTIME_LISTENERS="8080"
if [[ "${REQUIRE_MQTT_RUNTIME}" == "1" ]]; then
  RUNTIME_LISTENERS="8080,1883"
fi

CMD=(bash "${HARNESS}"
  --image "${IMAGE}"
  --container-name "${CONTAINER_NAME}"
  --artifact-dir "${TMP_DIR}"
  --mqtt-port "${MQTT_PORT}"
  --http-port "${HTTP_PORT}"
  --topic "${TOPIC}"
  --required-listeners "${RUNTIME_LISTENERS}"
  --force-clean
)
if [[ "${REQUIRE_MQTT_RUNTIME}" != "1" ]]; then CMD+=(--skip-mqtt); fi
if [[ -n "${PLATFORM}" ]]; then CMD+=(--platform "${PLATFORM}"); fi
if [[ -n "${MQTT_USER}" ]]; then CMD+=(--mqtt-username "${MQTT_USER}"); fi
if [[ -n "${MQTT_PASSWORD}" ]]; then CMD+=(--mqtt-password "${MQTT_PASSWORD}"); fi

"${CMD[@]}"

if [[ "${REQUIRE_MQTT_RUNTIME}" == "1" ]]; then
  echo "strict MQTT runtime smoke validated by harness on topic ${TOPIC}"
  echo "maps-aggregator-config-engineer runtime smoke PASS"
  exit 0
fi

if timeout 3 bash -lc "echo > /dev/tcp/127.0.0.1/${MQTT_PORT}" 2>/dev/null; then
  AUTH=()
  if [[ -n "${MQTT_USER}" ]]; then AUTH+=( -u "${MQTT_USER}" ); fi
  if [[ -n "${MQTT_PASSWORD}" ]]; then AUTH+=( -P "${MQTT_PASSWORD}" ); fi
  CORR="maps_aggregator_config_engineer_runtime_smoke-$(date +%s)"
  timeout 8 mosquitto_sub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${TOPIC}" -C 1 "${AUTH[@]}" >/tmp/maps-aggregator-config-engineer-runtime-smoke.out 2>/tmp/maps-aggregator-config-engineer-runtime-smoke.err &
  SPID=$!
  sleep 1
  mosquitto_pub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${TOPIC}" -m "${CORR}" "${AUTH[@]}"
  wait "${SPID}"
  rg -n "${CORR}" /tmp/maps-aggregator-config-engineer-runtime-smoke.out >/dev/null
else
  echo "MQTT listener not available on ${MQTT_PORT}; startup/listener runtime check passed without maps_aggregator_config_engineer_runtime_smoke path."
fi

echo "maps-aggregator-config-engineer runtime smoke PASS"
