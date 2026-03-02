---
name: maps-ml-stream-configurator
description: Build deployable MAPS ML stream configurations for selector-driven event processing, model store integration, and multi-pass inference pipelines. Use when requirements include ML selectors on streams, intermediate destination passes, staged model chaining, retraining thresholds, and production-safe verification with simple and advanced modes.
---

# MAPS ML Stream Configurator

Design ML-enabled MAPS stream pipelines that stay understandable: one simple baseline path first, then optional advanced multi-pass model chaining.

## Workflow

1. Normalize ML stream contract.
- Extract source topics, schema IDs, selector expressions, outlier or decision topics, model store type, caching policy, and retraining thresholds.
- Detect whether user needs single-pass or multi-pass model chaining.

2. Build pipeline shape in two layers.
- Layer A (simple): one model selector on one stream, one output topic.
- Layer B (advanced): staged passes using intermediate MAPS destinations with one model per pass.
- Read `skills/maps-ml-stream-configurator/references/ml-pipeline-patterns.md`.

3. Map to deployable config.
- Primary file: `MLModelManager.yaml`.
- Link to destination/schema config when intermediate stages or schema checks are required.
- Keep model-store type explicit (`file`, `nexus`, `s3`, `maps`) and include required config block details.

4. Validate before finalizing.
- Ensure each event stream has `id`, `topicFilter`, `schemaId`, `selector`, and output topic.
- Validate multi-pass chains have clear stage boundaries and deterministic topic handoff.
- Prefer bundled scripts for repeatable validation:
  - `skills/maps-ml-stream-configurator/scripts/run_maps_ml_stream_configurator_skill_smoke.sh`
  - `skills/maps-ml-stream-configurator/scripts/run_maps_ml_stream_configurator_runtime_smoke.sh`
- Include static checks:
```bash
rg -n "MLModelManager|eventStreams|selector|topicFilter|schemaId|outlierTopic|maxTrainEvents|retrainThreshold|modelStore" MLModelManager.yaml
```

5. Return with output contract.
- Follow `skills/maps-ml-stream-configurator/references/output-contract.md`.
- Include simple baseline and advanced option when requested.

## ML-Specific Rules

- Keep model pipelines deterministic and explain each stage in plain language.
- Always present a simple working baseline before advanced chain variants.
- For advanced pipelines, cap complexity by default (2 to 3 passes) unless user asks for more.
- Explicitly label each pass with input topic, selector model, and output topic.
- Keep serialization portable; do not rely on Java object serialization.
- Respect runtime constraints and mention Smile 4.3.0 compatibility assumptions for model-related guidance.

## Scenario Modes

- `Simple Local Default`:
  - One event stream selector and one output topic.
  - One locally testable publish/observe flow with minimal configuration.
- `Advanced Combination Matrix`:
  - Provide 3 to 6 variants across selector/model choice, pass count, intermediate topic design, and retraining behavior.
  - Include one recommended staged pipeline and explain why it is easier to operate.

## Observability and Architecture Outputs

- Always generate scenario-specific metrics mapped to ML stream behavior:
  - selector hit rate, outlier rate, false-positive review queue size, stage latency, model cache hit ratio, retrain trigger frequency.
- Always provide two dashboard options:
  - Grafana-ready panel/query definitions.
  - MAPS-hosted dashboard view specification.
- Support both quick and deep observability:
  - Simple mode: one health panel plus 3 to 6 core ML stream metrics.
  - Advanced mode: per-stage pipeline dashboard with alerts for drift or anomaly spikes.
- Always generate C4 diagrams (Context and Container minimum; Component for multi-pass) for the deployed ML flow.

## Reference Loading

Load only what is needed:
- Pipeline patterns and simplification guidance: `skills/maps-ml-stream-configurator/references/ml-pipeline-patterns.md`
- Final response structure: `skills/maps-ml-stream-configurator/references/output-contract.md`
