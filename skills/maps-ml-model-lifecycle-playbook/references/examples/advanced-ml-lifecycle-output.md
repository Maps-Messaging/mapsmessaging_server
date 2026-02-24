# Advanced ML Lifecycle Output Example

## Lifecycle Requirement Mapping
- Hybrid lifecycle:
  - external model bootstrap
  - stream-based retrain thresholds
  - staged inference pass-1 and pass-2 with intermediate destination
- File targets:
  - `/Users/krital/dev/starsense/mapsmessaging_server/MLModelManager.yaml`
  - `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml`
  - `/Users/krital/dev/starsense/mapsmessaging_server/routing.yaml`

## Lifecycle Shape
- Simple baseline retained.
- Advanced lifecycle option:
  1. external-model load
  2. stage-1 selector `/ml/in` -> `/ml/intermediate`
  3. stage-2 selector `/ml/intermediate` -> `/ml/final` or `/ml/outlier`
  4. retrain trigger path from stream counters

## Model and Store Assumptions
- Model type: `RandomForest` (external bootstrap), optional downstream `DecisionTree` retrain.
- Store mode: `s3` or `maps` in advanced mode; `file` fallback in local mode.
- Artifact formats: portable json/csv only.
- Smile 4.3.0 compatibility assumptions required before activation.
- Incompatible external artifacts route to fallback model and emit warning.

## Deployable Config Entity
```yaml
MLModelManager:
  type: MLModelManagerConfigDTO
  modelStore:
    type: maps
  eventStreams:
    - id: ml-advanced-stage1
      topicFilter: /ml/in
      schemaId: ml-schema-v2
      selector: "featureA > 0.4"
      outputTopic: /ml/intermediate
      maxTrainEvents: 5000
      retrainThreshold: 0.08
    - id: ml-advanced-stage2
      topicFilter: /ml/intermediate
      schemaId: ml-schema-v2
      selector: "confidence >= 0.7"
      outputTopic: /ml/final
      outlierTopic: /ml/outlier
```

## Apply Steps
```bash
cp /Users/krital/dev/starsense/mapsmessaging_server/MLModelManager.yaml /Users/krital/dev/starsense/mapsmessaging_server/MLModelManager.yaml.bak
# apply advanced lifecycle patch and restart runtime
```

## Verification
- Stage-by-stage checks:
  - model load
  - stage1 and stage2 output
  - retrain trigger observation
- External-model negative check:
  - incompatible artifact should fail validation and fallback.
```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_skill_smoke.sh
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_mqtt_smoke.sh --source-topic /ml/in --stage1-topic /ml/intermediate --final-topic /ml/final --outlier-topic /ml/outlier
```

## Risk and Operational Notes
- Multi-stage lifecycle increases operational complexity.
- Drift and retrain instability can increase false positives.
- Always retain rollback model reference and cutoff criteria.

## Scenario Metrics and Dashboard
- Metrics:
  - per-stage inference latency
  - model cache hit ratio
  - retrain trigger frequency
  - fallback model usage
  - outlier queue size
- Grafana: per-stage lifecycle dashboard with drift alerts.
- MAPS-hosted dashboard: retrain/fallback event stream.

## C4 Architecture Diagram
```mermaid
graph LR
  A[Input Stream] --> B[External Model Load]
  B --> C[Stage1 Inference]
  C --> D[/ml/intermediate]
  D --> E[Stage2 Inference]
  E --> F[/ml/final]
  E --> G[/ml/outlier]
  C --> H[Retrain Trigger]
```
