# MSG-180: Licence inventory baseline (standard and ML)

The original reporting-only baseline is now an **enforcing third-party licence gate**. Maven `verify` generates profile-specific reports and fails before `deploy` when a third-party component is unapproved, rejected, missing licence metadata, has a changed declaration, or has an expired review. Internal MapsMessaging dependencies remain in the report but are excluded from the third-party gate. This policy does not demonstrate distribution-notice or EPL source-availability compliance.

## Existing build integration

The server's existing `cyclonedx-maven-plugin` produces `target/bom.json` (and XML) in Maven's `package` phase. Maven `verify` invokes `packaging/scripts/license_baseline.py --enforce` **before installation and deployment**. The standard and `-Pml` snapshot and release Buildkite pipelines upload reports as build artifacts from `target/license-baseline/**/*`. Each profile is uploaded separately. A nonzero validator exit code is not suppressed.

The validator requires only Python 3 standard library. `docs/compliance/license-policy.yaml` is JSON-compatible YAML 1.2. Malformed policy or missing BOM data fails closed. Distribution layout, deployment actions and Maven runtime-licence fetching remain unchanged.

## Running locally

Build and validate each profile locally (without publishing):

```shell
mvn -DskipTests=true clean verify -U
mvn -DskipTests=true clean verify -Pml -U
```

Maven `verify` runs the policy validator and fails when a third-party licence is unapproved. Running standalone without `--enforce` is possible for investigations, but is not CI validation. Both profiles produce `target/bom.json`; build them separately or preserve the first report before cleaning the workspace. Buildkite runs them in separate steps.

## Reports

For each profile Buildkite retains `bom.json` (and `bom.xml` when present), `inventory.csv`, `review-register.csv`, `validation-failures.csv`, `summary.json` and `summary.txt`.

* `inventory.csv`: full CycloneDX component list, licence identifiers/expressions, URLs, any provided copyright, and the source BOM's scope.
* `review-register.csv`: licence inventory augmented with review fields, seeded from the version-controlled register. Reviewers can use this as a worksheet, but **do not replace the curated register with the entire generated file**.
* `summary.json`: component counts by review hint/status, plus the source BOM's SHA-256 and generation timestamp.

Third-party components declaring a single approved SPDX identifier are `policy-approved`. An explicit SPDX `OR` expression can select Apache-2.0 where available. Bare multi-entry CycloneDX licence lists, `AND`, `WITH` and complex expressions are **not** interpreted as interchangeable choices; these need an exact-version and declaration-matched verified-artifact entry in `license-policy.yaml` or a manual review in the CSV register. Selected licences and upstream evidence are recorded in the policy. Internal MapsMessaging components remain visible as `internal-unreviewed`, but are outside this *third-party* gate.

The central register is `docs/compliance/license-register.csv`. To record an approved or reviewed dependency, copy its `purl` and **exact** `declared_license` into the central register and fill `decision` (`approved`, `rejected`, `exception` or `pending`), `selected_license`, `reviewer`, `review_date`, `review_by` (ISO date, optional), and `notes`. Register entries are tied to an exact component/version and reported licence declaration. Changes to that declaration or an expired review date become `needs-re-review` or `review-expired` in reports. Unapproved, rejected, pending and expired third-party entries fail Maven `verify` and appear in `validation-failures.csv`. Legal decisions require an authorized reviewer.

For a combined register across standard and ML builds, compare both Buildkite `review-register.csv` artifacts and add each distinct `purl` to the central register after review. Never assume that the BOM proves a dependency is packaged: it includes Maven's selected compile/provided/runtime/system scopes, and native, JavaScript, container, installer and bundled assets require separate scanning in later stages of MSG-180.

## Verification

Run the script's standard-library tests:

```shell
python3 -m unittest discover -s packaging/scripts/tests -p 'test_license_baseline.py'
```

**Policy limitations:** Licence selection does not prove that the required licence copies, third-party copyright notices, EPL source-offer requirements or NOTICE files are packaged. The supplied Maven BOMs also omit JavaScript, OS packages and some native/bundled components. Those items remain separate MSG-180 work.

**Expected historical BOM failure:** Both supplied builds still list `consul-api:1.4.6` with missing licence metadata, plus `quickfixj-base:3.0.1` and `quickfixj-core:3.0.1` (expected to disappear when the updated `schemas` dependency is published). These three are intentionally not approved. Inspect the new build's `validation-failures.csv` rather than assuming they have gone.
