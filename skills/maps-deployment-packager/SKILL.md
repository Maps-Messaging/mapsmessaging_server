---
name: maps-deployment-packager
description: Package MAPS Messaging manager configurations into deployable artifacts for local runtime, Docker-based deployments, and cloud targets including Fly.io, AWS, Google Cloud, and Microsoft Azure, including air-gapped, degraded-connectivity, and delay-tolerant buffer-and-forward scenarios. Use when requirements ask for environment-specific packaging, authentication profiles, storage and volume options, file or Consul configuration source modes, startup command sets, container mounts, offline artifact bundles, or repeatable apply and validation procedures.
---

# MAPS Deployment Packager

Convert MAPS manager YAML and runtime settings into deployable packaging outputs with deterministic apply and verification steps.

## Workflow

1. Normalize deployment target contract.
- Identify target mode: local process, Docker container, Kubernetes-style config bundle, or cloud target (Fly.io, AWS, Google Cloud, Microsoft Azure).
- Extract required manager files, environment variables, port mappings, and runtime dependencies.
- Identify connectivity profile: connected, air-gapped, degraded/intermittent, or delay-tolerant store-and-forward.
- Extract deployment control dimensions:
  - authentication mode (any supported MAPS mechanism for this deployment)
  - storage and volume profile (ephemeral vs persistent, file/memory/tier choices)
  - configuration source mode (local file-based vs Consul-based)
  - state/config backend option details where applicable:
    - Fly.io KV default profile (no cluster required)
    - optional dedicated Consul cluster mode (HCP Consul or self-managed Consul)
  - object storage provider option details where applicable:
    - AWS S3
    - S3-compatible object storage (including Cloudflare R2)

2. Build environment packaging plan.
- Read `skills/maps-deployment-packager/references/deployment-targets.md`.
- Generate artifact set for the selected target with minimal duplication.
- Keep config and runtime command paths explicit.

3. Materialize deployable entities.
- Local: resolved manager YAML set + start command sequence.
- Docker: bind-mount plan, `docker run` command, image tag, exposed ports.
- Kubernetes-style: ConfigMap-ready YAML payload blocks and container args/env.
- Cloud-target packaging:
  - Fly.io deployment bundle and service config, including optional Fly KV mapping profile when selected
  - for Fly.io Docker builds:
    - Dockerfile path must be relative to the selected working directory for `fly deploy`
    - Dockerfile `COPY` sources must be relative to Docker build context (avoid repo-root-prefixed paths when deploying from a subdirectory)
    - include explicit image architecture selection when required by available artifacts (for example force `amd64` when only x86 MAPS base images are available)
    - when MAPS container runs as non-root and Fly volume mounts as root-owned, include startup ownership fix before process drop (for example `chown` of `MAPS_DATA` path)
    - include runtime sizing defaults for constrained Fly machines (`[vm]` memory and JVM heap/metaspace flags)
  - AWS deployment bundle (container/task/service profile)
  - Google Cloud deployment bundle (container/service profile)
  - Microsoft Azure deployment bundle (container/app service profile)
  - optional dedicated Consul cluster profile for edge/cloud:
    - HCP Consul
    - self-managed Consul
  - optional object storage profile:
    - AWS S3
    - S3-compatible endpoints (for example Cloudflare R2)
- Air-gapped/degraded profile: offline image and dependency manifest, local config bundle, and replay/retry startup strategy with explicit buffer storage paths.

4. Validate packaging integrity.
- Ensure all referenced files exist.
- Check config mount paths and runtime env coherence (`MAPS_HOME`, `MAPS_CONF`, `MAPS_DATA`).
- Include startup diagnostics and listener checks.
- Prefer bundled scripts for repeatable validation:
  - `skills/maps-deployment-packager/scripts/run_maps_deployment_packager_skill_smoke.sh`
  - `skills/maps-deployment-packager/scripts/run_maps_deployment_packager_runtime_smoke.sh`
- Validate store-and-forward readiness (persistent destination paths, queue/backlog expectations, and recovery behavior after reconnect).
- Validate auth, storage, and config-source alignment:
  - auth manager configuration matches selected authentication mode
  - storage/volume mappings match persistence expectations
  - config source mode is explicit (file or Consul) and startup flags/env align
  - when Fly.io is selected with KV mode, no Consul cluster dependency is required
  - when Consul mode is selected, deployment specifies dedicated Consul topology (HCP or self-managed)
  - when object storage is selected, provider, endpoint, bucket, and credentials mapping are explicit
  - when Fly.io is selected, validate Dockerfile resolution and build-context-relative `COPY` statements
  - when Fly.io is selected and image architecture availability is constrained, validate explicit architecture selection in build args or deploy command
  - when Fly.io persistent volume is selected, validate runtime write access to `MAPS_DATA` for effective service user
  - when Fly.io machine size is small, validate JVM options are bounded to avoid OOM/restart loops

5. Return using output contract.
- Follow `skills/maps-deployment-packager/references/output-contract.md`.
- Include artifacts, apply commands, validation, and rollback hints.

## Rules

- Prefer repository-native startup scripts and layout conventions.
- Keep generated artifacts environment-specific and explicit.
- Never omit absolute file paths in package manifests.
- Include both startup diagnostics and protocol listener checks.
- For air-gapped profiles, avoid steps requiring live external dependency fetch at deploy time.
- For delay-tolerant profiles, include explicit buffering assumptions and replay verification steps.
- Always include at least one simple local deployment profile and one advanced cloud profile in matrix mode.
- Always include auth mode, storage mode, and config source mode in generated deployment profiles.
- Always include backend option details when relevant: Fly KV usage, Consul topology choice, and object storage provider choice.
- For Fly.io profiles, always state the effective build context directory and ensure Dockerfile path is valid from that directory.
- For Fly.io profiles, always state architecture behavior (`amd64`/`arm64`) and the reason when forcing one architecture.

## Scenario Modes

- `Simple Local Default`:
  - Package one local runtime profile and one minimal Docker run profile with working defaults.
  - Include one default auth mode, one default persistent storage mode, and one default file-based config mode for quick local testing.
  - Include shortest apply/verify path for local smoke execution.
- `Advanced Combination Matrix`:
  - Provide 4 to 8 packaging variants across local, Docker, Kubernetes-style, Fly.io, AWS, Google Cloud, Azure, air-gapped, degraded, and delay-tolerant profiles.
  - Include auth/storage/config-source combinations in each variant.
  - Include artifact differences, operational tradeoffs, and recommended profile.

## Observability and Architecture Outputs

- Always generate scenario-specific metrics mapped to the deployed flow (throughput, latency, error and drop counters, backlog/queue depth, and protocol- or feature-specific KPIs).
- Always provide two dashboard options:
  - Grafana-ready panel and query specification (or JSON model when requested).
  - MAPS-hosted dashboard view specification (REST/WS backed) for local-first operation without external dependencies.
- Support both quick and deep modes for observability:
  - Simple local mode: one minimal dashboard and 3 to 6 core metrics.
  - Advanced mode: multi-pane dashboard with per-protocol/component metrics, alerts, and drill-down views.
- Always generate C4 architecture diagrams (Context and Container minimum; Component when useful) that visualize the exact deployed flow, protocol boundaries, and data movement paths.
- Include one diagram suited for local test topology and one diagram for advanced/production topology when both are discussed.

## Reference Loading

Load only what is needed:
- Target packaging patterns: `skills/maps-deployment-packager/references/deployment-targets.md`
- Final response structure: `skills/maps-deployment-packager/references/output-contract.md`
