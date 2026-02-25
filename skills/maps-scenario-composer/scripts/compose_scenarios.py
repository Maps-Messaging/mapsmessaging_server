#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from collections import defaultdict
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[3]
SKILLS_ROOT = ROOT / 'skills'

MANAGER_FILE_MAP = {
    'NetworkManager': 'NetworkManager.yaml',
    'DestinationManager': 'DestinationManager.yaml',
    'SchemaManager': 'SchemaManager.yaml',
    'AggregatorManager': 'AggregatorManager.yaml',
    'MLModelManager': 'MLModelManager.yaml',
    'AuthManager': 'AuthManager.yaml',
    'SecurityManager': 'SecurityManager.yaml',
    'DeviceManager': 'DeviceManager.yaml',
    'DiscoveryManager': 'DiscoveryManager.yaml',
    'NetworkConnectionManager': 'NetworkConnectionManager.yaml',
    'RestApi': 'RestApi.yaml',
    'TenantManagement': 'TenantManagement.yaml',
    'MessageDaemon': 'MessageDaemon.yaml',
    'LoRaDevice': 'LoRaDevice.yaml',
    'License': 'License.yaml',
    'routing': 'routing.yaml',
    'Routing': 'routing.yaml',
}


def parse_csv(raw: str) -> list[str]:
    return [x.strip() for x in raw.split(',') if x.strip()]


def parse_artifact_arg(raw: str) -> tuple[str, Path]:
    if '=' not in raw:
        raise ValueError(f'invalid --artifact value `{raw}`; expected skill=path')
    skill, path = raw.split('=', 1)
    return skill.strip(), Path(path.strip())


def extract_yaml_blocks(path: Path) -> list[str]:
    if path.suffix.lower() in {'.yaml', '.yml'}:
        return [path.read_text(encoding='utf-8')]

    text = path.read_text(encoding='utf-8')
    blocks = re.findall(r"```yaml\n(.*?)```", text, flags=re.DOTALL | re.IGNORECASE)
    return [b.strip() for b in blocks if b.strip()]


def detect_target_file(obj: Any, skill: str, index: int) -> str:
    if isinstance(obj, dict) and len(obj) == 1:
        top_key = next(iter(obj.keys()))
        if top_key in MANAGER_FILE_MAP:
            return MANAGER_FILE_MAP[top_key]

    if isinstance(obj, dict):
        keys = set(obj.keys())
        if {'routes', 'predefinedServers', 'autoDiscovery'} & keys:
            return 'routing.yaml'

    return f'composed-{skill}-{index}.yaml'


def sanitize_id(raw: str) -> str:
    return re.sub(r'[^a-zA-Z0-9_]+', '_', raw).strip('_') or 'source'


def deep_merge(
    base: Any,
    incoming: Any,
    path: str,
    conflicts: list[str],
    policy: str,
    source_tag: str,
    blocking_conflicts: list[str],
) -> Any:
    if base is None:
        return incoming

    if isinstance(base, dict):
        out = dict(base)
        for k, v in incoming.items():
            child_path = f'{path}.{k}' if path else str(k)
            if k in out:
                existing = out[k]
                if type(existing) != type(v):
                    msg = f'{child_path}: type conflict {type(existing).__name__} -> {type(v).__name__}'
                    if policy == 'strict-fail':
                        conflicts.append(msg + '; blocked')
                        blocking_conflicts.append(msg)
                    elif policy == 'rename':
                        renamed_key = f'{k}__from_{sanitize_id(source_tag)}'
                        out[renamed_key] = v
                        conflicts.append(msg + f'; renamed to {path}.{renamed_key}')
                    else:
                        out[k] = v
                        conflicts.append(msg + '; incoming wins')
                elif isinstance(existing, (dict, list)):
                    out[k] = deep_merge(existing, v, child_path, conflicts, policy, source_tag, blocking_conflicts)
                else:
                    if existing != v:
                        msg = f'{child_path}: value conflict `{existing}` -> `{v}`'
                        if policy == 'strict-fail':
                            conflicts.append(msg + '; blocked')
                            blocking_conflicts.append(msg)
                        elif policy == 'rename':
                            renamed_key = f'{k}__from_{sanitize_id(source_tag)}'
                            out[renamed_key] = v
                            conflicts.append(msg + f'; renamed to {path}.{renamed_key}')
                        else:
                            out[k] = v
                            conflicts.append(msg + '; incoming wins')
            else:
                out[k] = v
        return out

    if isinstance(base, list):
        out = list(base)
        existing = {yaml.safe_dump(item, sort_keys=True).strip() for item in out}
        for item in incoming:
            sig = yaml.safe_dump(item, sort_keys=True).strip()
            if sig not in existing:
                out.append(item)
                existing.add(sig)
            else:
                conflicts.append(f'{path}: duplicate list entry ignored')
        return out

    if base == incoming:
        return base

    msg = f'{path}: value conflict `{base}` -> `{incoming}`'
    if policy == 'strict-fail':
        conflicts.append(msg + '; blocked')
        blocking_conflicts.append(msg)
        return base
    if policy == 'rename':
        conflicts.append(msg + '; rename not possible at scalar root, incoming wins')
        return incoming

    conflicts.append(msg + '; incoming wins')
    return incoming


def collect_artifacts(
    skill_order: list[str],
    artifact_specs: list[tuple[str, Path]],
    policy: str,
) -> tuple[dict[str, Any], dict[str, list[str]], dict[str, list[str]], list[str]]:
    merged_by_file: dict[str, Any] = {}
    trace_by_file: dict[str, list[str]] = defaultdict(list)
    conflicts_by_file: dict[str, list[str]] = defaultdict(list)
    blocking_conflicts: list[str] = []

    order_index = {s: i for i, s in enumerate(skill_order)}
    ordered_specs = sorted(
        artifact_specs,
        key=lambda t: (order_index.get(t[0], 10_000), t[0], str(t[1])),
    )

    for skill, path in ordered_specs:
        blocks = extract_yaml_blocks(path)
        if not blocks:
            conflicts_by_file['global'].append(f'{skill}:{path} produced no YAML blocks')
            continue

        for i, block in enumerate(blocks, start=1):
            try:
                obj = yaml.safe_load(block)
            except Exception as ex:
                conflicts_by_file['global'].append(f'{skill}:{path} block#{i} YAML parse error: {ex}')
                continue

            if obj is None:
                continue

            target = detect_target_file(obj, skill, i)
            merged_existing = merged_by_file.get(target)
            merged_by_file[target] = deep_merge(
                merged_existing,
                obj,
                target,
                conflicts_by_file[target],
                policy,
                skill,
                blocking_conflicts,
            )
            if skill not in trace_by_file[target]:
                trace_by_file[target].append(skill)

    return merged_by_file, trace_by_file, conflicts_by_file, blocking_conflicts


def emit_apply_sequence(target_files: list[str], out_dir: Path | None) -> str:
    lines = ['```bash']
    if out_dir:
        lines.append(f'mkdir -p {out_dir}')
        for f in target_files:
            lines.append(f'cp {out_dir / f} {ROOT / f}')
    else:
        lines.append('# Write merged YAML blocks to target files in repo root')
        for f in target_files:
            lines.append(f'# target: {ROOT / f}')
    lines.append('# restart MAPS runtime once after all files are updated')
    lines.append('```')
    return '\n'.join(lines)


def write_output_files(merged_by_file: dict[str, Any], out_dir: Path) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    for file_name, obj in merged_by_file.items():
        (out_dir / file_name).write_text(
            yaml.safe_dump(obj, sort_keys=False, allow_unicode=False),
            encoding='utf-8',
        )


def main() -> int:
    parser = argparse.ArgumentParser(description='Compose multi-skill scenario deliverable with concrete merged YAML')
    parser.add_argument('--skills', required=True, help='Comma-separated skill names')
    parser.add_argument('--mode', default='hybrid', choices=['additive', 'staged', 'hybrid'])
    parser.add_argument(
        '--conflict-policy',
        default='override',
        choices=['strict-fail', 'override', 'rename'],
        help='Conflict policy for merge collisions',
    )
    parser.add_argument('--artifact', action='append', default=[], help='Per-skill artifact input in form skill=path (repeatable)')
    parser.add_argument('--out-dir', default='', help='Optional output directory for merged YAML files')
    args = parser.parse_args()

    skills = parse_csv(args.skills)
    missing = [s for s in skills if not (SKILLS_ROOT / s / 'SKILL.md').exists()]
    if missing:
        print('Missing skills: ' + ', '.join(missing))
        return 1

    artifact_specs: list[tuple[str, Path]] = []
    for raw in args.artifact:
        try:
            skill, path = parse_artifact_arg(raw)
        except ValueError as ex:
            print(str(ex))
            return 1
        if skill not in skills:
            print(f'Artifact skill `{skill}` not in --skills list')
            return 1
        if not path.exists():
            print(f'Artifact path does not exist: {path}')
            return 1
        artifact_specs.append((skill, path))

    merged_by_file, trace_by_file, conflicts_by_file, blocking_conflicts = collect_artifacts(
        skills, artifact_specs, args.conflict_policy
    )
    if args.conflict_policy == 'strict-fail' and blocking_conflicts:
        print('Composition failed due to strict-fail conflicts:')
        for c in blocking_conflicts:
            print(f'- {c}')
        return 1
    out_dir = Path(args.out_dir) if args.out_dir else None
    if out_dir:
        write_output_files(merged_by_file, out_dir)

    print('# Composed Scenario Deliverable')
    print()
    print('## Composition Matrix')
    print(f'- composition mode: {args.mode}')
    print(f'- conflict policy: {args.conflict_policy}')
    print('- source skills:')
    for s in skills:
        print(f'  - {s}')
    if artifact_specs:
        print('- source artifacts:')
        for skill, path in artifact_specs:
            print(f'  - {skill}: {path}')

    print('\n## Unified Assumptions')
    print('- Shared runtime and config surfaces merged by skill order as precedence.')
    print('- Conflicts resolved deterministically; incoming skill in order overrides scalar conflicts.')
    print('- Lists are merged with deduplication by YAML signature.')

    print('\n## Merged Deployable Entity')
    if not merged_by_file:
        print('No merged YAML artifacts were produced. Provide --artifact skill=path inputs.')
    else:
        for file_name in sorted(merged_by_file.keys()):
            print(f'### {ROOT / file_name}')
            print('```yaml')
            print(yaml.safe_dump(merged_by_file[file_name], sort_keys=False, allow_unicode=False).rstrip())
            print('```')

    print('\n## Unified Apply Sequence')
    print(emit_apply_sequence(sorted(merged_by_file.keys()), out_dir))

    print('\n## Integrated Verification')
    print('- Startup diagnostics and listener checks for expected endpoints.')
    print('- Per-skill verification paths preserved from source scenarios.')
    print('- Cross-scenario checks for staged flows and dependency boundaries.')

    print('\n## Failure Domain and Rollback')
    print('- Rollback by restoring target manager files from backups before compose apply.')
    print('- Isolate failures by merged file and source-skill ownership.')

    print('\n## Traceability Map')
    if not trace_by_file:
        print('- No trace entries (no merged artifacts).')
    else:
        for file_name in sorted(trace_by_file.keys()):
            src = ', '.join(trace_by_file[file_name])
            print(f'- {ROOT / file_name} <- {src}')

    print('\n## Conflict Resolution Log')
    has_conflicts = False
    for file_name in sorted(conflicts_by_file.keys()):
        entries = [e for e in conflicts_by_file[file_name] if e]
        if not entries:
            continue
        has_conflicts = True
        print(f'- {file_name}:')
        for e in entries:
            print(f'  - {e}')
    if not has_conflicts:
        print('- No merge conflicts detected.')

    print('\n## Scenario Metrics and Dashboard')
    print('- Combined metrics across included skills plus per-skill panes.')
    print('- Grafana and MAPS-hosted combined dashboard definitions.')

    print('\n## C4 Architecture Diagram')
    print('```mermaid')
    print('graph LR')
    print('  A[Input Scenario Artifacts] --> B[Scenario Composer Merge] --> C[Unified MAPS Deployable]')
    print('```')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
