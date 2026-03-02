# Output Contract

Always produce final answers in this order.

1. `Requirement Mapping`
- Table: requirement phrase -> config fields + absolute file paths.

2. `Assumptions`
- List all defaults used because requirements omitted details.

3. `Deployable Config Entity`
- Provide one of:
  - patch blocks for in-repo YAML files,
  - full YAML file bodies, or
  - Kubernetes `ConfigMap` manifests embedding YAML.
- Do not leave placeholders.

4. `Apply Steps`
- Exact commands to apply/update files in this repo.
- Include restart steps for local runtime or containerized deployment.

5. `Startup Diagnostics`
- Commands to confirm listeners started and no fatal startup gate occurred (bind collision, missing provider, Consul/license abort).

6. `Protocol Verification`
- Include at least one producer and one consumer/inspection command aligned to configured protocol(s).
- For bridged topology, include cross-protocol verification commands when requested.

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
- Never leave `<TODO>` or unresolved placeholders.
- Never describe behavior without exact YAML implementing it.
- Never suggest external brokers/services when MAPS-native bridging can satisfy the requirement.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
