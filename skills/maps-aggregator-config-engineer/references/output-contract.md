# Output Contract

Always return in this order.

1. `Aggregation Requirement Mapping`
- Requirement phrase -> exact fields and absolute file paths.

2. `Window Model`
- Explain windowDurationMs, timeoutMs, and contribution mode semantics for this design.

3. `Assumptions`
- List all defaults and inferred values.

4. `Deployable Config Entity`
- Patch blocks or full YAML implementing `AggregatorManager` changes.

5. `Apply Steps`
- Exact commands to apply and reload/restart runtime.

6. `Verification Plan`
- Input publish commands for each configured input topic.
- Output consume/assert commands with success criteria.
- Include aggregate summary field checks when required.

7. `Risk Notes`
- Highlight likely operational risks (drop risk, timeout sensitivity, ordering assumptions).

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never omit contributionMode per input.
- Never omit both windowDurationMs and timeoutMs.
- Never claim aggregator correctness without output-topic evidence.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
