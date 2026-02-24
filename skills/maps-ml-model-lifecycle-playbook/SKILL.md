---
name: maps-ml-model-lifecycle-playbook
description: Guide MAPS-native ML model lifecycle workflows from streamed data training to externally trained model ingestion for streaming inference. Use when requirements include model creation from event streams, model store strategy, retraining policy, external model onboarding, staged inference, and production-safe validation.
---

# MAPS ML Model Lifecycle Playbook

Provide clear, executable guidance for both model creation from streaming data and online inference with externally trained models, always within MAPS capabilities.

## Workflow

1. Normalize lifecycle contract.
- Extract objective (classification, anomaly, clustering, regression), source streams, schema IDs, training window assumptions, retraining triggers, and inference outputs.
- Classify mode:
  - stream-trained model pipeline
  - externally trained model ingestion pipeline
  - hybrid lifecycle

2. Design lifecycle blueprint.
- Read `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/references/lifecycle-patterns.md`.
- Build simple baseline first, then advanced staged variant when requested.
- Keep model-store type explicit (`file`, `nexus`, `s3`, `maps`).

3. Map to deployable config.
- Primary ML stream config in `/Users/krital/dev/starsense/mapsmessaging_server/MLModelManager.yaml`.
- Include destination/schema/routing changes when staged inference or quarantine flows are required.
- Include external model artifact ingest mapping for portable formats only.

4. Validate lifecycle behavior.
- Validate training and inference stage boundaries.
- Validate model-store and artifact metadata wiring.
- Validate retrain threshold semantics and rollback behavior.
- Include stage-by-stage runtime verification commands.
- Prefer bundled scripts for repeatable validation:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_skill_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_runtime_smoke.sh`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_vectors.py`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_mqtt_smoke.sh`

5. Return using output contract.
- Follow `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/references/output-contract.md`.
- Include simple baseline and advanced option(s) when requested.

## ML Lifecycle Rules

- Always present a simple working lifecycle first.
- Keep advanced chains bounded by default (2 to 3 passes) unless user requests more.
- Use portable model artifacts; do not use Java object serialization.
- Include Smile 4.3.0 compatibility assumptions in model-related guidance.
- For external model ingestion, include artifact format checks and compatibility fallback notes.

## Scenario Modes

- `Simple Local Default`:
  - One stream-trained or external-model inference flow with one output topic and one verification path.
- `Advanced Combination Matrix`:
  - Provide 3 to 6 variants across training/inference split, retraining policy, staged selectors, and model-store options.
  - Recommend one operationally simplest production path.

## Observability and Architecture Outputs

- Always generate scenario-specific ML lifecycle metrics:
  - train event count, retrain trigger frequency, model load success rate, inference latency, selector hit rate, outlier rate.
- Always provide two dashboard options:
  - Grafana-ready panel/query definitions.
  - MAPS-hosted dashboard view specification.
- Support both quick and deep observability:
  - Simple mode: one lifecycle health panel and 3 to 6 core metrics.
  - Advanced mode: per-stage training/inference lifecycle dashboard with drift alerts.
- Always generate C4 diagrams (Context and Container minimum; Component for multi-stage or hybrid lifecycle).

## Reference Loading

Load only what is needed:
- Lifecycle patterns and constraints: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/references/lifecycle-patterns.md`
- Final response format: `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/references/output-contract.md`
- Example artifacts:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/references/examples/simple-ml-lifecycle-output.md`
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/references/examples/advanced-ml-lifecycle-output.md`
