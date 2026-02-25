#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODEX_HOME_DEFAULT="${CODEX_HOME:-$HOME/.codex}"
VALIDATOR="${VALIDATOR:-${CODEX_HOME_DEFAULT}/skills/.system/skill-creator/scripts/quick_validate.py}"
python3 "${VALIDATOR}" "${ROOT}"
test -f "${ROOT}/references/output-contract.md"
rg -n "Scenario Metrics and Dashboard|C4 Architecture Diagram" "${ROOT}/references/output-contract.md" >/dev/null
test -f "${ROOT}/references/transform-chain-patterns.md"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_transform_chain_designer_runtime_smoke.sh"
fi
echo "maps-transform-chain-designer skill smoke PASS"
