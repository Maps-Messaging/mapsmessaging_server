#!/usr/bin/env bash
# Re-runnable task wrapper for local or CI usage.
# Examples:
#   scripts/release-notes-task.sh --branch development --since-tag --out notes.md
#   scripts/release-notes-task.sh --range v3.3.6..development --jira --out notes.md
#   scripts/release-notes-task.sh --branch main --since-tag --github-release --repo Maps-Messaging/your-repo --tag v3.3.7
#   scripts/release-notes-task.sh --branch main --since-tag --jira --jira-version "3.3.7"
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
NODE_BIN="${NODE_BIN:-node}"
$NODE_BIN tools/changelog/release-notes.js "$@"
