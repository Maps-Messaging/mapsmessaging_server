# Deployment Targets

## Deployment Dimension Matrix

Every deployment profile must declare all dimensions:
- Platform: local, Docker, Kubernetes-style, Fly.io, AWS, Google Cloud, Azure
- Connectivity: connected, air-gapped, degraded, delay-tolerant
- Authentication: selected MAPS-supported auth mechanism(s) for this deployment (for example htpasswd, LDAP, JWT, Auth0, token-based)
- Storage and volumes: ephemeral or persistent; file/memory/tier mix; volume mount strategy
- Config source: local file-based config or Consul-based config
- Backend option: Fly KV, no dedicated backend, or dedicated Consul cluster
- Consul topology (if Consul mode): HCP Consul or self-managed Consul
- Object storage option (if used): AWS S3 or S3-compatible provider (for example Cloudflare R2)

## Local Runtime Packaging

Inputs:
- manager YAML files in repo root or `src/main/resources`
- runtime env: `MAPS_HOME`, `MAPS_CONF`, `MAPS_DATA`
- explicit auth/storage/config-source selections

Execution references:
- `src/main/scripts/start.sh`
- `src/main/scripts/start.bat`

Output expectations:
- complete file set for conf directory
- startup command block
- post-start diagnostics

## Docker Packaging

Execution references:
- `buildDocker.sh`
- `src/main/scripts/startDocker.sh`
- `src/main/scripts/docker_run.sh`

Output expectations:
- image tag and run command
- mounted config map (host path -> `/opt/maps/conf/...`)
- port mappings and env vars
- auth and storage env/volume mappings
- startup diagnostics (`docker logs`, listener checks)

## Cloud Target Packaging

### Fly.io
- Generate app and service deployment bundle with mapped ports, env, volumes, and config source mode.
- Offer Fly.io KV profile as a first-class default for lightweight state/config needs without running a Consul cluster.
- If Consul-based mode is selected, treat it as an explicit override and include dedicated Consul topology choice.
- Make Fly build context explicit:
  - if deploying from the Fly bundle directory, use `dockerfile = "Dockerfile"` in `fly.toml`
  - if deploying from repo root, use a repo-root-relative Dockerfile path
  - Dockerfile `COPY` lines must be relative to the chosen build context
- Make architecture selection explicit:
  - include `TARGETARCH` behavior in Fly build args
  - when only x86 MAPS images are available, force `TARGETARCH=amd64` even when the operator runs on Apple Silicon/arm64

### AWS
- Generate container deployment profile (task/service style) including volume/storage class and auth/config parameters.

### Google Cloud
- Generate container service profile with runtime env, mounts, and config source wiring.

### Microsoft Azure
- Generate container app/service profile with env, volumes, and config-source mode.

For all cloud targets:
- include authentication profile mapping
- include storage/volume profile mapping
- include file vs Consul configuration mode mapping
- include backend option mapping (Fly KV, dedicated Consul, or none)
- if dedicated Consul is used, include topology (HCP or self-managed), addresses, and bootstrap/join details
- if object storage is used, include provider, endpoint style, bucket strategy, and credentials mapping
- include startup and listener diagnostics commands adapted to target runtime

## Consul Cluster Options

Dedicated Consul may be offered for any edge or cloud deployment target.

### HCP Consul
- Use managed control plane endpoints and ACL/bootstrap parameters from HashiCorp Cloud.
- Include datacenter, token handling, TLS settings, and agent/client connectivity details.

### Self-Managed Consul
- Include server count/profile, gossip/LAN/WAN ports, bootstrap-expect, and storage placement.
- Include upgrade and failure-domain assumptions (edge single-site vs regional multi-site).

## Object Storage Options

Use when deployment requires durable off-box artifacts, archives, or replay staging.

- AWS S3
  - native AWS auth path and region-aware bucket configuration
- S3-compatible providers (for example Cloudflare R2)
  - explicit endpoint URL, signature compatibility assumptions, and credential variables

## Air-Gapped Packaging

Generate an offline-ready bundle containing:
- manager YAML files
- startup scripts and env file
- prebuilt image reference and load instructions (no remote pull at deploy time)
- dependency manifest with checksums

Operational expectations:
- deployment must succeed without internet connectivity
- all runtime references must resolve locally

## Degraded / Delay-Tolerant Packaging

Design for intermittent links and temporal delivery:
- configure persistent storage and buffering paths in destination-related manager files
- document backlog and retry expectations
- include reconnect and replay verification commands

Recommended checks:
- simulate producer while downstream path is unavailable
- restore path and verify buffered events are forwarded

## Kubernetes-Style Packaging

No native Helm manifests are required for this skill.
Generate:
- ConfigMap-ready YAML sections for manager files
- container env/args aligned with MAPS startup conventions
- auth/storage/config-source explicit values
- apply/check commands for config-only deployment bundles

## Validation Checklist

```bash
rg -n "MAPS_HOME|MAPS_CONF|MAPS_DATA|java.security.auth.login.config|Consul" src/main/scripts/start.sh src/main/scripts/startDocker.sh
rg -n "NetworkManager|DestinationManager|routing|AggregatorManager|AuthManager" *.yaml
rg -n "directory:|storageConfig:|type: File|type: Memory|autoPauseTimeout" DestinationManager.yaml
```

## Failure Classes

- Missing manager file in package bundle.
- Mount path mismatch causes runtime to use baked-in defaults.
- Port mapping mismatch between container and host checks.
- Environment variables unresolved at startup.
- Air-gapped install references remote artifact endpoints.
- Buffer-and-forward profile uses non-persistent storage where persistence is required.
- Authentication profile declared but not wired in deployment env/config.
- File vs Consul mode mismatch between deployment commands and runtime flags/env.
- Fly KV mode selected but deployment still depends on unavailable Consul endpoint.
- Fly Dockerfile path resolved relative to wrong working directory (for example duplicated subpath segments).
- Fly Docker build context mismatch causes `COPY` source file not found at build time.
- Fly build auto-selects unsupported architecture image tag when only x86 artifacts are available.
- Consul mode selected without explicit topology (HCP vs self-managed) and connection details.
- Object storage selected without provider endpoint and credential mapping details.
