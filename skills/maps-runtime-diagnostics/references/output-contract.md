# Output Contract

Always return diagnostics in this order.

1. `Observed Failure`
- One paragraph: what failed, where, and when.

2. `Evidence`
- Exact command outputs (summarized) with concrete indicators.
- Include at least one startup-log check and one bind-state check.

3. `Root Cause`
- Primary root cause plus contributing factors.
- Separate confirmed facts from inference.

4. `Remediation`
- Exact commands and config edits to resolve the issue.
- Use absolute file paths.

5. `Post-Fix Verification`
- Listener verification command(s).
- Protocol smoke commands and expected success markers.

6. `Residual Risks`
- Any unresolved uncertainties or dependencies (image tags, provider plugins, external services).

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never claim port collision without explicit bind-error evidence.
- Never stop at symptoms; provide a concrete fix path.
- Never omit exact verification commands.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
