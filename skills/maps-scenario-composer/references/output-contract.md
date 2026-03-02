# Output Contract

Always return in this order.

1. `Composition Matrix`
- Source skill/scenario -> merged section mapping.
- Include composition mode (additive/staged/hybrid).
- Include conflict policy used (strict-fail/override/rename).

2. `Unified Assumptions`
- Shared assumptions and explicit conflict-resolution decisions.

3. `Merged Deployable Entity`
- One merged artifact set (patches/full files/scripts) with source trace tags.

4. `Unified Apply Sequence`
- One ordered command plan to apply all merged sections.

5. `Integrated Verification`
- Startup and listener checks.
- Per-scenario checks.
- Cross-scenario checks proving integration behavior.

6. `Failure Domain and Rollback`
- Failure split by scenario slice and integrated rollback path.

7. `Traceability Map`
- For each merged section, list source skills and rationale.

`Scenario Metrics and Dashboard`
- Provide combined metrics with per-scenario and cross-scenario views.
- Provide Grafana and MAPS-hosted combined dashboard definitions.

`C4 Architecture Diagram`
- Provide C4 diagrams for composed flow:
  - Context and Container required.
  - Component required for staged/hybrid composition.
- Include Mermaid source.

## Guardrails

- Never output disjoint per-skill artifacts without a merged apply plan.
- Never omit conflict-resolution decisions.
- Never omit cross-scenario verification.
- Never omit traceability from merged sections to source skills.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the composed flow.
