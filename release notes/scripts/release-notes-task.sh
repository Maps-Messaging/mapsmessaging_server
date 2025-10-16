#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
# auto-load .env if present
if [ -f ".env" ]; then set -a; source .env; set +a; fi
NODE_BIN="${NODE_BIN:-node}"
exec "$NODE_BIN" tools/changelog/release-notes.js "$@"
