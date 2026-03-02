# Output Contract

Always return outputs in this order.

1. `Schema Requirement Mapping`
- Requirement phrase -> config fields and absolute file paths.

2. `Assumptions`
- List all defaulted schema/content-type choices.

3. `Deployable Config Entity`
- Provide patch blocks, full YAML bodies, or ConfigMap manifests.
- Include exact `schemaId` and `contentType` values.

4. `Apply Steps`
- Exact commands to apply YAML changes in this repo.

5. `Validation`
- Commands that verify schema linkage and content-type alignment.

6. `Runtime Verification`
- Producer/consumer checks showing payload acceptance on expected namespace.

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never omit absolute file paths.
- Never leave unresolved placeholders.
- Never provide schema behavior without exact implementing YAML.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
