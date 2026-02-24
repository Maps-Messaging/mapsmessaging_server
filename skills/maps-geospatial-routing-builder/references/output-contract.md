# Output Contract

Always return in this order.

1. `Geospatial Requirement Mapping`
- Requirement phrase -> fields and absolute file paths.

2. `Geo Model`
- Coordinate assumptions, units, geohash precision, and threshold model.

3. `Assumptions`
- Defaults and inferred geospatial behavior.

4. `Deployable Config Entity`
- Patch blocks or full YAML implementing geospatial routing behavior.

5. `Apply Steps`
- Exact commands to apply and restart/reload runtime.

6. `Verification`
- Known coordinate vectors.
- Geohash path assertions.
- Distance-threshold pass/fail assertions.

7. `Risk Notes`
- Precision tradeoffs, GPS quality risks, and fallback behavior.

`Scenario Metrics and Dashboard`
- Provide geospatial metrics with collection points and expected ranges.
- Provide Grafana dashboard and MAPS-hosted dashboard definitions.

`C4 Architecture Diagram`
- Provide C4 diagrams for geospatial flow:
  - Context and Container required.
  - Component required for hybrid/multi-stage geo pipelines.
- Include Mermaid source.

## Guardrails

- Never omit units and coordinate assumptions.
- Never omit at least one known-distance verification.
- Never omit invalid-GPS fallback route.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
