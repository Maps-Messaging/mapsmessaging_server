#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Install all skills from this repository into Codex using the system skill installer.

Usage:
  skills/install-all.sh --repo <owner/repo> [--ref <git-ref>] [--dest <codex-skills-dir>] [--dry-run]

Examples:
  skills/install-all.sh --repo acme/maps-skills
  skills/install-all.sh --repo acme/maps-skills --ref main
  skills/install-all.sh --repo acme/maps-skills --ref v1.2.0 --dry-run
EOF
}

REPO=""
REF="main"
DEST=""
DRY_RUN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)
      REPO="${2:-}"
      shift 2
      ;;
    --ref)
      REF="${2:-}"
      shift 2
      ;;
    --dest)
      DEST="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${REPO}" ]]; then
  echo "Missing required argument: --repo <owner/repo>" >&2
  usage
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_INSTALLER="${HOME}/.codex/skills/.system/skill-installer/scripts/install-skill-from-github.py"

if [[ ! -f "${SKILL_INSTALLER}" ]]; then
  echo "Skill installer not found at: ${SKILL_INSTALLER}" >&2
  echo "Ensure Codex system skills are installed on this machine." >&2
  exit 1
fi

SKILL_PATHS=()
for dir in "${SCRIPT_DIR}"/*; do
  [[ -d "${dir}" ]] || continue
  if [[ -f "${dir}/SKILL.md" ]]; then
    SKILL_PATHS+=("skills/$(basename "${dir}")")
  fi
done
IFS=$'\n' SKILL_PATHS=($(printf '%s\n' "${SKILL_PATHS[@]}" | sort))

if [[ ${#SKILL_PATHS[@]} -eq 0 ]]; then
  echo "No installable skills found under ${SCRIPT_DIR}" >&2
  exit 1
fi

CMD=(python3 "${SKILL_INSTALLER}" --repo "${REPO}" --ref "${REF}")
if [[ -n "${DEST}" ]]; then
  CMD+=(--dest "${DEST}")
fi
for path in "${SKILL_PATHS[@]}"; do
  CMD+=(--path "${path}")
done

echo "Repository: ${REPO}"
echo "Ref: ${REF}"
echo "Skills to install: ${#SKILL_PATHS[@]}"
printf ' - %s\n' "${SKILL_PATHS[@]}"

if [[ "${DRY_RUN}" -eq 1 ]]; then
  echo
  echo "Dry run command:"
  printf '%q ' "${CMD[@]}"
  echo
  exit 0
fi

"${CMD[@]}"
echo "Install complete. Restart Codex to pick up new skills."
