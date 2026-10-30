"""Unit tests for the enforcing MSG-180 third-party licence gate."""

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
        self.policy = license_baseline.DEFAULT_POLICY

    def write_bom(self, components):
        self.bom.write_text(json.dumps({
            "bomFormat": "CycloneDX", "specVersion": "1.6",
            "metadata": {"component": {"name": "maps", "version": "4.6.0-SNAPSHOT"}},
            "components": components,
        }), encoding="utf-8")

    def run_baseline(self, profile="standard", enforce=True):
        output = self.root / profile
        license_baseline.run(SimpleNamespace(
            profile=profile, bom=self.bom, register=self.register, policy=self.policy,
            output_dir=output, enforce=enforce
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
        self.assertTrue(summary["enforcement_enabled"])
        self.assertEqual("missing-license-metadata", rows[1]["review_hint"])
        self.assertEqual("manual-review", rows[0]["review_hint"])
        self.assertEqual("single-spdx", rows[2]["review_hint"])
        self.assertEqual({"unreviewed": 2, "policy-approved": 1}, summary["review_statuses"])
        self.assertEqual(2, summary["unapproved_third_party_count"])

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


    def test_explicit_or_selects_apache_and_and_is_unreviewed(self):
        self.write_bom([
            {"purl": "pkg:maven/example/a@1", "name": "a",
             "licenses": [{"expression": "Apache-2.0 OR EPL-2.0"}]},
            {"purl": "pkg:maven/example/b@1", "name": "b",
             "licenses": [{"expression": "Apache-2.0 AND EPL-2.0"}]},
        ])
        rows, summary = self.run_baseline()
        self.assertEqual("policy-approved", rows[0]["review_status"])
        self.assertEqual("Apache-2.0", rows[0]["selected_license"])
        self.assertEqual("unreviewed", rows[1]["review_status"])
        self.assertEqual(1, summary["unapproved_third_party_count"])

    def test_specific_jetty_version_approved_but_new_version_fails(self):
        self.write_bom([
            {"purl": "pkg:maven/org.eclipse.jetty/jetty-http@12.1.12?type=jar",
             "group": "org.eclipse.jetty", "name": "jetty-http", "version": "12.1.12",
             "licenses": [{"license": {"id": "Apache-2.0"}}, {"license": {"id": "EPL-2.0"}}]},
            {"purl": "pkg:maven/org.eclipse.jetty/jetty-http@12.1.13?type=jar",
             "group": "org.eclipse.jetty", "name": "jetty-http", "version": "12.1.13",
             "licenses": [{"license": {"id": "Apache-2.0"}}, {"license": {"id": "EPL-2.0"}}]},
        ])
        rows, summary = self.run_baseline()
        self.assertEqual("Apache-2.0", rows[0]["selected_license"])
        self.assertEqual("unreviewed", rows[1]["review_status"])
        self.assertEqual(1, summary["unapproved_third_party_count"])

    def test_external_missing_metadata_blocks_but_internal_does_not(self):
        self.write_bom([
            {"purl": "pkg:maven/com.ecwid.consul/consul-api@1.4.6", "group": "com.ecwid.consul", "name": "consul-api"},
            {"purl": "pkg:maven/io.mapsmessaging/cot@1.1.0", "group": "io.mapsmessaging", "name": "cot"},
        ])
        rows, summary = self.run_baseline()
        self.assertEqual("unreviewed", rows[0]["review_status"])
        self.assertEqual("internal-unreviewed", rows[1]["review_status"])
        self.assertEqual(1, summary["unapproved_third_party_count"])

    def test_policy_corruption_fails_closed(self):
        self.write_bom([])
        self.policy = self.root / "bad-policy.yaml"
        self.policy.write_text('{"schema_version":1,"mode":"report-only"}', encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "non-enforcing"):
            self.run_baseline()

    def test_policy_approved_single_epl(self):
        self.write_bom([{
            "purl": "pkg:maven/org.glassfish.grizzly/grizzly-http@4.0.2", "name": "grizzly-http",
            "group": "org.glassfish.grizzly", "licenses": [{"license": {"id": "EPL-2.0"}}]
        }])
        rows, summary = self.run_baseline()
        self.assertEqual(0, summary["unapproved_third_party_count"])
        self.assertEqual("EPL-2.0", rows[0]["selected_license"])

    def test_new_library_with_nested_apache_or_is_policy_approved(self):
        self.write_bom([{
            "purl": "pkg:maven/example/new-library@2.1", "name": "new-library",
            "licenses": [{"expression": "(LGPL-2.1-only OR (MIT OR Apache-2.0))"}],
        }])
        rows, summary = self.run_baseline()
        self.assertEqual("policy-approved", rows[0]["review_status"])
        self.assertEqual("Apache-2.0", rows[0]["selected_license"])
        self.assertEqual(0, summary["unapproved_third_party_count"])

    def test_new_library_chooses_other_approved_license_if_no_apache(self):
        self.write_bom([{
            "purl": "pkg:maven/example/new-library@2.1", "name": "new-library",
            "licenses": [{"expression": "(GPL-3.0-only OR MIT)"}],
        }])
        rows, summary = self.run_baseline()
        self.assertEqual("MIT", rows[0]["selected_license"])
        self.assertEqual("policy-approved", rows[0]["review_status"])
        self.assertEqual(0, summary["unapproved_third_party_count"])

    def test_new_library_is_not_approved_by_unverified_license_list(self):
        self.write_bom([{
            "purl": "pkg:maven/example/new-library@2.1", "name": "new-library",
            "licenses": [{"license": {"id": "Apache-2.0"}},
                         {"license": {"id": "GPL-3.0-only"}}],
        }])
        rows, summary = self.run_baseline()
        self.assertEqual("unreviewed", rows[0]["review_status"])
        self.assertEqual("", rows[0]["selected_license"])
        self.assertEqual(1, summary["unapproved_third_party_count"])

    def test_nested_spdx_precedence_never_ignores_required_and(self):
        policy = license_baseline.load_policy(self.policy)
        selection = license_baseline.policy_selection
        self.assertEqual("Apache-2.0", selection("expression:Apache-2.0 OR (MIT AND GPL-3.0-only)", policy))
        self.assertEqual("EPL-2.0", selection("expression:(Apache-2.0 AND MIT) OR EPL-2.0", policy))
        self.assertEqual("", selection("expression:Apache-2.0 AND (MIT OR GPL-3.0-only)", policy))
        self.assertEqual("", selection("expression:(Apache-2.0 OR MIT) AND LGPL-3.0-only", policy))

    def test_spdx_with_exception_is_not_approved_without_explicit_review(self):
        policy = license_baseline.load_policy(self.policy)
        selection = license_baseline.policy_selection
        self.assertEqual("", selection("expression:Apache-2.0 WITH Classpath-exception-2.0", policy))
        self.assertEqual("MIT", selection("expression:MIT OR (GPL-2.0-only WITH Classpath-exception-2.0)", policy))
        self.assertEqual("", selection("expression:Apache-2.0 OR (MIT WITH (BSD-3-Clause))", policy))

    def test_malformed_expressions_fail_closed(self):
        policy = license_baseline.load_policy(self.policy)
        for expression in (
            "Apache-2.0 OR", "(Apache-2.0 OR MIT", "Apache-2.0 OR OR MIT",
            "Apache-2.0 AND", "Apache-2.0) OR MIT", "Apache-2.0 | GPL-3.0",
            "(GPL-3.0-only OR MIT) AND", "Apache-2.0 OR MIT AND",
        ):
            with self.subTest(expression=expression):
                self.assertEqual("", license_baseline.policy_selection("expression:" + expression, policy))



if __name__ == "__main__":
    unittest.main()
