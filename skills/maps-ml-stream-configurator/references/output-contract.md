# Output Contract

Always return in this order.

1. `ML Stream Requirement Mapping`
- Requirement phrase -> exact fields and absolute file paths.

2. `Pipeline Shape`
- Simple baseline pipeline.
- Advanced multi-pass option(s) when requested.

3. `Model and Store Assumptions`
- Model type assumptions and store mode (`file`, `nexus`, `s3`, `maps`).
- Include compatibility notes (Smile 4.3.0 and portable model artifacts).

4. `Deployable Config Entity`
- Patch blocks or full YAML implementing ML stream configuration.

5. `Apply Steps`
- Exact commands to apply and restart/reload runtime.

6. `Verification`
- Publish/observe steps per stage.
- For multi-pass, include stage-by-stage evidence checks.

7. `Risk and Complexity Notes`
- Operational complexity, drift risk, retrain sensitivity, fallback behavior.

`Scenario Metrics and Dashboard`
- Provide scenario-specific ML metrics with collection points and expected ranges.
- Provide Grafana dashboard definition and MAPS-hosted dashboard definition.

`C4 Architecture Diagram`
- Provide C4 diagrams for the ML flow:
  - Context and Container required.
  - Component required for multi-pass pipelines.
- Include Mermaid source.

## Guardrails

- Never omit the simple baseline.
- Never present advanced chain without explicit stage boundaries.
- Never omit model-store assumptions.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
