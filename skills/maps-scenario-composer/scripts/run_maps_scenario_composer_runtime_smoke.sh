#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
HARNESS="${ROOT}/skills/maps-artifact-execution-smoke-harness/scripts/run_artifact_smoke.sh"
IMAGE="${IMAGE:-}"
PLATFORM="${PLATFORM:-}"
MQTT_PORT="${MQTT_PORT:-1883}"
HTTP_PORT="${HTTP_PORT:-8080}"
CONTAINER_NAME="${CONTAINER_NAME:-maps-scenario-composer-runtime-smoke}"
TOPIC_NAMESPACE="${TOPIC_NAMESPACE:-/skill/maps-scenario-composer-runtime-smoke/runtime}"
TEST_RUN_ID="${TEST_RUN_ID:-$(date +%s)-$$}"
TOPIC="${TOPIC:-${TOPIC_NAMESPACE}/${TEST_RUN_ID}}"
MQTT_USER="${MQTT_USER:-}"
MQTT_PASSWORD="${MQTT_PASSWORD:-}"
REQUIRE_MQTT_RUNTIME="${REQUIRE_MQTT_RUNTIME:-0}"
STRICT_MQTT_BASELINE="${STRICT_MQTT_BASELINE:-1}"

TMP_DIR="$(mktemp -d /tmp/maps-scenario-composer-runtime-smoke.XXXXXX)"
trap 'rm -rf "${TMP_DIR}"' EXIT

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
  python3 "${ROOT}/skills/smoke/ensure_mqtt_listener.py" "${TMP_DIR}/NetworkManager.yaml"
fi

RUNTIME_LISTENERS="8080"
if [[ "${REQUIRE_MQTT_RUNTIME}" == "1" ]]; then
  RUNTIME_LISTENERS="8080,1883"
fi

CMD=(bash "${HARNESS}"
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
  echo "maps-scenario-composer runtime smoke PASS"
  exit 0
fi

if timeout 3 bash -lc "echo > /dev/tcp/127.0.0.1/${MQTT_PORT}" 2>/dev/null; then
  AUTH=()
  if [[ -n "${MQTT_USER}" ]]; then AUTH+=( -u "${MQTT_USER}" ); fi
  if [[ -n "${MQTT_PASSWORD}" ]]; then AUTH+=( -P "${MQTT_PASSWORD}" ); fi
  CORR="maps_scenario_composer_runtime_smoke-$(date +%s)"
  timeout 8 mosquitto_sub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${TOPIC}" -C 1 "${AUTH[@]}" >/tmp/maps-scenario-composer-runtime-smoke.out 2>/tmp/maps-scenario-composer-runtime-smoke.err &
  SPID=$!
  sleep 1
  mosquitto_pub -V mqttv311 -h 127.0.0.1 -p "${MQTT_PORT}" -t "${TOPIC}" -m "${CORR}" "${AUTH[@]}"
  wait "${SPID}"
  rg -n "${CORR}" /tmp/maps-scenario-composer-runtime-smoke.out >/dev/null
else
  echo "MQTT listener not available on ${MQTT_PORT}; startup/listener runtime check passed without maps_scenario_composer_runtime_smoke path."
fi

echo "maps-scenario-composer runtime smoke PASS"
