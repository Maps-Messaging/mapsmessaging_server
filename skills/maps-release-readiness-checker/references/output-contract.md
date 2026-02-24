# Output Contract

Always return in this order.

1. `Release Scope`
- Candidate type, target environment, mandatory features/protocols.
- Include required Docker image tags for this release decision.

2. `Gate Results`
- Numbered gate results with pass/fail and concise evidence.
- Include explicit Docker image availability and container smoke evidence.

3. `Blocking Findings`
- Blocking issues with impact and exact remediation steps.

4. `Non-Blocking Findings`
- Warnings, risk notes, and mitigation owners.

5. `Readiness Decision`
- PASS, CONDITIONAL PASS, or FAIL.

6. `Rerun Criteria`
- Exact command list to rerun after remediation.

7. `Rollback Confidence`
- Rollback commands and residual risk statement.

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never issue PASS without evidence across build, Docker image smoke, startup, and runtime checks.
- Never mix blockers with warnings.
- Never omit rerun criteria.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
