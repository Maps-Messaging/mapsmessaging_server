#!/usr/bin/env python3
"""Generate MSG-180 licence inventory and enforce reviewed third-party policy."""

import argparse
import csv
import hashlib
import json
import re
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
FAILURE_FIELDS = ("purl", "name", "declared_license", "review_status", "reason")
DEFAULT_POLICY = Path(__file__).resolve().parents[2] / "docs/compliance/license-policy.yaml"


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
    if declaration.startswith("id:") and " | " not in declaration:
        return "single-spdx"
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


def load_policy(path):
    # JSON is valid YAML 1.2, so no additional parser is needed on Buildkite.
    policy = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(policy, dict) or policy.get("schema_version") != 1 or policy.get("mode") != "enforce":
        raise ValueError("Unsupported or non-enforcing licence policy")
    ids = policy.get("approved_single_licenses")
    if not isinstance(ids, list) or not ids or any(not isinstance(x, str) or not x for x in ids) or len(ids) != len(set(ids)):
        raise ValueError("Invalid approved licence list")
    preferred = policy.get("preferred_license")
    if preferred not in ids:
        raise ValueError("Preferred licence must be an approved licence")
    if policy.get("internal_group") != "io.mapsmessaging":
        raise ValueError("Internal group must be explicitly io.mapsmessaging")
    artifacts = {}
    rules = policy.get("verified_artifacts")
    if not isinstance(rules, list):
        raise ValueError("Missing verified_artifacts policy list")
    for entry in rules:
        if not isinstance(entry, dict):
            raise ValueError("Invalid verified artifact rule")
        purl, declaration, selected, evidence = (
            entry.get("purl"), entry.get("declared_license"),
            entry.get("selected_license"), entry.get("evidence_url")
        )
        if not all(isinstance(x, str) and x for x in (purl, declaration, selected, evidence)):
            raise ValueError("Invalid verified artifact metadata")
        if not purl.startswith("pkg:maven/") or purl.startswith("pkg:maven/io.mapsmessaging/"):
            raise ValueError("Verified artifact must be an external Maven dependency: " + purl)
        if selected not in ids or purl in artifacts or not evidence.startswith("https://"):
            raise ValueError("Invalid, unapproved, or duplicate artifact selection: " + purl)
        if selected != "MIT" or declaration != "name:Bouncy Castle Licence":
            names = [d.removeprefix("id:") for d in declaration.split(" | ")]
            if selected not in names and "name:Apache License, 2.0" not in declaration:
                raise ValueError("Selected licence absent from declaration: " + purl)
        artifacts[purl] = entry
    return {"ids": frozenset(ids), "preferred": preferred, "verified": artifacts}


def policy_selection(declaration, policy):
    if declaration.startswith("id:") and " | " not in declaration:
        license_id = declaration[3:]
        return license_id if license_id in policy["ids"] else ""
    if declaration.startswith("expression:"):
        expression = declaration[len("expression:"):].strip()
        # Only explicit OR alternatives; not AND, WITH, or parenthesised expressions.
        choices = expression.split(" OR ")
        if len(choices) >= 2 and all(re.fullmatch(r"[A-Za-z0-9-.+]+", x) for x in choices):
            if policy["preferred"] in choices:
                return policy["preferred"]
            for name in choices:
                if name in policy["ids"]:
                    return name
    return ""


def review_status(purl, group, declaration, register, policy, today):
    previous = register.get(purl)
    if previous:
        if previous.get("declared_license", "") != declaration:
            return "needs-re-review", "", previous
        if previous.get("review_by") and previous["review_by"] < today:
            return "review-expired", "", previous
        decision = previous.get("decision") or "unreviewed"
        if decision in ("approved", "exception") and not previous.get("selected_license"):
            return "needs-re-review", "", previous
        return decision, previous.get("selected_license", ""), previous
    if group == "io.mapsmessaging":
        return "internal-unreviewed", "", {}
    approved = policy_selection(declaration, policy)
    if approved:
        return "policy-approved", approved, {}
    verified = policy["verified"].get(purl)
    if verified and verified["declared_license"] == declaration:
        return "policy-approved", verified["selected_license"], {}
    if verified:
        return "needs-re-review", "", {}
    return "unreviewed", "", {}


def inventory_rows(bom, profile, register, policy, today=None):
    if bom.get("bomFormat") != "CycloneDX" or not isinstance(bom.get("components"), list):
        raise ValueError("Expected a CycloneDX JSON BOM containing components")
    today = today or date.today().isoformat()
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
        status, selected, previous = review_status(purl, group, declaration, register, policy, today)
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
            "selected_license": selected,
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
    policy = load_policy(args.policy)
    rows = inventory_rows(bom, args.profile, register, policy)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    (args.output_dir / "bom.json").write_bytes(raw)
    xml_bom = args.bom.with_suffix(".xml")
    if xml_bom.exists():
        shutil.copyfile(xml_bom, args.output_dir / "bom.xml")
    write_csv(args.output_dir / "inventory.csv", INVENTORY_FIELDS, rows)
    write_csv(args.output_dir / "review-register.csv", REVIEW_FIELDS, rows)
    failures = []
    for row in rows:
        if row["group"] == "io.mapsmessaging":
            continue
        if row["review_status"] not in ("policy-approved", "approved", "exception"):
            failures.append({
                "purl": row["purl"], "name": row["name"],
                "declared_license": row["declared_license"],
                "review_status": row["review_status"],
                "reason": "Third-party dependency has no current approved licence selection",
            })
    write_csv(args.output_dir / "validation-failures.csv", FAILURE_FIELDS, failures)
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
        "unapproved_third_party_count": len(failures),
        "enforcement_enabled": bool(args.enforce),
        "limitations": [
            "Licence approval does not satisfy copyright, NOTICE or EPL source-availability obligations.",
            "Maven BOM scope does not prove which files are shipped in installers or containers.",
            "Native binaries, bundled assets and JavaScript require separate inventories.",
            "Internal MapsMessaging components are reported but excluded from this third-party gate.",
        ],
    }
    (args.output_dir / "summary.json").write_text(json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    summary_lines = [
        f"MSG-180 licence validation ({args.profile})",
        f"Dependencies: {len(rows)}",
        "Review hints: " + ", ".join(f"{key}={value}" for key, value in hints.items()),
        "Review statuses: " + ", ".join(f"{key}={value}" for key, value in statuses.items()),
        f"Unapproved third-party dependencies: {len(failures)}",
        "Enforcement: " + ("enabled" if args.enforce else "report-only"),
        "See validation-failures.csv for blockers; notice compliance requires separate verification.",
    ]
    (args.output_dir / "summary.txt").write_text("\n".join(summary_lines) + "\n", encoding="utf-8")
    print("\n".join(summary_lines))
    if args.enforce and failures:
        for issue in failures:
            print("LICENCE BLOCKER: " + issue["purl"] + " (" + issue["review_status"] + ")")
    return len(failures)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", choices=("standard", "ml"), required=True)
    parser.add_argument("--bom", type=Path, required=True)
    parser.add_argument("--register", type=Path, required=True)
    parser.add_argument("--policy", type=Path, default=DEFAULT_POLICY)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--enforce", action="store_true")
    args = parser.parse_args()
    return 1 if args.enforce and run(args) else 0


if __name__ == "__main__":
    raise SystemExit(main())
