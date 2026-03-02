#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ALLOWED_EXTS = {'.json', '.csv'}
BANNED_EXTS = {'.ser', '.bin', '.obj'}
ROOT = Path(__file__).resolve().parents[3]


def check_portable_extensions() -> list[str]:
    errors: list[str] = []
    sample_artifacts = [
        ROOT / 'skills' / 'maps-ml-model-lifecycle-playbook' / 'references' / 'examples' / 'model-artifact-sample.json',
        ROOT / 'skills' / 'maps-ml-model-lifecycle-playbook' / 'references' / 'examples' / 'model-metrics-sample.csv',
    ]
    for p in sample_artifacts:
        if p.suffix.lower() not in ALLOWED_EXTS:
            errors.append(f'non-portable extension: {p}')

    for banned in BANNED_EXTS:
        if list((ROOT / 'skills' / 'maps-ml-model-lifecycle-playbook').rglob(f'*{banned}')):
            errors.append(f'found banned artifact extension {banned} under skill tree')

    return errors


def check_no_java_serialization_markers() -> list[str]:
    errors: list[str] = []
    for p in (ROOT / 'skills' / 'maps-ml-model-lifecycle-playbook').rglob('*.md'):
        txt = p.read_text(encoding='utf-8')
        if 'ObjectInputStream' in txt or 'ObjectOutputStream' in txt:
            errors.append(f'java serialization marker found in {p}')
    return errors


def main() -> int:
    errors = []
    errors.extend(check_portable_extensions())
    errors.extend(check_no_java_serialization_markers())

    if errors:
        print('ml lifecycle vectors failed:')
        for e in errors:
            print(f'- {e}')
        return 1

    print('ml lifecycle vector smoke PASS')
    print('portable formats: json/csv; no Java serialization markers detected')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
