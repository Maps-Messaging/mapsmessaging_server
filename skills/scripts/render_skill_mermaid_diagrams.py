#!/usr/bin/env python3
"""
Render Mermaid diagrams for all skills.

Inputs:
- .mmd files under skills/
- ```mermaid fenced blocks inside .md files under skills/

Output:
- SVG files under skills/rendered-mermaid/ mirroring source layout.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


MERMAID_FENCE_RE = re.compile(r"```mermaid\s*\n(.*?)\n```", re.DOTALL)
NODE_LABEL_RE = re.compile(r"(\b[A-Za-z0-9_]+)\[([^\[\]\n\"]+)\]")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Render Mermaid diagrams for skills.")
    parser.add_argument(
        "--skills-root",
        default="skills",
        help="Path to skills directory (default: skills)",
    )
    parser.add_argument(
        "--out-dir",
        default="skills/rendered-mermaid",
        help="Output directory for rendered SVGs (default: skills/rendered-mermaid)",
    )
    parser.add_argument(
        "--include-md-fences",
        action="store_true",
        help="Include mermaid fences inside markdown files.",
    )
    parser.add_argument(
        "--puppeteer-config",
        default="",
        help="Optional Puppeteer config JSON path passed to mmdc (-p).",
    )
    return parser.parse_args()


def ensure_mmdc() -> str:
    mmdc = shutil.which("mmdc")
    if mmdc:
        return mmdc
    print(
        "mmdc not found on PATH. Install Mermaid CLI, e.g. `npm i -g @mermaid-js/mermaid-cli`.",
        file=sys.stderr,
    )
    raise SystemExit(2)


def iter_mmd_files(skills_root: Path) -> list[Path]:
    return sorted(
        path
        for path in skills_root.rglob("*.mmd")
        if "rendered-mermaid" not in path.parts
    )


def iter_md_mermaid_fences(skills_root: Path) -> list[tuple[Path, int, str]]:
    results: list[tuple[Path, int, str]] = []
    for md in sorted(skills_root.rglob("*.md")):
        if "rendered-mermaid" in md.parts:
            continue
        text = md.read_text(encoding="utf-8")
        matches = list(MERMAID_FENCE_RE.finditer(text))
        for idx, match in enumerate(matches, start=1):
            results.append((md, idx, match.group(1).strip() + "\n"))
    return results


def sanitize_mermaid(diagram: str) -> str:
    """
    Normalize node labels for Mermaid CLI compatibility.

    Converts node labels from `A[label]` to `A["label"]` and escapes angle
    brackets in label text to avoid parser errors on path-like labels such as
    `/geo/intermediate/<geohash>`.
    """

    def repl(match: re.Match[str]) -> str:
        node_id = match.group(1)
        label = match.group(2)
        safe = label.replace("\\", "\\\\").replace('"', '\\"')
        safe = safe.replace("<", "&lt;").replace(">", "&gt;")
        return f'{node_id}["{safe}"]'

    return NODE_LABEL_RE.sub(repl, diagram)


def render(mmdc: str, src: Path, dst_svg: Path, puppeteer_cfg: Path | None) -> None:
    dst_svg.parent.mkdir(parents=True, exist_ok=True)
    cmd = [mmdc, "-i", str(src), "-o", str(dst_svg), "-b", "transparent"]
    if puppeteer_cfg:
        cmd.extend(["-p", str(puppeteer_cfg)])
    subprocess.run(cmd, check=True)


def main() -> int:
    args = parse_args()
    skills_root = Path(args.skills_root).resolve()
    out_dir = Path(args.out_dir).resolve()

    if not skills_root.exists():
        print(f"Skills root not found: {skills_root}", file=sys.stderr)
        return 2

    mmdc = ensure_mmdc()
    rendered = 0
    failed = 0
    auto_puppeteer_cfg: tempfile.NamedTemporaryFile[str] | None = None
    puppeteer_cfg: Path | None = None

    if args.puppeteer_config:
        puppeteer_cfg = Path(args.puppeteer_config).resolve()
    else:
        auto_puppeteer_cfg = tempfile.NamedTemporaryFile(
            mode="w", suffix=".json", delete=False
        )
        json.dump(
            {"args": ["--no-sandbox", "--disable-setuid-sandbox"]},
            auto_puppeteer_cfg,
        )
        auto_puppeteer_cfg.flush()
        puppeteer_cfg = Path(auto_puppeteer_cfg.name)

    for src in iter_mmd_files(skills_root):
        rel = src.relative_to(skills_root)
        dst_svg = out_dir / rel.with_suffix(".svg")
        try:
            render(mmdc, src, dst_svg, puppeteer_cfg)
            rendered += 1
        except subprocess.CalledProcessError as exc:
            failed += 1
            print(f"Failed rendering {src}: {exc}", file=sys.stderr)

    if args.include_md_fences:
        for md_file, idx, diagram in iter_md_mermaid_fences(skills_root):
            rel = md_file.relative_to(skills_root)
            tmp_mmd = out_dir / rel.parent / f"{md_file.stem}.mermaid-{idx}.mmd"
            tmp_mmd.parent.mkdir(parents=True, exist_ok=True)
            tmp_mmd.write_text(sanitize_mermaid(diagram), encoding="utf-8")
            dst_svg = tmp_mmd.with_suffix(".svg")
            try:
                render(mmdc, tmp_mmd, dst_svg, puppeteer_cfg)
                rendered += 1
            except subprocess.CalledProcessError as exc:
                failed += 1
                print(f"Failed rendering {md_file}#{idx}: {exc}", file=sys.stderr)

    if auto_puppeteer_cfg is not None:
        try:
            Path(auto_puppeteer_cfg.name).unlink(missing_ok=True)
        except OSError:
            pass

    print(f"Rendered {rendered} Mermaid diagram(s) into {out_dir}; failures={failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
