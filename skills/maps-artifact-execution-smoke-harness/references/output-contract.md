# Output Contract

Always return in this order.

1. `Smoke Scope`
- Artifact source, runtime mode, expected listeners/routes, and pass criteria.

2. `Execution Plan`
- Numbered phases with exact commands and timeouts.

3. `Applied Artifact Manifest`
- Absolute paths and mounted/applied file map.

4. `Startup Evidence`
- Startup command outputs summary and blocker scan findings.

5. `Listener Evidence`
- Bind checks and explicit pass/fail for each required listener.

6. `Traffic Evidence`
- Producer/consumer or route verification outcomes with correlation markers.

7. `Failure Classification`
- Failure class and root-cause notes when failing.

8. `Remediation and Re-run`
- Exact fix commands and minimal rerun command set.

9. `Teardown Evidence`
- Cleanup commands and final environment state check.

`Scenario Metrics and Dashboard`
- Provide scenario-specific smoke metrics with collection points and expected ranges.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the executed flow:
  - Context and Container required.
  - Component required when multiple test stages or protocol routes are involved.
- Include Mermaid source.

## Guardrails

- Never report PASS without startup, listener, and traffic evidence.
- Never omit teardown commands.
- Never omit absolute artifact paths.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
