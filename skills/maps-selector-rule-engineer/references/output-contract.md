# Output Contract

Always return in this order.

1. `Selector Requirement Mapping`
- Requirement phrase -> selector field/path and absolute file path.

2. `Selector Evaluation Model`
- Routing selector order and subscriber selector order.
- Conflict/precedence policy.

3. `Assumptions`
- Defaults and inferred selector constraints.

4. `Deployable Config Entity`
- Patch blocks or full YAML implementing selectors and routes.

5. `Apply Steps`
- Exact commands to apply and restart/reload runtime.

6. `Verification`
- Positive and negative test vectors.
- Runtime publish/consume commands with pass/fail assertions.

7. `Performance and Risk Notes`
- Complexity notes, likely hot paths, and fallback guidance.

`Scenario Metrics and Dashboard`
- Provide selector-specific metrics list with collection points and expected ranges.
- Provide Grafana dashboard and MAPS-hosted dashboard definitions.

`C4 Architecture Diagram`
- Provide C4 diagrams for selector-enabled flow:
  - Context and Container required.
  - Component required for chained selectors.
- Include Mermaid source.

## Guardrails

- Never omit selector order.
- Never omit non-match evidence.
- Never omit absolute file paths.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
