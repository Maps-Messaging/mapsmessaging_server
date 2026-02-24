#!/usr/bin/env bash
set -euo pipefail

ROOT="/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-scenario-composer"
VALIDATOR="/Users/krital/.codex/skills/.system/skill-creator/scripts/quick_validate.py"
OUT="/tmp/maps-scenario-composer.out"
MERGED_DIR="/tmp/maps-scenario-composer-merged"
TMP_A="/tmp/maps-scenario-composer-a.yaml"
TMP_B="/tmp/maps-scenario-composer-b.yaml"
TMP_RENAME="/tmp/maps-scenario-composer-rename.out"
TMP_OVERRIDE="/tmp/maps-scenario-composer-override.out"

python3 "${VALIDATOR}" "${ROOT}"
python3 "${ROOT}/scripts/compose_scenarios.py" \
  --skills maps-deployment-packager,maps-selector-rule-engineer,maps-geospatial-routing-builder \
  --mode hybrid \
  --artifact maps-deployment-packager=/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-deployment-packager/references/output-contract.md \
  --artifact maps-selector-rule-engineer=/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-selector-rule-engineer/references/examples/simple-selector-output.md \
  --artifact maps-geospatial-routing-builder=/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-geospatial-routing-builder/references/examples/simple-geospatial-output.md \
  --conflict-policy override \
  --out-dir "${MERGED_DIR}" >"${OUT}"
rg -n "Composition Matrix|Unified Assumptions|Merged Deployable Entity|Unified Apply Sequence|Integrated Verification|Traceability Map|Conflict Resolution Log|Scenario Metrics and Dashboard|C4 Architecture Diagram" "${OUT}" >/dev/null
test -f "${MERGED_DIR}/routing.yaml"

cat >"${TMP_A}" <<YAML
routing:
  autoDiscovery: false
YAML
cat >"${TMP_B}" <<YAML
routing:
  autoDiscovery: true
YAML

# strict-fail must abort on conflict
if python3 "${ROOT}/scripts/compose_scenarios.py" \
  --skills maps-selector-rule-engineer,maps-geospatial-routing-builder \
  --mode additive \
  --conflict-policy strict-fail \
  --artifact maps-selector-rule-engineer="${TMP_A}" \
  --artifact maps-geospatial-routing-builder="${TMP_B}" >/tmp/maps-scenario-composer-strict.out; then
  echo "strict-fail policy did not fail on conflict" >&2
  exit 1
fi

# rename must preserve incoming conflict with suffix
python3 "${ROOT}/scripts/compose_scenarios.py" \
  --skills maps-selector-rule-engineer,maps-geospatial-routing-builder \
  --mode additive \
  --conflict-policy rename \
  --artifact maps-selector-rule-engineer="${TMP_A}" \
  --artifact maps-geospatial-routing-builder="${TMP_B}" >"${TMP_RENAME}"
rg -n "autoDiscovery__from_maps_geospatial_routing_builder" "${TMP_RENAME}" >/dev/null

# override must prefer later skill value
python3 "${ROOT}/scripts/compose_scenarios.py" \
  --skills maps-selector-rule-engineer,maps-geospatial-routing-builder \
  --mode additive \
  --conflict-policy override \
  --artifact maps-selector-rule-engineer="${TMP_A}" \
  --artifact maps-geospatial-routing-builder="${TMP_B}" >"${TMP_OVERRIDE}"
rg -n "autoDiscovery: true" "${TMP_OVERRIDE}" >/dev/null

if [[ "${RUN_RUNTIME_SMOKE:-0}" == "1" ]]; then
  bash "${ROOT}/scripts/run_maps_scenario_composer_runtime_smoke.sh"
fi

echo "scenario composer skill smoke PASS"
