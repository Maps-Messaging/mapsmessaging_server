# MSG-180: Licence inventory baseline (standard and ML)

This is a **reporting-only** first step. It does not determine whether a licence is legally compatible, approve any dependency, change the Maven dependency graph, or block a build for a licence finding. Enforcement, comprehensive source scanning, and packaging notices require separate approval and implementation.

## Existing build integration

The server's existing `cyclonedx-maven-plugin` produces `target/bom.json` (and XML) in Maven's `package` phase. The standard and `-Pml` snapshot and release Buildkite pipelines run the baseline report script after the Maven deploy step. Reports are uploaded as Buildkite artifacts from `target/license-baseline/**/*` and remain separate for each profile.

The baseline script uses Python 3's standard library only. The Buildkite command catches errors and prints a conspicuous warning to avoid changing existing release failure semantics; a missing report is **not** a clean result and must be investigated. The POM, distribution layout, deployment actions and Maven licence-fetching logic remain unchanged.

## Running locally

Build the selected profile as usual (without publishing):

```shell
mvn -DskipTests=true clean package -U
python3 packaging/scripts/license_baseline.py \
  --profile standard --bom target/bom.json \
  --register docs/compliance/license-register.csv \
  --output-dir target/license-baseline/standard

mvn -DskipTests=true clean package -Pml -U
python3 packaging/scripts/license_baseline.py \
  --profile ml --bom target/bom.json \
  --register docs/compliance/license-register.csv \
  --output-dir target/license-baseline/ml
```

Because both Maven builds write `target/bom.json`, copy the first report before `clean` removes `target/`. The Buildkite pipelines upload each profile's results independently. Do not combine a standard and ML report by overwriting one with the other.

## Reports

For each profile Buildkite retains `bom.json` (and `bom.xml` when present), `inventory.csv`, `review-register.csv`, `summary.json` and `summary.txt`.

* `inventory.csv`: full CycloneDX component list, licence identifiers/expressions, URLs, any provided copyright, and the source BOM's scope.
* `review-register.csv`: licence inventory augmented with review fields, seeded from the version-controlled register. Reviewers can use this as a worksheet, but **do not replace the curated register with the entire generated file**.
* `summary.json`: component counts by review hint/status, plus the source BOM's SHA-256 and generation timestamp.

All entries are `unreviewed` until a human records a decision. `permissive-candidate` means only that a single common SPDX identifier was declared. It is **not** an approval. Dual licences, SPDX expressions with operators, unknown metadata and internally licensed MapsMessaging components require review.

The central register is `docs/compliance/license-register.csv`. To record an approved or reviewed dependency, copy its `purl` and **exact** `declared_license` into the central register and fill `decision` (`approved`, `rejected`, `exception` or `pending`), `selected_license`, `reviewer`, `review_date`, `review_by` (ISO date, optional), and `notes`. Register entries are tied to an exact component/version and reported licence declaration. Changes to that declaration or an expired review date become `needs-re-review` or `review-expired` in reports. All such findings remain informational for this phase. Legal decisions require an authorized reviewer.

For a combined register across standard and ML builds, compare both Buildkite `review-register.csv` artifacts and add each distinct `purl` to the central register after review. Never assume that the BOM proves a dependency is packaged: it includes Maven's selected compile/provided/runtime/system scopes, and native, JavaScript, container, installer and bundled assets require separate scanning in later stages of MSG-180.

## Verification

Run the script's standard-library tests:

```shell
python3 -m unittest discover -s packaging/scripts/tests -p 'test_license_baseline.py'
```

This baseline does not replace legal review, ORT source-code scanning, or validation of final release notices. These are follow-up work under MSG-180, not silently implied by a generated BOM.
