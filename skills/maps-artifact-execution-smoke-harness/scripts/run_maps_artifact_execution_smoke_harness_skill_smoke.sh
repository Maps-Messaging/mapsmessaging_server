#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODEX_HOME_DEFAULT="${CODEX_HOME:-$HOME/.codex}"
VALIDATOR="${VALIDATOR:-${CODEX_HOME_DEFAULT}/skills/.system/skill-creator/scripts/quick_validate.py}"
python3 "${VALIDATOR}" "${ROOT}"
test -f "${ROOT}/references/output-contract.md"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_artifact_execution_smoke_harness_runtime_smoke.sh"
fi
rg -n "Scenario Metrics and Dashboard|C4 Architecture Diagram" "${ROOT}/references/output-contract.md" >/dev/null
test -f "${ROOT}/references/execution-catalog.md"; bash -n "${ROOT}/scripts/run_artifact_smoke.sh"; bash -n "${ROOT}/scripts/run_matrix.sh"
echo "maps-artifact-execution-smoke-harness skill smoke PASS"
