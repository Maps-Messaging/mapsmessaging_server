#!/usr/bin/env bash
set -euo pipefail

HOST="127.0.0.1"
PORT="1883"
SOURCE_TOPIC="/selector/in"
MATCH_TOPIC="/selector/match"
NON_MATCH_TOPIC="/selector/nonmatch"
POSITIVE_PAYLOAD='{"kind":"priority","priority":9}'
NEGATIVE_PAYLOAD='{"kind":"normal","priority":1}'
USERNAME=""
PASSWORD=""

usage() {
  cat <<USAGE
Usage: $(basename "$0") [options]

Options:
  --host <host>                Broker host (default: ${HOST})
  --port <port>                Broker port (default: ${PORT})
  --source-topic <topic>       Source publish topic (default: ${SOURCE_TOPIC})
  --match-topic <topic>        Expected match topic (default: ${MATCH_TOPIC})
  --nonmatch-topic <topic>     Expected non-match topic (default: ${NON_MATCH_TOPIC})
  --positive-payload <json>    Payload expected to hit match selector
  --negative-payload <json>    Payload expected to hit non-match selector
  --username <user>            MQTT username (optional)
  --password <pass>            MQTT password (optional)
  -h, --help                   Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --source-topic) SOURCE_TOPIC="$2"; shift 2 ;;
    --match-topic) MATCH_TOPIC="$2"; shift 2 ;;
    --nonmatch-topic) NON_MATCH_TOPIC="$2"; shift 2 ;;
    --positive-payload) POSITIVE_PAYLOAD="$2"; shift 2 ;;
    --negative-payload) NEGATIVE_PAYLOAD="$2"; shift 2 ;;
    --username) USERNAME="$2"; shift 2 ;;
    --password) PASSWORD="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
  esac
done

MQTT_AUTH_ARGS=()
if [[ -n "${USERNAME}" ]]; then MQTT_AUTH_ARGS+=( -u "${USERNAME}" ); fi
if [[ -n "${PASSWORD}" ]]; then MQTT_AUTH_ARGS+=( -P "${PASSWORD}" ); fi

POS_MATCH="/tmp/selector-pos-match.out"
POS_NONMATCH="/tmp/selector-pos-nonmatch.out"
NEG_MATCH="/tmp/selector-neg-match.out"
NEG_NONMATCH="/tmp/selector-neg-nonmatch.out"

: >"${POS_MATCH}"; : >"${POS_NONMATCH}"; : >"${NEG_MATCH}"; : >"${NEG_NONMATCH}"

# Subscribe before publish.
timeout 12 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${MATCH_TOPIC}" -C 1 "${MQTT_AUTH_ARGS[@]}" >"${POS_MATCH}" 2>/tmp/selector-pos-match.err &
PID_PM=$!
timeout 4 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${NON_MATCH_TOPIC}" -C 1 "${MQTT_AUTH_ARGS[@]}" >"${POS_NONMATCH}" 2>/tmp/selector-pos-nonmatch.err &
PID_PN=$!
sleep 1
mosquitto_pub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${SOURCE_TOPIC}" -m "${POSITIVE_PAYLOAD}" "${MQTT_AUTH_ARGS[@]}"

wait "${PID_PM}"
wait "${PID_PN}" || true

if [[ ! -s "${POS_MATCH}" ]]; then
  echo "positive case failed: no match-topic message" >&2
  exit 1
fi
if [[ -s "${POS_NONMATCH}" ]]; then
  echo "positive case failed: unexpected nonmatch-topic message" >&2
  exit 1
fi

# Negative payload should route to non-match.
timeout 4 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${MATCH_TOPIC}" -C 1 "${MQTT_AUTH_ARGS[@]}" >"${NEG_MATCH}" 2>/tmp/selector-neg-match.err &
PID_NM=$!
timeout 12 mosquitto_sub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${NON_MATCH_TOPIC}" -C 1 "${MQTT_AUTH_ARGS[@]}" >"${NEG_NONMATCH}" 2>/tmp/selector-neg-nonmatch.err &
PID_NN=$!
sleep 1
mosquitto_pub -V mqttv311 -h "${HOST}" -p "${PORT}" -t "${SOURCE_TOPIC}" -m "${NEGATIVE_PAYLOAD}" "${MQTT_AUTH_ARGS[@]}"

wait "${PID_NM}" || true
wait "${PID_NN}"

if [[ -s "${NEG_MATCH}" ]]; then
  echo "negative case failed: unexpected match-topic message" >&2
  exit 1
fi
if [[ ! -s "${NEG_NONMATCH}" ]]; then
  echo "negative case failed: no nonmatch-topic message" >&2
  exit 1
fi

echo "selector mqtt smoke PASS"
