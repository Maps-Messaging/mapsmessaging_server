#!/usr/bin/env bash
set -euo pipefail

ROOT="/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-stream-configurator"
VALIDATOR="/Users/krital/.codex/skills/.system/skill-creator/scripts/quick_validate.py"

python3 "${VALIDATOR}" "${ROOT}"
test -f "${ROOT}/references/output-contract.md"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_ml_stream_configurator_runtime_smoke.sh"
fi
rg -n "Scenario Metrics and Dashboard|C4 Architecture Diagram" "${ROOT}/references/output-contract.md" >/dev/null
test -f "${ROOT}/references/ml-pipeline-patterns.md"
rg -n "simple baseline" "${ROOT}/references/output-contract.md" >/dev/null
echo "maps-ml-stream-configurator skill smoke PASS"
