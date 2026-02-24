#!/usr/bin/env bash
set -euo pipefail

ROOT="/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-selector-rule-engineer"
VALIDATOR="/Users/krital/.codex/skills/.system/skill-creator/scripts/quick_validate.py"

python3 "${VALIDATOR}" "${ROOT}"
python3 "${ROOT}/scripts/validate_selector_artifacts.py"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_selector_rule_engineer_runtime_smoke.sh"
fi
echo "selector skill smoke PASS"
