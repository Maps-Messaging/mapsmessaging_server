#!/usr/bin/env python3
"""Generate non-enforcing MSG-180 licence inventory from a CycloneDX JSON BOM."""

import argparse
import csv
import hashlib
import json
import shutil
from collections import Counter
from datetime import date, datetime, timezone
from pathlib import Path

REGISTER_FIELDS = (
    "purl", "declared_license", "decision", "selected_license", "reviewer",
    "review_date", "review_by", "notes",
)
INVENTORY_FIELDS = (
    "profile", "purl", "group", "name", "version", "type", "bom_scope",
    "declared_license", "license_urls", "copyright", "review_hint",
)
REVIEW_FIELDS = INVENTORY_FIELDS + (
    "review_status", "recorded_decision", "selected_license", "reviewer",
    "review_date", "review_by", "notes",
)
PERMISSIVE_IDS = frozenset(("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "ISC"))


def declared_licenses(component):
    declarations = []
    urls = []
    for entry in component.get("licenses") or []:
        if not isinstance(entry, dict):
            continue
        if entry.get("expression"):
            declarations.append("expression:" + str(entry["expression"]))
        license_value = entry.get("license")
        if isinstance(license_value, dict):
            identifier = license_value.get("id")
            name = license_value.get("name")
            if identifier:
                declarations.append("id:" + str(identifier))
            elif name:
                declarations.append("name:" + str(name))
            if license_value.get("url"):
                urls.append(str(license_value["url"]))
    return " | ".join(sorted(set(declarations))), " | ".join(sorted(set(urls)))


def review_hint(component, declaration):
    if not declaration:
        return "missing-license-metadata"
    if component.get("group") == "io.mapsmessaging":
        return "internal-review"
    if declaration.startswith("id:") and declaration[3:] in PERMISSIVE_IDS:
        return "permissive-candidate"
    if declaration.startswith("expression:") and declaration[11:] in PERMISSIVE_IDS:
        return "permissive-candidate"
    return "manual-review"


def load_register(path):
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        if not reader.fieldnames or not set(REGISTER_FIELDS).issubset(reader.fieldnames):
            raise ValueError("Register is missing required columns: " + ", ".join(REGISTER_FIELDS))
        records = {}
        for row in reader:
            purl = (row.get("purl") or "").strip()
            if not purl:
                raise ValueError("Register has an entry without a purl")
            if purl in records:
                raise ValueError("Duplicate register entry: " + purl)
            decision = (row.get("decision") or "").strip()
            if decision not in ("", "approved", "rejected", "exception", "pending"):
                raise ValueError("Invalid register decision for " + purl + ": " + decision)
            if row.get("review_by"):
                date.fromisoformat(row["review_by"])
            records[purl] = row
        return records


def inventory_rows(bom, profile, register):
    if bom.get("bomFormat") != "CycloneDX" or not isinstance(bom.get("components"), list):
        raise ValueError("Expected a CycloneDX JSON BOM containing components")
    rows = []
    for component in bom["components"]:
        if not isinstance(component, dict):
            raise ValueError("Invalid CycloneDX component")
        group = str(component.get("group") or "")
        name = str(component.get("name") or "")
        version = str(component.get("version") or "")
        purl = str(component.get("purl") or component.get("bom-ref") or "")
        if not purl:
            raise ValueError("CycloneDX component lacks a purl/bom-ref: " + name)
        declaration, urls = declared_licenses(component)
        previous = register.get(purl, {})
        status = "unreviewed"
        if previous:
            if previous.get("declared_license", "") != declaration:
                status = "needs-re-review"
            elif previous.get("review_by") and previous["review_by"] < date.today().isoformat():
                status = "review-expired"
            else:
                status = previous.get("decision") or "unreviewed"
        rows.append({
            "profile": profile,
            "purl": purl,
            "group": group,
            "name": name,
            "version": version,
            "type": str(component.get("type") or ""),
            "bom_scope": str(component.get("scope") or ""),
            "declared_license": declaration,
            "license_urls": urls,
            "copyright": str(component.get("copyright") or ""),
            "review_hint": review_hint(component, declaration),
            "review_status": status,
            "recorded_decision": previous.get("decision", ""),
            "selected_license": previous.get("selected_license", ""),
            "reviewer": previous.get("reviewer", ""),
            "review_date": previous.get("review_date", ""),
            "review_by": previous.get("review_by", ""),
            "notes": previous.get("notes", ""),
        })
    return sorted(rows, key=lambda row: (row["purl"], row["name"]))


def write_csv(path, fields, rows):
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def run(args):
    raw = args.bom.read_bytes()
    bom = json.loads(raw)
    register = load_register(args.register)
    rows = inventory_rows(bom, args.profile, register)

    args.output_dir.mkdir(parents=True, exist_ok=True)
    (args.output_dir / "bom.json").write_bytes(raw)
    xml_bom = args.bom.with_suffix(".xml")
    if xml_bom.exists():
        shutil.copyfile(xml_bom, args.output_dir / "bom.xml")
    write_csv(args.output_dir / "inventory.csv", INVENTORY_FIELDS, rows)
    write_csv(args.output_dir / "review-register.csv", REVIEW_FIELDS, rows)

    hints = dict(sorted(Counter(row["review_hint"] for row in rows).items()))
    statuses = dict(sorted(Counter(row["review_status"] for row in rows).items()))
    project = bom.get("metadata", {}).get("component") or {}
    summary = {
        "profile": args.profile,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "project": {"name": project.get("name"), "version": project.get("version")},
        "bom_sha256": hashlib.sha256(raw).hexdigest(),
        "bom_serial_number": bom.get("serialNumber"),
        "dependency_count": len(rows),
        "review_hints": hints,
        "review_statuses": statuses,
        "enforcement_enabled": False,
        "limitations": [
            "Licence classifications are metadata-based review hints, not legal approvals.",
            "Maven BOM scope does not prove which files are shipped in installers or containers.",
            "Native binaries, bundled assets and JavaScript require separate inventories.",
        ],
    }
    (args.output_dir / "summary.json").write_text(
        json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    summary_lines = [
        f"MSG-180 licence baseline ({args.profile})",
        f"Dependencies: {len(rows)}",
        "Review hints: " + ", ".join(f"{key}={value}" for key, value in hints.items()),
        "Review statuses: " + ", ".join(f"{key}={value}" for key, value in statuses.items()),
        "Informational only: no licence finding fails this build.",
        "Check review-register.csv and source BOM; licence metadata is not legal advice.",
    ]
    (args.output_dir / "summary.txt").write_text("\n".join(summary_lines) + "\n", encoding="utf-8")
    print("\n".join(summary_lines))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", choices=("standard", "ml"), required=True)
    parser.add_argument("--bom", type=Path, required=True)
    parser.add_argument("--register", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    run(args)


if __name__ == "__main__":
    main()
