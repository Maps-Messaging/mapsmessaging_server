#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SKILLS_ARG=""
DEEP=0
BOOTSTRAP=0

usage() {
  cat <<USAGE
Usage: $(basename "$0") --skills <skill1,skill2,...> [options]

Options:
  --skills <csv>        Comma-separated skill names to test (required)
  --deep                Also run skill-specific deep smoke scripts when available
  --bootstrap-fixtures  Regenerate fixtures before contract smoke
  -h, --help            Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skills)
      SKILLS_ARG="$2"; shift 2 ;;
    --deep)
      DEEP=1; shift ;;
    --bootstrap-fixtures)
      BOOTSTRAP=1; shift ;;
    -h|--help)
      usage; exit 0 ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2 ;;
  esac
done

if [[ -z "${SKILLS_ARG}" ]]; then
  echo "--skills is required" >&2
  usage
  exit 2
fi

VALIDATE_CMD=(python3 "${ROOT_DIR}/skills/smoke/validate_skills_smoke.py" --skills "${SKILLS_ARG}")
if [[ "${BOOTSTRAP}" -eq 1 ]]; then
  VALIDATE_CMD+=(--bootstrap-fixtures)
fi
"${VALIDATE_CMD[@]}"

if [[ "${DEEP}" -eq 1 ]]; then
  IFS=',' read -r -a SKILLS <<< "${SKILLS_ARG}"
  for skill in "${SKILLS[@]}"; do
    s="$(echo "${skill}" | tr -d '[:space:]')"
    if [[ -z "${s}" ]]; then
      continue
    fi
    script_path="$(find "${ROOT_DIR}/skills/${s}/scripts" -maxdepth 1 -type f -name '*skill_smoke.sh' 2>/dev/null | head -n 1 || true)"
    if [[ -n "${script_path}" ]]; then
      bash "${script_path}"
    else
      echo "No deep smoke script for ${s}; contract smoke only."
    fi
  done
fi

echo "Skill combination smoke PASS (${SKILLS_ARG})"
