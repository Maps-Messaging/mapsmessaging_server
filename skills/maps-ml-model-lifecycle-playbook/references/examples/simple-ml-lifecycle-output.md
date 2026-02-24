# Simple ML Lifecycle Output Example

## Lifecycle Requirement Mapping
- Stream-trained baseline with one selector stage and one output destination.
- Optional external model artifact load for warm start.
- File targets:
  - `/Users/krital/dev/starsense/mapsmessaging_server/MLModelManager.yaml`
  - `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml`

## Lifecycle Shape
- Simple baseline:
  - Input stream `/ml/in`
  - Selector stage output `/ml/stage1`
  - Final output `/ml/final`
- Advanced options not enabled in this profile.

## Model and Store Assumptions
- Model type: `DecisionTree`.
- Store mode: `file`.
- Artifact format: portable json/csv.
- Smile 4.3.0 compatibility assumptions are explicitly required.
- External model ingestion allowed from:
  - `/Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/references/examples/model-artifact-sample.json`

## Deployable Config Entity
```yaml
MLModelManager:
  type: MLModelManagerConfigDTO
  modelStore:
    type: file
    path: /Users/krital/dev/starsense/mapsmessaging_server/models
  eventStreams:
    - id: ml-simple-stage1
      topicFilter: /ml/in
      schemaId: ml-schema-v1
      selector: "featureA > 0.5"
      outputTopic: /ml/stage1
    - id: ml-simple-final
      topicFilter: /ml/stage1
      schemaId: ml-schema-v1
      selector: "label != null"
      outputTopic: /ml/final
```

## Apply Steps
```bash
cp /Users/krital/dev/starsense/mapsmessaging_server/MLModelManager.yaml /Users/krital/dev/starsense/mapsmessaging_server/MLModelManager.yaml.bak
# apply lifecycle patch and restart runtime
```

## Verification
- Stream verification:
  - publish to `/ml/in`, expect event at `/ml/stage1` and `/ml/final`.
- External-model compatibility verification:
  - validate artifact metadata and portable format.
```bash
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_skill_smoke.sh
bash /Users/krital/dev/starsense/mapsmessaging_server/skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_mqtt_smoke.sh --source-topic /ml/in --stage1-topic /ml/stage1 --final-topic /ml/final --outlier-topic /ml/outlier
```

## Risk and Operational Notes
- Retrain trigger frequency should be bounded to avoid thrashing.
- Drift monitoring required for sustained external-model accuracy.
- Rollback to previous model artifact on compatibility or quality failure.

## Scenario Metrics and Dashboard
- Metrics:
  - train event count
  - model load success rate
  - stage latency
  - selector hit rate
  - outlier rate
- Grafana: lifecycle health + stage latency panel.
- MAPS-hosted dashboard: load/retrain timeline and anomaly counters.

## C4 Architecture Diagram
```mermaid
graph LR
  A[Input Stream] --> B[Stage1 Selector]
  B --> C[Final Inference]
  C --> D[/ml/final]
  B --> E[/ml/outlier]
```
