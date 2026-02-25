#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / 'references' / 'output-contract.md'
EXAMPLES = [
    ROOT / 'references' / 'examples' / 'simple-selector-output.md',
    ROOT / 'references' / 'examples' / 'advanced-selector-output.md',
]


def parse_sections(text: str) -> list[str]:
    sections: list[str] = []
    seen: set[str] = set()
    for line in text.splitlines():
        line = line.strip()
        m = re.match(r'^\d+\.\s+`([^`]+)`', line)
        if not m:
            m = re.match(r'^`([^`]+)`$', line)
        if m:
            val = m.group(1).strip()
            if val not in seen:
                seen.add(val)
                sections.append(val)
    return sections


def main() -> int:
    if not CONTRACT.exists():
        print(f'missing contract: {CONTRACT}')
        return 1

    sections = parse_sections(CONTRACT.read_text(encoding='utf-8'))
    if not sections:
        print('no sections parsed from contract')
        return 1

    errors: list[str] = []
    for ex in EXAMPLES:
        if not ex.exists():
            errors.append(f'missing example: {ex}')
            continue
        txt = ex.read_text(encoding='utf-8')
        for section in sections:
            if f'## {section}' not in txt:
                errors.append(f'{ex.name}: missing section {section}')
        for marker in ['positive', 'negative']:
            if marker not in txt.lower():
                errors.append(f'{ex.name}: missing {marker} verification marker')
        if '```bash' not in txt:
            errors.append(f'{ex.name}: missing bash command block')
        if '```mermaid' not in txt:
            errors.append(f'{ex.name}: missing mermaid diagram block')
        if 'skills/' not in txt:
            errors.append(f'{ex.name}: missing repo-relative reference')

    if errors:
        print('selector artifact validation failed:')
        for e in errors:
            print(f'- {e}')
        return 1

    print('selector artifact validation passed.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
