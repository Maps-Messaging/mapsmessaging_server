#!/usr/bin/env python3
"""Smoke tests for MAPS skill definitions and generated artifact outputs."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parents[2]
SKILLS_DIR = REPO_ROOT / "skills"
FIXTURES_DIR = SKILLS_DIR / "smoke" / "fixtures"
DEFAULT_QUICK_VALIDATE = Path(
    "$CODEX_HOME/skills/.system/skill-creator/scripts/quick_validate.py"
)


def iter_skill_dirs() -> Iterable[Path]:
    for path in sorted(SKILLS_DIR.iterdir()):
        if not path.is_dir():
            continue
        if path.name in {"smoke"}:
            continue
        if (path / "SKILL.md").exists():
            yield path


def parse_skill_filter(raw: str | None) -> set[str]:
    if not raw:
        return set()
    items = [item.strip() for item in raw.split(",")]
    return {item for item in items if item}


def parse_contract_sections(contract_text: str) -> list[str]:
    sections: list[str] = []
    seen: set[str] = set()
    for raw_line in contract_text.splitlines():
        line = raw_line.strip()
        numbered = re.match(r"^\d+\.\s+`([^`]+)`", line)
        bare = re.match(r"^`([^`]+)`$", line)
        value = None
        if numbered:
            value = numbered.group(1).strip()
        elif bare:
            value = bare.group(1).strip()
        if value and value not in seen:
            seen.add(value)
            sections.append(value)
    return sections


def build_fixture(skill_name: str, sections: list[str]) -> str:
    lines: list[str] = [
        f"# {skill_name} Artifact Fixture",
        "",
        "Synthetic output used by smoke tests to verify output-contract coverage.",
        "",
    ]
    for section in sections:
        lines.extend(
            [
                f"## {section}",
                f"Smoke placeholder for `{section}`.",
                "",
            ]
        )
        lowered = section.lower()
        if any(
            token in lowered
            for token in (
                "apply",
                "diagnostic",
                "verification",
                "test commands",
                "preflight",
                "gate results",
                "manifest",
                "deployable",
            )
        ):
            lines.extend(
                [
                    "```bash",
                    "echo smoke-check",
                    "```",
                    "",
                ]
            )

    lines.extend(
        [
            "## Absolute Path Example",
            f"`{REPO_ROOT / 'NetworkManager.yaml'}`",
            "",
            "## Mermaid C4 Placeholder",
            "```mermaid",
            "graph LR",
            '  A["Source"] --> B["MAPS"] --> C["Destination"]',
            "```",
            "",
        ]
    )
    return "\n".join(lines)


def check_contains(text: str, expected: str, errors: list[str], label: str) -> None:
    if expected not in text:
        errors.append(f"{label}: missing `{expected}`")


def validate_orchestrator_fixture(skill_dir: Path, errors: list[str]) -> None:
    fixture = FIXTURES_DIR / skill_dir.name / "artifact.md"
    if not fixture.exists():
        errors.append(f"{skill_dir.name}: missing fixture `{fixture}`")
        return
    text = fixture.read_text(encoding="utf-8")
    for expected in (
        "## Quick Start Template",
        "## Advanced Template",
        "mapsmessaging-config-builder",
        "maps-ml-stream-configurator",
    ):
        check_contains(text, expected, errors, skill_dir.name)


def validate_artifact_fixture(skill_dir: Path, errors: list[str]) -> None:
    contract = skill_dir / "references" / "output-contract.md"
    if not contract.exists():
        if skill_dir.name == "maps-skill-suite-orchestrator":
            validate_orchestrator_fixture(skill_dir, errors)
        return

    sections = parse_contract_sections(contract.read_text(encoding="utf-8"))
    if not sections:
        errors.append(f"{skill_dir.name}: no sections parsed from `{contract}`")
        return

    fixture = FIXTURES_DIR / skill_dir.name / "artifact.md"
    if not fixture.exists():
        errors.append(f"{skill_dir.name}: missing fixture `{fixture}`")
        return

    fixture_text = fixture.read_text(encoding="utf-8")
    for section in sections:
        check_contains(fixture_text, f"## {section}", errors, skill_dir.name)

    if "```bash" not in fixture_text:
        errors.append(f"{skill_dir.name}: fixture missing bash command block")
    if "```mermaid" not in fixture_text:
        errors.append(f"{skill_dir.name}: fixture missing mermaid block")
    if str(REPO_ROOT) not in fixture_text:
        errors.append(f"{skill_dir.name}: fixture missing absolute repo path")


def ensure_skill_definition(skill_dir: Path, quick_validate: Path, errors: list[str]) -> None:
    for required in ("SKILL.md", "agents/openai.yaml"):
        if not (skill_dir / required).exists():
            errors.append(f"{skill_dir.name}: missing required file `{required}`")

    cmd = [sys.executable, str(quick_validate), str(skill_dir)]
    result = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        output = (result.stdout + "\n" + result.stderr).strip()
        errors.append(f"{skill_dir.name}: quick_validate failed: {output}")


def bootstrap_fixtures(skill_dirs: list[Path]) -> None:
    for skill_dir in skill_dirs:
        target = FIXTURES_DIR / skill_dir.name / "artifact.md"
        target.parent.mkdir(parents=True, exist_ok=True)

        contract = skill_dir / "references" / "output-contract.md"
        if contract.exists():
            sections = parse_contract_sections(contract.read_text(encoding="utf-8"))
            target.write_text(build_fixture(skill_dir.name, sections), encoding="utf-8")
            continue

        if skill_dir.name == "maps-skill-suite-orchestrator":
            target.write_text(
                "\n".join(
                    [
                        "# maps-skill-suite-orchestrator Artifact Fixture",
                        "",
                        "## Quick Start Template",
                        "Use <skill-name> in Simple Local Default mode.",
                        "",
                        "## Advanced Template",
                        "Use <skill-name> in Advanced Combination Matrix mode.",
                        "",
                        "## Indexed Skills",
                        "- mapsmessaging-config-builder",
                        "- maps-runtime-diagnostics",
                        "- maps-protocol-bridge-tester",
                        "- maps-satellite-gateway-config",
                        "- maps-schema-pipeline-builder",
                        "- maps-transform-chain-designer",
                        "- maps-aggregator-config-engineer",
                        "- maps-canbus-ingestion-builder",
                        "- maps-deployment-packager",
                        "- maps-release-readiness-checker",
                        "- maps-ml-stream-configurator",
                        "",
                        "## Absolute Path Example",
                        f"`{REPO_ROOT / 'skills' / 'maps-skill-suite-orchestrator' / 'SKILL.md'}`",
                        "",
                        "```bash",
                        "echo smoke-check",
                        "```",
                        "",
                        "```mermaid",
                        "graph LR",
                        '  A["Request"] --> B["Orchestrator"] --> C["Target Skill"]',
                        "```",
                        "",
                    ]
                ),
                encoding="utf-8",
            )


def main() -> int:
    parser = argparse.ArgumentParser(description="Smoke test MAPS skills and artifacts")
    parser.add_argument(
        "--quick-validate",
        type=Path,
        default=DEFAULT_QUICK_VALIDATE,
        help="Path to skill-creator quick_validate.py",
    )
    parser.add_argument(
        "--bootstrap-fixtures",
        action="store_true",
        help="Generate or refresh fixture outputs before validation",
    )
    parser.add_argument(
        "--skills",
        default="",
        help="Comma-separated skill names to validate (default: all skills)",
    )
    args = parser.parse_args()

    requested = parse_skill_filter(args.skills)
    skills = list(iter_skill_dirs())
    if requested:
        skills = [s for s in skills if s.name in requested]
        missing = sorted(requested - {s.name for s in skills})
        if missing:
            print(
                "Requested skills not found: " + ", ".join(missing),
                file=sys.stderr,
            )
            return 1
    if not skills:
        print("No skill directories found.", file=sys.stderr)
        return 1

    if args.bootstrap_fixtures:
        bootstrap_fixtures(skills)

    errors: list[str] = []
    for skill_dir in skills:
        ensure_skill_definition(skill_dir, args.quick_validate, errors)
        validate_artifact_fixture(skill_dir, errors)

    if errors:
        print("Skill smoke test failed:")
        for err in errors:
            print(f"- {err}")
        return 1

    print(f"Skill smoke test passed for {len(skills)} skills.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
