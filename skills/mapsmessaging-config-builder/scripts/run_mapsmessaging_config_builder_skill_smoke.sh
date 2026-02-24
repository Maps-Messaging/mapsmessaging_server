#!/usr/bin/env bash
set -euo pipefail
ROOT="/Users/krital/dev/starsense/mapsmessaging_server/skills/mapsmessaging-config-builder"
VALIDATOR="/Users/krital/.codex/skills/.system/skill-creator/scripts/quick_validate.py"
python3 "${VALIDATOR}" "${ROOT}"
test -f "${ROOT}/references/output-contract.md"
rg -n "Scenario Metrics and Dashboard|C4 Architecture Diagram" "${ROOT}/references/output-contract.md" >/dev/null
test -f "${ROOT}/references/protocol-and-routing-map.md"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_mapsmessaging_config_builder_runtime_smoke.sh"
fi
echo "mapsmessaging-config-builder skill smoke PASS"
