#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

EXCLUDE_DIRS = {
    "__pycache__",
    ".pytest_cache",
    ".mypy_cache",
    ".ruff_cache",
}

EXCLUDE_SUFFIXES = {
    ".pyc",
    ".pyo",
}

EXCLUDE_FILES = {
    ".DS_Store",
}


def should_include(path: Path) -> bool:
    if any(part in EXCLUDE_DIRS for part in path.parts):
        return False
    if path.name in EXCLUDE_FILES:
        return False
    if path.suffix in EXCLUDE_SUFFIXES:
        return False
    if path.suffix == ".zip":
        return False
    return True


def find_skill_dirs(skills_dir: Path) -> list[Path]:
    dirs: list[Path] = []
    for child in sorted(skills_dir.iterdir()):
        if not child.is_dir():
            continue
        if child.name.startswith("."):
            continue
        if (child / "SKILL.md").is_file():
            dirs.append(child)
    return dirs


def package_skill(skill_dir: Path, out_dir: Path) -> Path:
    zip_path = out_dir / f"{skill_dir.name}.zip"
    if zip_path.exists():
        zip_path.unlink()

    with ZipFile(zip_path, mode="w", compression=ZIP_DEFLATED) as zf:
        for p in sorted(skill_dir.rglob("*")):
            if p.is_dir():
                continue
            rel = p.relative_to(skill_dir.parent)
            if not should_include(rel):
                continue
            zf.write(p, arcname=str(rel))
    return zip_path


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Create one installable zip artifact per skill directory."
    )
    parser.add_argument(
        "--skills-dir",
        default=str(Path(__file__).resolve().parent),
        help="Directory containing skill folders (default: script directory)",
    )
    parser.add_argument(
        "--output-dir",
        default=None,
        help="Directory for zip artifacts (default: --skills-dir)",
    )
    args = parser.parse_args()

    skills_dir = Path(args.skills_dir).resolve()
    out_dir = Path(args.output_dir).resolve() if args.output_dir else skills_dir

    if not skills_dir.is_dir():
        raise SystemExit(f"skills dir does not exist: {skills_dir}")

    out_dir.mkdir(parents=True, exist_ok=True)
    skill_dirs = find_skill_dirs(skills_dir)
    if not skill_dirs:
        raise SystemExit(f"no skill folders found under: {skills_dir}")

    created: list[Path] = []
    for skill_dir in skill_dirs:
        created.append(package_skill(skill_dir, out_dir))

    print(f"Packaged {len(created)} skills into: {out_dir}")
    for item in created:
        print(f"- {item.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
