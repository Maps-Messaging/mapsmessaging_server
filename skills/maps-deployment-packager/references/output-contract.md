# Output Contract

Always return in this order.

1. `Deployment Target Matrix`
- Include target platform options used or considered:
  - local, Docker, Kubernetes-style, Fly.io, AWS, Google Cloud, Azure
- Include connectivity profile: connected, air-gapped, degraded, delay-tolerant.

2. `Authentication Profile`
- Selected auth mechanism(s) and exact deployment wiring points.
- Include fallback/default behavior if auth details are omitted.

3. `Storage and Volume Profile`
- Selected storage mode (ephemeral/persistent) and destination/storage type expectations.
- Include exact volume/mount mappings per target.

4. `Configuration Source Mode`
- File-based or Consul-based configuration mode.
- Include exact env/flags/paths required by selected mode.

5. `State and Config Backend Choice`
- Declare backend selection:
  - Fly KV (especially for Fly.io lightweight deployments, no cluster required), or
  - dedicated Consul cluster, or
  - none/local only.
- If Consul is selected, include `Consul Topology Option`.

6. `Consul Topology Option`
- Required only when Consul mode is selected.
- Must be one of:
  - HCP Consul
  - self-managed Consul
- Include addresses/endpoints, auth/TLS assumptions, and deployment placement (edge/cloud).

7. `Object Storage Option`
- Required when object storage is used.
- Include provider selection:
  - AWS S3
  - S3-compatible provider (for example Cloudflare R2)
- Include endpoint/bucket/credential wiring details.

8. `Artifact Manifest`
- Absolute file paths and generated artifact list.

9. `Deployable Entity`
- Full files, patch blocks, command scripts, or ConfigMap YAML blocks.
- For Fly.io targets, include:
  - the intended `fly deploy` working directory
  - Dockerfile path as resolved from that working directory
  - explicit architecture selection behavior (`TARGETARCH` and/or deploy flags)

10. `Apply Steps`
- Exact commands to apply/deploy artifacts.
- For air-gapped bundles, include offline image/artifact load steps.
- For Fly.io targets, commands must be coherent with the declared working directory and Dockerfile path.

11. `Startup Diagnostics`
- Commands to confirm process/container startup and config load.

12. `Listener Verification`
- Commands proving expected protocol listeners are bound.

13. `Buffer-and-Forward Verification`
- Commands that validate queueing/buffering during outage and forward-on-recovery behavior.

14. `Rollback`
- Minimal rollback commands or file restoration path.

`Scenario Metrics and Dashboard`
- Provide scenario-specific metrics list with collection points and expected ranges for the deployed topology.
- Provide a Grafana dashboard artifact definition and a MAPS-hosted dashboard definition suitable for local-first use.

`C4 Architecture Diagram`
- Provide C4 diagrams for the deployed flow:
  - Context and Container diagrams required.
  - Component diagram required when multiple internal services/transform stages are involved.
- Include diagram source in Mermaid format so it is directly renderable.

## Guardrails

- Never omit absolute paths in manifest.
- Never omit startup and listener checks.
- Never claim deployment readiness without diagnostics.
- Never omit connectivity-profile assumptions.
- Never omit outage/recovery verification steps for delay-tolerant scenarios.
- Never omit authentication profile mapping.
- Never omit storage and volume mapping.
- Never omit file-vs-Consul config source declaration.
- Never omit backend choice declaration (Fly KV vs dedicated Consul vs none).
- Never omit Fly working directory plus Dockerfile path resolution details when Fly.io is selected.
- Never omit explicit Fly architecture selection behavior when image availability is architecture-constrained.
- Never omit Consul topology details when Consul mode is selected.
- Never omit object storage provider/endpoint/credential mapping when object storage is selected.
- Never omit scenario-specific metrics and at least one testable dashboard output.
- Never omit C4 diagram source for the deployed flow.
