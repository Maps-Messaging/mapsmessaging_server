#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNNER="${SCRIPT_DIR}/run_artifact_smoke.sh"

IMAGE=""
ARTIFACT_DIR="${ROOT_DIR}"
BASE_MQTT_PORT="1883"
BASE_HTTP_PORT="8080"
INCLUDE_DEFAULT_IMAGE=0
INCLUDE_UNPATCHED=0
CHANNEL="release"
VERSION="4.3.1-snapshot"
ARCH="auto"
PLATFORM=""
SKIP_MQTT=0
MQTT_USERNAME=""
MQTT_PASSWORD=""

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
  if [[ "${CHANNEL}" == "snapshot" ]]; then
    if [[ "${selected_arch}" == "arm64" ]]; then
      echo "mapsmessaging/server_daemon_arm_${VERSION}:latest"
    else
      echo "mapsmessaging/server_daemon_${VERSION}:latest"
    fi
  else
    echo "mapsmessaging/server_daemon:latest"
  fi
}

usage() {
  cat <<USAGE
Usage: $(basename "$0") [options]

Options:
  --image <image>          Docker image to test (overrides channel/version resolution)
  --channel <release|snapshot>  Image channel (default: ${CHANNEL})
  --version <value>        Snapshot version (default: ${VERSION}; used when channel=snapshot)
  --arch <auto|amd64|arm64> Architecture selector for snapshot image tags (default: ${ARCH})
  --platform <linux/arch>  Docker run target platform (default: derived from --arch)
  --skip-mqtt              Skip MQTT traffic smoke and run startup/listener-only checks
  --mqtt-username <value>  MQTT username for smoke checks (optional)
  --mqtt-password <value>  MQTT password for smoke checks (optional)
  --artifact-dir <dir>     Artifact directory for mounted-artifacts scenario (default: repo root)
  --base-mqtt-port <port>  Base host MQTT port (default: ${BASE_MQTT_PORT})
  --base-http-port <port>  Base host HTTP port (default: ${BASE_HTTP_PORT})
  --include-default-image  Also run baked image config scenario (may require Consul)
  --include-unpatched      Also run mounted-artifacts scenario without Consul patch
  -h, --help               Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --image)
      IMAGE="$2"; shift 2 ;;
    --channel)
      CHANNEL="$2"; shift 2 ;;
    --version)
      VERSION="$2"; shift 2 ;;
    --arch)
      ARCH="$2"; shift 2 ;;
    --platform)
      PLATFORM="$2"; shift 2 ;;
    --skip-mqtt)
      SKIP_MQTT=1; shift ;;
    --mqtt-username)
      MQTT_USERNAME="$2"; shift 2 ;;
    --mqtt-password)
      MQTT_PASSWORD="$2"; shift 2 ;;
    --artifact-dir)
      ARTIFACT_DIR="$2"; shift 2 ;;
    --base-mqtt-port)
      BASE_MQTT_PORT="$2"; shift 2 ;;
    --base-http-port)
      BASE_HTTP_PORT="$2"; shift 2 ;;
    --include-default-image)
      INCLUDE_DEFAULT_IMAGE=1; shift ;;
    --include-unpatched)
      INCLUDE_UNPATCHED=1; shift ;;
    -h|--help)
      usage; exit 0 ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2 ;;
  esac
done

if [[ "${CHANNEL}" != "release" && "${CHANNEL}" != "snapshot" ]]; then
  echo "Invalid --channel: ${CHANNEL} (expected release or snapshot)" >&2
  exit 2
fi
if [[ "${ARCH}" != "auto" && "${ARCH}" != "amd64" && "${ARCH}" != "arm64" ]]; then
  echo "Invalid --arch: ${ARCH} (expected auto, amd64, or arm64)" >&2
  exit 2
fi

if [[ -z "${IMAGE}" ]]; then
  SELECTED_ARCH="${ARCH}"
  if [[ "${SELECTED_ARCH}" == "auto" ]]; then
    SELECTED_ARCH="$(detect_arch)"
  fi
  IMAGE="$(resolve_image "${SELECTED_ARCH}")"
fi
if [[ -z "${PLATFORM}" ]]; then
  EFFECTIVE_ARCH="${ARCH}"
  if [[ "${EFFECTIVE_ARCH}" == "auto" ]]; then
    EFFECTIVE_ARCH="$(detect_arch)"
  fi
  PLATFORM="linux/${EFFECTIVE_ARCH}"
fi

EXTRA_ARGS=()
if [[ "${SKIP_MQTT}" -eq 1 ]]; then
  EXTRA_ARGS+=( --skip-mqtt )
fi
if [[ -n "${MQTT_USERNAME}" ]]; then
  EXTRA_ARGS+=( --mqtt-username "${MQTT_USERNAME}" )
fi
if [[ -n "${MQTT_PASSWORD}" ]]; then
  EXTRA_ARGS+=( --mqtt-password "${MQTT_PASSWORD}" )
fi

FIRST_MQTT_PORT="${BASE_MQTT_PORT}"
FIRST_HTTP_PORT="${BASE_HTTP_PORT}"
SECOND_MQTT_PORT="$((BASE_MQTT_PORT + 1))"
SECOND_HTTP_PORT="$((BASE_HTTP_PORT + 1))"
THIRD_MQTT_PORT="$((BASE_MQTT_PORT + 2))"
THIRD_HTTP_PORT="$((BASE_HTTP_PORT + 2))"

TMP_DIR="$(mktemp -d /tmp/maps-artifact-smoke.XXXXXX)"
trap 'rm -rf "${TMP_DIR}"' EXIT

for f in AggregatorManager.yaml AuthManager.yaml DestinationManager.yaml DeviceManager.yaml DiscoveryManager.yaml License.yaml LoRaDevice.yaml MLModelManager.yaml MessageDaemon.yaml NetworkConnectionManager.yaml NetworkManager.yaml RestApi.yaml SchemaManager.yaml SecurityManager.yaml TenantManagement.yaml jolokia.yaml routing.yaml; do
  if [[ -f "${ARTIFACT_DIR}/${f}" ]]; then
    cp "${ARTIFACT_DIR}/${f}" "${TMP_DIR}/${f}"
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

if [[ "${INCLUDE_DEFAULT_IMAGE}" -eq 1 ]]; then
  echo "Scenario 1/3: default image config"
  bash "${RUNNER}" \
    --image "${IMAGE}" \
    --container-name maps-artifact-smoke-default \
    --platform "${PLATFORM}" \
    --mqtt-port "${FIRST_MQTT_PORT}" \
    --http-port "${FIRST_HTTP_PORT}" \
    --topic /skill/artifact/smoke/default \
    "${EXTRA_ARGS[@]}" \
    --force-clean
fi

echo "Scenario 2/3: mounted artifacts (consul-optional patch)"
bash "${RUNNER}" \
  --image "${IMAGE}" \
  --container-name maps-artifact-smoke-mounted-patched \
  --platform "${PLATFORM}" \
  --artifact-dir "${TMP_DIR}" \
  --mqtt-port "${SECOND_MQTT_PORT}" \
  --http-port "${SECOND_HTTP_PORT}" \
  --topic /skill/artifact/smoke/mounted/patched \
  "${EXTRA_ARGS[@]}" \
  --force-clean

if [[ "${INCLUDE_UNPATCHED}" -eq 1 ]]; then
  echo "Scenario 3/3: mounted artifacts (as-is)"
  bash "${RUNNER}" \
    --image "${IMAGE}" \
    --container-name maps-artifact-smoke-mounted-unpatched \
    --platform "${PLATFORM}" \
    --artifact-dir "${ARTIFACT_DIR}" \
    --mqtt-port "${THIRD_MQTT_PORT}" \
    --http-port "${THIRD_HTTP_PORT}" \
    --topic /skill/artifact/smoke/mounted/unpatched \
    "${EXTRA_ARGS[@]}" \
    --force-clean
fi

echo "Matrix smoke PASS"
