#!/usr/bin/env bash
set -euo pipefail
ROOT="/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-schema-pipeline-builder"
VALIDATOR="/Users/krital/.codex/skills/.system/skill-creator/scripts/quick_validate.py"
python3 "${VALIDATOR}" "${ROOT}"
test -f "${ROOT}/references/output-contract.md"
rg -n "Scenario Metrics and Dashboard|C4 Architecture Diagram" "${ROOT}/references/output-contract.md" >/dev/null
test -f "${ROOT}/references/schema-mapping-guide.md"
if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_schema_pipeline_builder_runtime_smoke.sh"
fi
echo "maps-schema-pipeline-builder skill smoke PASS"
