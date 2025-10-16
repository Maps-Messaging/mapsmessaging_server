# MAPS Release Notes Tool

Reusable CLI for generating and publishing release notes from git history.

---

## 📦 Folder Structure

```
release-notes/
├── tools/
│   └── changelog/
│       ├── release-notes.js
│       ├── generate-release-notes.sh
├── scripts/
│   └── release-notes-task.sh
├── .env.example
├── package.json
└── buildkite-step-release-notes-task.yml
```

---

## ⚙️ Requirements

- **Node.js 18+** (no npm installs needed; uses only built-in modules)
- **Git** available in PATH
- **Buildkite** agent for CI integration

---

## 🔐 Environment Variables

All tokens and secrets should be stored in **Buildkite’s environment**, not in a checked-in `.env`.

| Variable | Description | Required |
|-----------|--------------|-----------|
| `JIRA_BASE` | Jira base URL (e.g. `https://mapsmessaging.atlassian.net`) | For Jira integration |
| `JIRA_USER` | Jira account for API access | ✅ |
| `JIRA_TOKEN` | Jira API token | ✅ |
| `JIRA_PROJECT_ID` | Project ID number (not key) | For Jira version publishing |
| `JIRA_KEY_PREFIX` | Key prefix (default `MSG`) | optional |
| `GITHUB_TOKEN` | GitHub Personal Access Token | For publishing GitHub Releases |

> 🧠 Define these in Buildkite → *Pipeline Settings → Environment Variables* or your secrets store.

---

## 🧩 Usage

### 1️⃣ Generate preview notes (safe to rerun)

```bash
./release-notes/scripts/release-notes-task.sh --branch development --since-tag --out notes.md
```

- Compares commits since the last tag on `development`
- Outputs `notes.md` in the current directory

---

### 2️⃣ Enrich with Jira issues

```bash
export JIRA_BASE=https://mapsmessaging.atlassian.net
export JIRA_USER=bot@mapsmessaging.io
export JIRA_TOKEN=***
export JIRA_PROJECT_ID=12345

./release-notes/scripts/release-notes-task.sh --branch development --since-tag --jira --out notes.md
```

- Adds linked `MSG-####` issues, summaries, and statuses

---

### 3️⃣ Publish a GitHub Release

```bash
export GITHUB_TOKEN=ghp_XXXX
./release-notes/scripts/release-notes-task.sh   --branch main --since-tag   --github-release --repo Maps-Messaging/maps-server   --tag v3.3.7   --out release-notes-v3.3.7.md
```

---

### 4️⃣ Create a Jira Version (optional)

```bash
./release-notes/scripts/release-notes-task.sh   --branch main --since-tag   --jira --jira-version "3.3.7"   --out release-notes-v3.3.7.md
```

---

## 🔁 Re-runnable task

You can run this script multiple times before finalising a release—ideal for fixing commit messages or tagging later.

---

## 🧱 Buildkite Example

Include `buildkite-step-release-notes-task.yml`:

```yaml
steps:
  - label: ":notebook: Generate release notes"
    key: "release-notes"
    agents:
      queue: "java_build_queue"
    commands:
      - "git fetch --tags --all"
      - "./release-notes/scripts/release-notes-task.sh --branch ${BUILDKITE_BRANCH:-development} --since-tag --jira --out release-notes-${BUILDKITE_BRANCH:-development}.md"
    artifact_paths:
      - "release-notes-*.md"
```

---

## 📘 Notes

- Detects Conventional Commit types (`feat`, `fix`, `perf`, etc.).
- Flags `BREAKING CHANGE` and groups sections.
- Extracts and hyperlinks Jira issues (e.g., `MSG-1234`).
- Safe to run locally or in CI.
- Compatible with your Maps release workflow (`development → main`).

---

_MapsMessaging B.V. — Automated release documentation for MAPS Server_
