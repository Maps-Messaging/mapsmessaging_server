# Output Contract

Always return in this order.

1. `Satellite Service Pattern Matrix`
- At least 2 variants when user asks for alternatives.
- Each variant: provider profile, ingress protocol, egress protocol(s), namespace boundary, and tradeoffs.

2. `Selected Pattern`
- State chosen variant and why.

3. `Provider and Protocol Assumptions`
- List inferred provider/runtime constraints and protocol availability assumptions.
- Include encoding assumptions and whether CBC SIN/MIN auto-routing is active.

4. `Deployable Config Entity`
- Patch blocks or full YAML implementing satellite plus protocol fan-out changes.

5. `Apply Steps`
- Exact commands to apply and restart or reload runtime.

6. `Diagnostics`
- Provider startup checks, protocol listener checks, and bind checks.

7. `Verification`
- Commands proving satellite-boundary ingest and downstream pub-sub delivery for chosen protocol paths.
- Include one check proving CBC messages with SIN/MIN values under `127` are published into individual `.../{sin}/{min}` hierarchy topics.

8. `Fallback Notes`
- Explicit fallback behavior when requested provider or protocol path is not currently runtime-supported.

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never omit variant matrix when alternatives are requested.
- Never claim provider support without diagnostics.
- Never omit protocol listener verification for each chosen downstream protocol.
- Never omit CBC `<127` SIN/MIN hierarchy routing behavior when CBC encoding is requested.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
