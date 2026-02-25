# Skills Install Guide

This guide explains how to package and install MAPS skills into Codex, Claude, and other agent runtimes.

## What to distribute

Distribute skill folders from `skills/` (each folder should include `SKILL.md`, `references/`, and `scripts/` as needed).

Examples:
- `skills/mapsmessaging-config-builder`
- `skills/maps-runtime-diagnostics`
- `skills/maps-aggregator-config-engineer`

## Install into Codex

Codex reads skills from `$CODEX_HOME/skills` (often `~/.codex/skills`).

Install selected skills:

```bash
mkdir -p ~/.codex/skills
cp -R skills/mapsmessaging-config-builder ~/.codex/skills/
cp -R skills/maps-runtime-diagnostics ~/.codex/skills/
```

Install all skills:

```bash
mkdir -p ~/.codex/skills
cp -R skills/* ~/.codex/skills/
```

Open a new Codex session and invoke by name, for example:
- `$mapsmessaging-config-builder`
- `$maps-runtime-diagnostics`

## Install into Claude

For Claude project agents:

1. Copy skill folders into your project (for example under `skills/` or `.claude/skills/`).
2. Register them in `AGENTS.md` with:
- skill name
- short description
- path to the skill `SKILL.md`
3. Trigger by explicit skill mention or task intent.

## Install into other LLM agents

If the platform has no native skill system, use one of these patterns:

1. Prompt-pack pattern:
- load `SKILL.md` plus required reference files into system/developer context.

2. Tool wrapper pattern:
- expose `scripts/` as callable tools for the agent.

3. Retrieval pattern:
- index `skills/` content and inject relevant snippets into runtime context.

## Runtime prerequisites (for MAPS smoke/runtime scripts)

- Docker
- `bash`
- `mosquitto_pub` and `mosquitto_sub`
- `python3` (optional helper scripts may also need `PyYAML`)
- `rg` (recommended)

## Packaging recommendation

For internal distribution:

1. Publish the repo branch/tag containing skill updates.
2. Keep skill docs and scripts versioned with source.
3. Prefer repo-relative paths in docs and references.
4. Include a short changelog entry for added/updated skills.
