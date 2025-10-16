#!/usr/bin/env bash
set -euo pipefail
BRANCH="${1:-development}"
TITLE="${2:-Release Notes for $BRANCH}"
DATE="$(date +%Y-%m-%d)"
OUT="RELEASE-notes-${BRANCH}-${DATE}.md"
shift 2 || true
EXTRA_ARGS=("$@")
node tools/changelog/release-notes.js --branch "$BRANCH" --title "$TITLE" --out "$OUT" "${EXTRA_ARGS[@]}"
echo "Release notes saved to $OUT"
