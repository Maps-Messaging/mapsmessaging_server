# Git Branching and Release Policy

## 1. Purpose

This document defines the Git branching model, pull request workflow,
release process, and enforcement rules for the repository.\
The goal is to ensure traceability, stability, controlled releases, and
predictable development cycles.

Only tagged releases are considered production releases.

------------------------------------------------------------------------

## 2. Branch Types

### 2.1 main (Protected)

-   Contains the history starting from the first `X.Y.0` release
    forward.
-   Represents the integration history of production releases derived
    from `development`.
-   No direct commits allowed.
-   Merges only occur via Pull Request.
-   Linear history enforced (Rebase and Merge only).
-   Only tagged commits represent official releases.

### 2.2 development (Protected)

-   Integration branch for the next production release cycle.
-   Not production-ready.
-   All new feature work targets this branch.
-   Forward-ported hotfixes from release branches must be applied here.
-   No direct commits allowed.
-   Merges only occur via Pull Request.
-   Linear history enforced (Rebase and Merge only).

### 2.3 release-X.Y.Z (Protected, then Frozen)

-   Patch train branch for release version `X.Y.Z`.
-   Created from the previous release branch or from `main` as
    appropriate.
-   All fixes for that specific patch version are merged here via Pull
    Request.
-   After the release is tagged, the branch is frozen (no further
    changes).
-   Linear history enforced (Rebase and Merge only).

### 2.4 Short-Lived Branches

All work must be performed on short-lived branches created from either:

-   `development` (for features and improvements)
-   `release-X.Y.Z` (for hotfixes)

Naming conventions:

-   `feature/<short-description>`
-   `bugfix/<short-description>`
-   `hotfix/<short-description>`

Short-lived branches must be deleted after merge.

------------------------------------------------------------------------

## 3. Pull Request Rules

All merges into protected branches must:

1.  Be submitted via Pull Request.
2.  Pass all required CI status checks.
3.  Be up to date with the target branch before merge.
4.  Receive required approvals.
5.  Use **Rebase and Merge** as the only allowed merge strategy.
6.  Maintain linear history.
7.  Not bypass branch protections (applies to administrators).

Direct pushes, force pushes, and branch deletion are disabled for
protected branches.

------------------------------------------------------------------------

## 4. Release Process

### 4.1 Minor / Major Release

1.  Complete development work in `development`.
2.  Open Pull Request from `development` → `main`.
3.  Merge using Rebase and Merge.
4.  Tag the release as `vX.Y.Z` on `main`.
5.  Create new `release-X.Y.Z` branch if required.
6.  Continue next development cycle on `development`.

### 4.2 Patch Release (Hotfix Train)

1.  Create `release-X.Y.(Z+1)` from the previous release branch.
2.  Merge fixes into this branch via Pull Request.
3.  Tag the release as `vX.Y.(Z+1)` on this branch.
4.  Freeze the branch after release.

------------------------------------------------------------------------

## 5. Hotfix Forward-Port Rule (Mandatory)

If a fix is applied to a `release-X.Y.Z` branch:

1.  The fix must first land in the appropriate release branch.
2.  The fix must then be cherry-picked into `development`.
3.  The Pull Request into `development` must reference the original fix.
4.  This forward-port must occur within the same development cycle.

This ensures all fixes eventually appear in future releases and in
`main`.

------------------------------------------------------------------------

## 6. Source of Truth

-   Production state is defined by **tags**, not branches.
-   Only tagged commits represent released versions.
-   Branches represent working lines, not deployable artifacts.

------------------------------------------------------------------------

## 7. Enforcement via GitHub

Branch protections / rulesets must enforce:

-   Required Pull Requests
-   Required status checks
-   Required approvals
-   Required linear history
-   No force pushes
-   No direct commits
-   Protection applies to administrators

Releases and tags must be protected from deletion or modification.

------------------------------------------------------------------------

## 8. AI-Assisted Review Policy (PR-Level)

Automated review tools (e.g., AI code reviewers) may be used to:

-   Enforce coding standards
-   Validate test coverage
-   Detect security concerns
-   Validate forward-port compliance
-   Ensure PR templates are completed

AI checks may be configured as required status checks before merge.

------------------------------------------------------------------------

## 9. Branch Lifecycle Summary


| Branch Type     | Direct Push | PR Required | Frozen After Release | Linear History |
|-----------------|------------|------------|----------------------|----------------|
| main            | No         | Yes        | No                   | Yes            |
| development     | No         | Yes        | No                   | Yes            |
| release-X.Y.Z   | No         | Yes        | Yes                  | Yes            |
| feature/*       | Yes        | Yes        | Deleted After Merge  | N/A            |


------------------------------------------------------------------------

## 10. Deletion Policy

-   Feature, bugfix, and hotfix branches are deleted after merge.
-   Release branches are never deleted but are frozen after release.
-   main and development are permanent branches.

------------------------------------------------------------------------

## 11. Exceptions

Emergency changes require:

-   Documented justification.
-   Post-merge review.
-   Immediate corrective PR if required.

No history rewrites are permitted on protected branches.
