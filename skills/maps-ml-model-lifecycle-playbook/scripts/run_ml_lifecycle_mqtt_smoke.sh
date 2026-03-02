#!/usr/bin/env bash
set -euo pipefail

HOST="127.0.0.1"
PORT="1883"
SOURCE_TOPIC="/ml/in"
STAGE1_TOPIC="/ml/stage1"
FINAL_TOPIC="/ml/final"
OUTLIER_TOPIC="/ml/outlier"
PAYLOAD='{"featureA":1.2,"featureB":3.4,"label":"normal"}'
USERNAME=""
PASSWORD=""

usage() {
  cat <<USAGE
Usage: $(basename "$0") [options]

Options:
  --host <host>              Broker host (default: ${HOST})
  --port <port>              Broker port (default: ${PORT})
  --source-topic <topic>     Input stream topic (default: ${SOURCE_TOPIC})
  --stage1-topic <topic>     Stage-1 output topic (default: ${STAGE1_TOPIC})
  --final-topic <topic>      Final output topic (default: ${FINAL_TOPIC})
  --outlier-topic <topic>    Outlier output topic (default: ${OUTLIER_TOPIC})
  --payload <json>           Test payload
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
    --stage1-topic) STAGE1_TOPIC="$2"; shift 2 ;;
    --final-topic) FINAL_TOPIC="$2"; shift 2 ;;
    --outlier-topic) OUTLIER_TOPIC="$2"; shift 2 ;;
    --payload) PAYLOAD="$2"; shift 2 ;;
    --username) USERNAME="$2"; shift 2 ;;
    --password) PASSWORD="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
  esac
done

AUTH=()
if [[ -n "${USERNAME}" ]]; then AUTH+=( -u "${USERNAME}" ); fi
if [[ -n "${PASSWORD}" ]]; then AUTH+=( -P "${PASSWORD}" ); fi

# Stage-1 check: subscribe before publish
timeout 12 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${STAGE1_TOPIC}" -C 1 "${AUTH[@]}" >/tmp/ml-stage1.out 2>/tmp/ml-stage1.err &
PID_S1=$!
sleep 1
mosquitto_pub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${SOURCE_TOPIC}" -m "${PAYLOAD}" "${AUTH[@]}"
wait "${PID_S1}"
[[ -s /tmp/ml-stage1.out ]] || { echo "stage1 inference output missing" >&2; exit 1; }

# Final stage check: final OR outlier must receive one event
timeout 10 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${FINAL_TOPIC}" -C 1 "${AUTH[@]}" >/tmp/ml-final.out 2>/tmp/ml-final.err &
PID_F=$!
timeout 10 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${OUTLIER_TOPIC}" -C 1 "${AUTH[@]}" >/tmp/ml-outlier.out 2>/tmp/ml-outlier.err &
PID_O=$!
sleep 1
mosquitto_pub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${SOURCE_TOPIC}" -m "${PAYLOAD}" "${AUTH[@]}"
wait "${PID_F}" || true
wait "${PID_O}" || true

if [[ ! -s /tmp/ml-final.out && ! -s /tmp/ml-outlier.out ]]; then
  echo "neither final nor outlier stream received output" >&2
  exit 1
fi

echo "ml lifecycle mqtt smoke PASS"
