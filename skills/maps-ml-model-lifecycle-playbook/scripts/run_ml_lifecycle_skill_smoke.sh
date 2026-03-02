#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODEX_HOME_DEFAULT="${CODEX_HOME:-$HOME/.codex}"
VALIDATOR="${VALIDATOR:-${CODEX_HOME_DEFAULT}/skills/.system/skill-creator/scripts/quick_validate.py}"

python3 "${VALIDATOR}" "${ROOT}"
python3 "${ROOT}/scripts/validate_ml_lifecycle_artifacts.py"
python3 "${ROOT}/scripts/run_ml_lifecycle_vectors.py"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_ml_lifecycle_runtime_smoke.sh"
fi
echo "ml lifecycle skill smoke PASS"
