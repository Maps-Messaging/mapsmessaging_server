#!/usr/bin/env bash
set -euo pipefail

IMAGE="mapsmessaging/server_daemon:latest"
CONTAINER_NAME="maps-artifact-smoke"
ARTIFACT_DIR=""
HOST_MQTT_PORT="1883"
HOST_HTTP_PORT="8080"
TOPIC="/skill/artifact/smoke"
SKIP_MQTT=0
FORCE_CLEAN=0
CONTAINER_CONF_DIR="/message_daemon-3.3.0/conf"
TARGET_PLATFORM=""
MQTT_USERNAME=""
MQTT_PASSWORD=""
REQUIRED_LISTENERS=""

usage() {
  cat <<USAGE
Usage: $(basename "$0") [options]

Options:
  --image <image>              Docker image (default: ${IMAGE})
  --container-name <name>      Container name (default: ${CONTAINER_NAME})
  --artifact-dir <dir>         Directory containing MAPS manager YAML artifacts to mount
  --mqtt-port <port>           Host MQTT port (default: ${HOST_MQTT_PORT})
  --http-port <port>           Host HTTP port (default: ${HOST_HTTP_PORT})
  --topic <topic>              MQTT smoke topic (default: ${TOPIC})
  --container-conf-dir <dir>   Config directory inside container (default: ${CONTAINER_CONF_DIR})
  --platform <linux/arch>      Docker target platform (for example linux/amd64, linux/arm64)
  --mqtt-username <value>      MQTT username for smoke publish/subscribe (optional)
  --mqtt-password <value>      MQTT password for smoke publish/subscribe (optional)
  --required-listeners <csv>   Required container listener ports (for example 1883,8080)
  --skip-mqtt                  Skip MQTT publish/subscribe smoke check
  --force-clean                Remove existing container with same name before run
  -h, --help                   Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --image)
      IMAGE="$2"; shift 2 ;;
    --container-name)
      CONTAINER_NAME="$2"; shift 2 ;;
    --artifact-dir)
      ARTIFACT_DIR="$2"; shift 2 ;;
    --mqtt-port)
      HOST_MQTT_PORT="$2"; shift 2 ;;
    --http-port)
      HOST_HTTP_PORT="$2"; shift 2 ;;
    --topic)
      TOPIC="$2"; shift 2 ;;
    --container-conf-dir)
      CONTAINER_CONF_DIR="$2"; shift 2 ;;
    --platform)
      TARGET_PLATFORM="$2"; shift 2 ;;
    --mqtt-username)
      MQTT_USERNAME="$2"; shift 2 ;;
    --mqtt-password)
      MQTT_PASSWORD="$2"; shift 2 ;;
    --required-listeners)
      REQUIRED_LISTENERS="$2"; shift 2 ;;
    --skip-mqtt)
      SKIP_MQTT=1; shift ;;
    --force-clean)
      FORCE_CLEAN=1; shift ;;
    -h|--help)
      usage; exit 0 ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2 ;;
  esac
done

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

port_in_use() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  if command -v ss >/dev/null 2>&1; then
    ss -lnt | awk '{print $4}' | grep -E "[:.]${port}$" >/dev/null 2>&1
    return $?
  fi
  if command -v netstat >/dev/null 2>&1; then
    netstat -lnt 2>/dev/null | awk '{print $4}' | grep -E "[:.]${port}$" >/dev/null 2>&1
    return $?
  fi
  echo "No supported socket tool found (lsof/ss/netstat)." >&2
  exit 1
}

cleanup() {
  docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
}

require_cmd docker
if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is not accessible from this environment." >&2
  exit 1
fi

if [[ "${FORCE_CLEAN}" -eq 1 ]]; then
  cleanup
fi

if [[ -z "${REQUIRED_LISTENERS}" ]]; then
  if [[ "${SKIP_MQTT}" -eq 0 ]]; then
    REQUIRED_LISTENERS="1883"
  else
    REQUIRED_LISTENERS=""
  fi
fi

if docker ps -a --format '{{.Names}}' | grep -Fx "${CONTAINER_NAME}" >/dev/null 2>&1; then
  echo "Container ${CONTAINER_NAME} already exists. Use --force-clean to replace it." >&2
  exit 1
fi

if port_in_use "${HOST_MQTT_PORT}"; then
  echo "Port ${HOST_MQTT_PORT} is already in use; refusing to start smoke run." >&2
  exit 1
fi
if port_in_use "${HOST_HTTP_PORT}"; then
  echo "Port ${HOST_HTTP_PORT} is already in use; refusing to start smoke run." >&2
  exit 1
fi

RUN_ARGS=(
  run -d
  --name "${CONTAINER_NAME}"
  -p "${HOST_MQTT_PORT}:1883/tcp"
  -p "${HOST_HTTP_PORT}:8080/tcp"
)
if [[ -n "${TARGET_PLATFORM}" ]]; then
  RUN_ARGS+=( --platform "${TARGET_PLATFORM}" )
fi

if [[ -n "${ARTIFACT_DIR}" ]]; then
  if [[ ! -d "${ARTIFACT_DIR}" ]]; then
    echo "Artifact directory does not exist: ${ARTIFACT_DIR}" >&2
    exit 1
  fi
  for f in AggregatorManager.yaml AuthManager.yaml DestinationManager.yaml DeviceManager.yaml DiscoveryManager.yaml License.yaml LoRaDevice.yaml MLModelManager.yaml MessageDaemon.yaml NetworkConnectionManager.yaml NetworkManager.yaml RestApi.yaml SchemaManager.yaml SecurityManager.yaml TenantManagement.yaml jolokia.yaml routing.yaml; do
    if [[ -f "${ARTIFACT_DIR}/${f}" ]]; then
      RUN_ARGS+=( -v "${ARTIFACT_DIR}/${f}:${CONTAINER_CONF_DIR}/${f}:ro" )
    fi
  done
fi

RUN_ARGS+=("${IMAGE}")

echo "Starting container ${CONTAINER_NAME} using image ${IMAGE}"
docker "${RUN_ARGS[@]}" >/dev/null
trap cleanup EXIT

sleep 4

if [[ "$(docker inspect -f '{{.State.Running}}' "${CONTAINER_NAME}")" != "true" ]]; then
  echo "Container exited during startup" >&2
  docker logs "${CONTAINER_NAME}" 2>&1 | sed -n '1,200p' >&2
  exit 1
fi

echo "Checking startup logs for hard blockers"
if docker logs "${CONTAINER_NAME}" 2>&1 | rg -n "Address already in use|BindException|Failed to bind|Protocol not available|Cannot assign requested address" >/tmp/${CONTAINER_NAME}-errors.log; then
  echo "Startup blockers detected:" >&2
  sed -n '1,120p' /tmp/${CONTAINER_NAME}-errors.log >&2
  exit 1
fi

if docker logs "${CONTAINER_NAME}" 2>&1 | rg -n "ConsulManagerFactory|Consol Server is not responding" >/tmp/${CONTAINER_NAME}-consul.warn; then
  echo "Non-blocking warning: Consul unavailable, expecting file-based fallback."
  sed -n '1,80p' /tmp/${CONTAINER_NAME}-consul.warn
fi

echo "Verifying listeners inside container"
docker exec "${CONTAINER_NAME}" sh -lc '(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | sed -n "1,120p"' >/tmp/${CONTAINER_NAME}-listeners.log || true
if [[ -n "${REQUIRED_LISTENERS}" ]]; then
  attempt=0
  while true; do
    missing=0
    IFS=',' read -r -a ports <<< "${REQUIRED_LISTENERS}"
    for p in "${ports[@]}"; do
      p_trimmed="$(echo "${p}" | tr -d '[:space:]')"
      if [[ -z "${p_trimmed}" ]]; then
        continue
      fi
      if ! grep -q ":${p_trimmed}" /tmp/${CONTAINER_NAME}-listeners.log; then
        missing=1
        break
      fi
    done
    if [[ "${missing}" -eq 0 ]]; then
      break
    fi
    attempt=$((attempt + 1))
    if [[ "${attempt}" -ge 30 ]]; then
      echo "Listener check failed: expected listeners [${REQUIRED_LISTENERS}] were not all bound" >&2
      sed -n '1,120p' /tmp/${CONTAINER_NAME}-listeners.log >&2 || true
      exit 1
    fi
    sleep 1
    docker exec "${CONTAINER_NAME}" sh -lc '(ss -lnt 2>/dev/null || netstat -lnt 2>/dev/null || netstat -an 2>/dev/null) | sed -n "1,120p"' >/tmp/${CONTAINER_NAME}-listeners.log || true
  done
fi

if ! docker exec "${CONTAINER_NAME}" sh -lc 'echo ok' >/dev/null 2>&1; then
  echo "Listener check failed" >&2
  exit 1
fi

if [[ "${SKIP_MQTT}" -eq 0 ]]; then
  require_cmd mosquitto_pub
  require_cmd mosquitto_sub

  CORR="artifact-smoke-$(date +%s)"
  success=0
  for attempt in 1 2 3; do
    SUB_OUT="/tmp/${CONTAINER_NAME}-mqtt-${attempt}.out"
    SUB_ERR="/tmp/${CONTAINER_NAME}-mqtt-sub-${attempt}.err"
    PUB_ERR="/tmp/${CONTAINER_NAME}-mqtt-pub-${attempt}.err"
    : >"${SUB_OUT}"
    : >"${SUB_ERR}"
    : >"${PUB_ERR}"

    echo "Running MQTT round-trip smoke check (attempt ${attempt}/3) on topic ${TOPIC}"
    SUB_CMD=(timeout 12 mosquitto_sub -d -V mqttv311 -h 127.0.0.1 -p "${HOST_MQTT_PORT}" -t "${TOPIC}" -C 1)
    if [[ -n "${MQTT_USERNAME}" ]]; then SUB_CMD+=( -u "${MQTT_USERNAME}" ); fi
    if [[ -n "${MQTT_PASSWORD}" ]]; then SUB_CMD+=( -P "${MQTT_PASSWORD}" ); fi
    "${SUB_CMD[@]}" >"${SUB_OUT}" 2>"${SUB_ERR}" &
    SUB_PID=$!
    for _ in 1 2 3 4 5; do
      if ! kill -0 "${SUB_PID}" 2>/dev/null; then
        break
      fi
      if rg -n "CONNACK|SUBACK|Subscribed" "${SUB_ERR}" >/dev/null 2>&1; then
        break
      fi
      sleep 1
    done

    PUB_CMD=(mosquitto_pub -V mqttv311 -h 127.0.0.1 -p "${HOST_MQTT_PORT}" -t "${TOPIC}" -q 1 -m "${CORR}")
    if [[ -n "${MQTT_USERNAME}" ]]; then PUB_CMD+=( -u "${MQTT_USERNAME}" ); fi
    if [[ -n "${MQTT_PASSWORD}" ]]; then PUB_CMD+=( -P "${MQTT_PASSWORD}" ); fi
    if ! "${PUB_CMD[@]}" 2>"${PUB_ERR}"; then
      wait "${SUB_PID}" || true
      sleep 1
      continue
    fi

    if wait "${SUB_PID}" && grep -F "${CORR}" "${SUB_OUT}" >/dev/null 2>&1; then
      success=1
      break
    fi
    sleep 1
  done

  if [[ "${success}" -ne 1 ]]; then
    echo "MQTT smoke failed after retries" >&2
    sed -n '1,80p' /tmp/${CONTAINER_NAME}-mqtt-sub-3.err >&2 || true
    sed -n '1,80p' /tmp/${CONTAINER_NAME}-mqtt-pub-3.err >&2 || true
    docker logs "${CONTAINER_NAME}" 2>&1 | tail -n 120 >&2 || true
    exit 1
  fi
fi

echo "Artifact smoke PASS"
