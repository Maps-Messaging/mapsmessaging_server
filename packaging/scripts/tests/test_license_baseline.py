"""Unit tests for the non-blocking MSG-180 licence inventory builder."""

import csv
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

SCRIPT = Path(__file__).resolve().parents[1] / "license_baseline.py"
SPEC = importlib.util.spec_from_file_location("license_baseline", SCRIPT)
license_baseline = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(license_baseline)


class LicenseBaselineTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bom = self.root / "bom.json"
        self.register = self.root / "register.csv"
        self.register.write_text(",".join(license_baseline.REGISTER_FIELDS) + "\n", encoding="utf-8")

    def write_bom(self, components):
        self.bom.write_text(json.dumps({
            "bomFormat": "CycloneDX", "specVersion": "1.6",
            "metadata": {"component": {"name": "maps", "version": "4.6.0-SNAPSHOT"}},
            "components": components,
        }), encoding="utf-8")

    def run_baseline(self, profile="standard"):
        output = self.root / profile
        license_baseline.run(SimpleNamespace(
            profile=profile, bom=self.bom, register=self.register, output_dir=output
        ))
        with (output / "review-register.csv").open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle))
        return rows, json.loads((output / "summary.json").read_text(encoding="utf-8"))

    def test_unknown_and_multilicense_do_not_block_or_get_approved(self):
        self.write_bom([
            {"purl": "pkg:maven/example/none@1", "name": "none"},
            {"purl": "pkg:maven/example/dual@1", "name": "dual", "licenses": [
                {"license": {"id": "Apache-2.0"}}, {"license": {"id": "GPL-2.0-only"}}
            ]},
            {"purl": "pkg:maven/example/ok@1", "name": "ok", "licenses": [
                {"license": {"id": "Apache-2.0"}}
            ]},
        ])
        rows, summary = self.run_baseline()
        self.assertEqual(3, summary["dependency_count"])
        with (self.root / "standard" / "inventory.csv").open(encoding="utf-8", newline="") as handle:
            self.assertNotIn("review_status", csv.DictReader(handle).fieldnames)
        self.assertFalse(summary["enforcement_enabled"])
        self.assertEqual("missing-license-metadata", rows[1]["review_hint"])
        self.assertEqual("manual-review", rows[0]["review_hint"])
        self.assertEqual("permissive-candidate", rows[2]["review_hint"])
        self.assertEqual({"unreviewed": 3}, summary["review_statuses"])

    def test_prior_review_is_preserved_only_for_matching_declaration(self):
        self.write_bom([{
            "purl": "pkg:maven/example/a@1", "name": "a",
            "licenses": [{"license": {"id": "MIT"}}],
        }])
        with self.register.open("a", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle)
            writer.writerow(["pkg:maven/example/a@1", "id:MIT", "approved", "MIT", "reviewer", "2026-01-01", "", "reviewed"])
        rows, _ = self.run_baseline()
        self.assertEqual("approved", rows[0]["review_status"])
        self.assertEqual("reviewer", rows[0]["reviewer"])
        self.write_bom([{
            "purl": "pkg:maven/example/a@1", "name": "a",
            "licenses": [{"license": {"id": "GPL-3.0-only"}}],
        }])
        rows, _ = self.run_baseline()
        self.assertEqual("needs-re-review", rows[0]["review_status"])

    def test_ml_profile_and_bom_are_distinct(self):
        self.write_bom([{
            "purl": "pkg:maven/example/ml@1", "name": "ml",
            "licenses": [{"expression": "MIT OR Apache-2.0"}],
        }])
        rows, summary = self.run_baseline("ml")
        self.assertEqual("ml", summary["profile"])
        self.assertEqual("manual-review", rows[0]["review_hint"])
        self.assertEqual(self.bom.read_bytes(), (self.root / "ml" / "bom.json").read_bytes())

    def test_invalid_bom_is_reported(self):
        self.bom.write_text('{"bomFormat":"CycloneDX"}', encoding="utf-8")
        with self.assertRaises(ValueError):
            self.run_baseline()

    def test_expired_approval_is_not_treated_as_current(self):
        self.write_bom([{
            "purl": "pkg:maven/example/a@1", "name": "a",
            "licenses": [{"license": {"id": "MIT"}}],
        }])
        with self.register.open("a", encoding="utf-8", newline="") as handle:
            csv.writer(handle).writerow([
                "pkg:maven/example/a@1", "id:MIT", "approved", "MIT",
                "reviewer", "2026-01-01", "2000-01-01", "old approval",
            ])
        rows, _ = self.run_baseline()
        self.assertEqual("review-expired", rows[0]["review_status"])
        self.assertEqual("approved", rows[0]["recorded_decision"])

    def test_duplicate_register_entry_is_rejected(self):
        self.write_bom([])
        with self.register.open("a", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle)
            for _ in range(2):
                writer.writerow(["pkg:maven/example/a@1", "id:MIT", "approved", "MIT", "reviewer", "2026-01-01", "", "reviewed"])
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            self.run_baseline()


if __name__ == "__main__":
    unittest.main()
