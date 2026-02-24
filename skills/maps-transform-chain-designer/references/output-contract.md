# Output Contract

Always return in this order.

1. `Transformation Contract`
- Source format, target format, required stages, namespace/path.

2. `Ordered Stage Plan`
- Numbered stages with input/output contentType and purpose.

3. `Deployable Config Entity`
- Patch blocks or full YAML/ConfigMap artifacts implementing the chain.

4. `Apply Steps`
- Exact commands to apply config changes.

5. `Validation`
- Static checks for stage compatibility and key settings.

6. `Runtime Verification`
- Producer and consumer commands proving transformed output reaches destination in expected format.

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never omit stage order.
- Never omit contentType transitions.
- Never claim success without destination-side transformed payload evidence.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
