#!/usr/bin/env bash
set -euo pipefail

HOST="127.0.0.1"
PORT="1883"
SOURCE_TOPIC="/geo/in"
NEAR_TOPIC="/geo/out/near"
FAR_TOPIC="/geo/out/far"
INVALID_TOPIC="/geo/out/invalid"
NEAR_PAYLOAD='{"lat":51.5074,"lon":-0.1278}'
FAR_PAYLOAD='{"lat":40.7128,"lon":-74.0060}'
INVALID_PAYLOAD='{"lat":123.45,"lon":8.3}'
USERNAME=""
PASSWORD=""

usage() {
  cat <<USAGE
Usage: $(basename "$0") [options]

Options:
  --host <host>              Broker host (default: ${HOST})
  --port <port>              Broker port (default: ${PORT})
  --source-topic <topic>     Source publish topic (default: ${SOURCE_TOPIC})
  --near-topic <topic>       Expected near route topic (default: ${NEAR_TOPIC})
  --far-topic <topic>        Expected far route topic (default: ${FAR_TOPIC})
  --invalid-topic <topic>    Expected invalid route topic (default: ${INVALID_TOPIC})
  --near-payload <json>      Near-route payload
  --far-payload <json>       Far-route payload
  --invalid-payload <json>   Invalid-route payload
  --username <user>          MQTT username (optional)
  --password <pass>          MQTT password (optional)
  -h, --help                 Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --source-topic) SOURCE_TOPIC="$2"; shift 2 ;;
    --near-topic) NEAR_TOPIC="$2"; shift 2 ;;
    --far-topic) FAR_TOPIC="$2"; shift 2 ;;
    --invalid-topic) INVALID_TOPIC="$2"; shift 2 ;;
    --near-payload) NEAR_PAYLOAD="$2"; shift 2 ;;
    --far-payload) FAR_PAYLOAD="$2"; shift 2 ;;
    --invalid-payload) INVALID_PAYLOAD="$2"; shift 2 ;;
    --username) USERNAME="$2"; shift 2 ;;
    --password) PASSWORD="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
  esac
done

AUTH=()
if [[ -n "${USERNAME}" ]]; then AUTH+=( -u "${USERNAME}" ); fi
if [[ -n "${PASSWORD}" ]]; then AUTH+=( -P "${PASSWORD}" ); fi

# near case: subscribe first, then publish.
timeout 10 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${NEAR_TOPIC}" -C 1 "${AUTH[@]}" >/tmp/geo-near.out 2>/tmp/geo-near.err &
PID_NEAR=$!
sleep 1
mosquitto_pub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${SOURCE_TOPIC}" -m "${NEAR_PAYLOAD}" "${AUTH[@]}"
wait "${PID_NEAR}"
[[ -s /tmp/geo-near.out ]] || { echo "near route failed" >&2; exit 1; }

# far case
timeout 10 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${FAR_TOPIC}" -C 1 "${AUTH[@]}" >/tmp/geo-far.out 2>/tmp/geo-far.err &
PID_FAR=$!
sleep 1
mosquitto_pub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${SOURCE_TOPIC}" -m "${FAR_PAYLOAD}" "${AUTH[@]}"
wait "${PID_FAR}"
[[ -s /tmp/geo-far.out ]] || { echo "far route failed" >&2; exit 1; }

# invalid case
timeout 10 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${INVALID_TOPIC}" -C 1 "${AUTH[@]}" >/tmp/geo-invalid.out 2>/tmp/geo-invalid.err &
PID_INV=$!
sleep 1
mosquitto_pub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${SOURCE_TOPIC}" -m "${INVALID_PAYLOAD}" "${AUTH[@]}"
wait "${PID_INV}"
[[ -s /tmp/geo-invalid.out ]] || { echo "invalid route failed" >&2; exit 1; }

echo "geospatial mqtt smoke PASS"
