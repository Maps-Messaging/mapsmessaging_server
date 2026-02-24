# Output Contract

Always return in this order.

1. `Lifecycle Requirement Mapping`
- Requirement phrase -> fields and absolute file paths.

2. `Lifecycle Shape`
- Simple baseline lifecycle.
- Advanced lifecycle option(s) when requested.

3. `Model and Store Assumptions`
- Model type, store mode, artifact format, Smile 4.3.0 compatibility assumptions.

4. `Deployable Config Entity`
- Patch blocks or full YAML implementing lifecycle configuration.

5. `Apply Steps`
- Exact commands to apply and restart/reload runtime.

6. `Verification`
- Stage-by-stage checks for model load, inference output, and retrain behavior.

7. `Risk and Operational Notes`
- Drift, retrain instability, rollback plan, compatibility limits.

`Scenario Metrics and Dashboard`
- Provide ML lifecycle metrics with collection points and expected ranges.
- Provide Grafana dashboard and MAPS-hosted dashboard definitions.

`C4 Architecture Diagram`
- Provide C4 diagrams for lifecycle flow:
  - Context and Container required.
  - Component required for hybrid or multi-stage lifecycle.
- Include Mermaid source.

## Guardrails

- Never omit simple baseline lifecycle.
- Never omit model-store assumptions and artifact format constraints.
- Never omit external-model compatibility checks when external ingestion is requested.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
