#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODEX_HOME_DEFAULT="${CODEX_HOME:-$HOME/.codex}"
VALIDATOR="${VALIDATOR:-${CODEX_HOME_DEFAULT}/skills/.system/skill-creator/scripts/quick_validate.py}"

python3 "${VALIDATOR}" "${ROOT}"
rg -n "maps-scenario-composer|Quick Start Template|Advanced Template|Multi-Skill Unified Deliverable" "${ROOT}/SKILL.md" >/dev/null

if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_skill_suite_orchestrator_runtime_smoke.sh"
fi

echo "maps-skill-suite-orchestrator skill smoke PASS"
