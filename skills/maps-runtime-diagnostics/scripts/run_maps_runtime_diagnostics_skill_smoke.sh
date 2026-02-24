#!/usr/bin/env bash
set -euo pipefail
ROOT="/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-runtime-diagnostics"
VALIDATOR="/Users/krital/.codex/skills/.system/skill-creator/scripts/quick_validate.py"
python3 "${VALIDATOR}" "${ROOT}"
test -f "${ROOT}/references/output-contract.md"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_runtime_diagnostics_runtime_smoke.sh"
fi
rg -n "Scenario Metrics and Dashboard|C4 Architecture Diagram" "${ROOT}/references/output-contract.md" >/dev/null
test -f "${ROOT}/references/startup-gates-and-bind-checks.md"
echo "maps-runtime-diagnostics skill smoke PASS"
