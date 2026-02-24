#!/usr/bin/env bash
set -euo pipefail

ROOT="/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-satellite-gateway-config"
VALIDATOR="/Users/krital/.codex/skills/.system/skill-creator/scripts/quick_validate.py"

python3 "${VALIDATOR}" "${ROOT}"
test -f "${ROOT}/references/output-contract.md"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_satellite_gateway_config_runtime_smoke.sh"
fi
rg -n "Scenario Metrics and Dashboard|C4 Architecture Diagram" "${ROOT}/references/output-contract.md" >/dev/null
test -f "${ROOT}/references/satellite-pubsub-patterns.md"
test -f "${ROOT}/references/satellite-config-map.md"
rg -n "CBC" "${ROOT}/SKILL.md" >/dev/null
echo "maps-satellite-gateway-config skill smoke PASS"
