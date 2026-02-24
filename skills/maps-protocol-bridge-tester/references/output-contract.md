# Output Contract

Always return test outputs in this order.

1. `Bridge Matrix`
- Source protocol -> destination protocol, ports/listeners, namespace mapping, payload format.

2. `Preflight`
- Startup and bind checks with clear pass/fail state.

3. `Test Commands`
- Numbered command list for each layer: ingress, egress, end-to-end.
- Include timeouts and expected success markers.

4. `Results`
- For each layer: pass/fail, evidence summary, and observed output markers.

5. `Failure Classification`
- Identify failure domain: ingress, routing, transform/schema, egress.

6. `Remediation and Re-test`
- Exact config/runtime fixes and minimal re-test command set.

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never report pass without destination-side evidence.
- Never skip preflight checks.
- Never omit correlation IDs in test payload examples.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
