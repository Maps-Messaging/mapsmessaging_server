# Development/Main Release Flow Policy

## 1. Purpose

This document defines the development-to-release workflow, branching model, pull request rules, tagging, build types (snapshot vs release), and maintenance tasks to keep the repository clean and enforceable.

This is **not** “GitHub Flow” in the strict sense (which typically merges straight to `main`). We use a controlled release workflow to support predictable releases and selective patching without treating `main` as a perpetual integration branch.

Only **tagged commits** are considered releases.

---

## 2. Branch Model

### 2.1 main (Protected)

- Represents the latest production release line history (from the first shipped `X.Y.0` onward for that line).
- Receives changes only from `development` via Pull Request.
- Linear history enforced (**Rebase and Merge** only).
- Every production release from `main` is tagged `vX.Y.Z`.

**Important:** `main` is *not* used as the integration branch for patch trains on older `release-*` branches. Patch fixes must be forward-ported to `development` so they appear in a future `main` release.

### 2.2 development (Protected)

- Integration branch for the **next** production release cycle.
- Not production-ready.
- All feature work targets `development` (via PR).
- Any fix applied to `release-*` must be forward-ported (cherry-picked) into `development`.

### 2.3 release-X.Y.Z (Protected, then Frozen)

- Patch-train branch for a specific release version `X.Y.Z`.
- Created from the previous patch release branch (e.g. `release-X.Y.1` from `release-X.Y.0`) when a patch train is started.
- Hotfixes for that patch release land here via PR.
- After tagging the patch release, the branch is **frozen** (locked/read-only).

### 2.4 Short-Lived Branches

All work happens on short-lived branches created from:

- `development` (features and improvements), or
- `release-X.Y.Z` (hotfixes for that patch line)

Naming conventions:

- `feature/<ticket>-<short-description>`
- `bugfix/<ticket>-<short-description>`
- `hotfix/<ticket>-<short-description>`

Short-lived branches must be deleted after merge.

---

## 3. Pull Request Rules (All Protected Branches)

All merges into protected branches must:

1. Be submitted via Pull Request.
2. Pass all required CI status checks.
3. Be up to date with the target branch before merge (no “green on an old base”).
4. Receive required approvals.
5. Use **Rebase and Merge** as the only allowed merge strategy (linear history).
6. Not bypass branch protections (applies to administrators).

Direct pushes, force pushes, and deletion are disabled for protected branches.

---

## 4. Tags and Releases

### 4.1 Tagging

- Production releases are tagged as `vX.Y.Z`.
- Tags are the source of truth for “what shipped”.
- Tags must be protected from deletion/mutation.

### 4.2 Release from main

1. Merge `development` → `main` via PR.
2. Tag the resulting commit on `main` as `vX.Y.Z`.
3. Build and publish release artifacts from that tag.

### 4.3 Patch releases (release-X.Y.Z)

1. Create the next patch train branch `release-X.Y.(Z+1)` from `release-X.Y.Z` **only when patch support is needed**.
2. Merge hotfix PRs into `release-X.Y.(Z+1)`.
3. Tag the released commit on that branch as `vX.Y.(Z+1)`.
4. Freeze the branch after release.

**Support burden note:** Patch trains are created only when support is required. If support is rare, this keeps branch sprawl down.

---

## 5. Snapshot Builds

Snapshot builds exist to validate integration continuously without implying a release.

### 5.1 What counts as a snapshot

- Any build produced from `development` (and optionally feature branches) that is **not** a release tag.
- Snapshot versioning must be clearly non-release, e.g.:
    - `X.Y.Z-SNAPSHOT`
    - or `X.Y.Z-dev.<buildNumber>` (project preference)

### 5.2 Where snapshots come from

- Snapshots are produced automatically from `development` on merge.
- Optional: snapshots may be produced from PR branches for validation, but must be published to a clearly separated snapshot repository/channel.

### 5.3 Snapshot repositories / retention

- Snapshot artifacts must be stored separately from release artifacts.
- Retention policy should prevent unbounded growth (e.g., keep last N snapshots per branch or last N days).

---

## 6. Hotfix Forward-Port Rule (Mandatory)

If a fix is merged into a `release-X.Y.Z` branch:

1. The fix must land in the relevant `release-*` branch first (the supported line).
2. The fix must then be **cherry-picked into `development`**.
3. The `development` PR must reference the original fix PR/commit.
4. If the fix is intentionally not applicable to `development`, the PR must explicitly state why.

This prevents “fixed in prod, re-broken in the next release” regressions.

---

## 7. GitHub Enforcement (Rulesets / Branch Protections)

Branch protections (prefer GitHub Rulesets) must enforce:

- PR required before merging
- Required approvals
- Required status checks
- “Require branches to be up to date”
- Linear history (Rebase and Merge)
- No force-push
- No direct pushes
- Protections apply to administrators

Tags matching `v*` should be protected from deletion/mutation.

---

## 8. Repository Maintenance Tasks (Keep It Clean and Enforceable)

### 8.1 Enforce branch naming conventions

Add a CI check (GitHub Action) on PRs that validates:

- Source branch name matches allowed patterns (`feature/*`, `bugfix/*`, `hotfix/*`)
- Target branch is valid (`development` or `release-*` depending on intent)

### 8.2 Enforce folder/subfolder structure

Add a CI check (GitHub Action) to validate:

- New modules/packages must live under approved top-level folders (project-specific list).
- No ad-hoc top-level directories without explicit approval.
- Optional: forbid moving/renaming top-level directories except via approved “repo restructure” PR.

This keeps the repo from turning into a junk drawer.

### 8.3 Stale branch detection and cleanup

Automate branch hygiene:

- Nightly/weekly job to:
    - Mark branches stale after N days with no commits (label, comment, or create an issue).
    - Notify branch owners (if available via commit metadata or CODEOWNERS).
    - Auto-delete stale **short-lived** branches after an additional grace period.
- Never auto-delete `main`, `development`, or `release-*` branches.

### 8.4 Snapshot artifact retention

Automate cleanup of snapshot artifacts per retention policy to avoid storage creep.

---

## 9. Branch Lifecycle Summary
| Branch Type     | Direct Push | PR Required | Frozen After Release | Linear History |
|-----------------|------------|------------|----------------------|----------------|
| main            | No         | Yes        | No                   | Yes            |
| development     | No         | Yes        | No                   | Yes            |
| release-X.Y.Z   | No         | Yes        | Yes                  | Yes            |
| feature/*       | Yes        | No         | Deleted After Merge  | N/A            |
| bugfix/*        | Yes        | No         | Deleted After Merge  | N/A            |
| hotfix/*        | Yes        | No         | Deleted After Merge  | N/A            |
---

## 10. Deletion Policy

- Feature/bugfix/hotfix branches are deleted after merge.
- Release branches are not deleted; they are frozen after the associated release.
- `main` and `development` are permanent.

---

## 11. Exceptions

Emergency changes require:

- Documented justification in the PR.
- Post-merge review.
- Immediate corrective PR if needed.

No history rewrites are permitted on protected branches.
