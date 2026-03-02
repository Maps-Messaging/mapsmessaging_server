# Output Contract

Always return in this order.

1. `CAN Requirement Mapping`
- Requirement phrase -> exact fields and absolute file paths.

2. `Interface Profile`
- State vcan or native profile and device names used.

3. `Assumptions`
- List inferred protocol/decode choices and database defaults.

4. `Deployable Config Entity`
- Patch blocks or full YAML implementing canbus and protocol changes.

5. `Apply Steps`
- Exact commands to apply changes and restart/reload runtime.

6. `Verification`
- Startup checks.
- vcan/native injection commands or harness instructions.
- Output topic assertions with expected markers.

7. `Limits and Risks`
- Mention differences between vcan validation and physical bus behavior.

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never omit `deviceName` for canbus endpoints.
- Never omit protocol mode (raw or n2k decode).
- Never claim success without destination topic evidence.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
