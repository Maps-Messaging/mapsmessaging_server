# ML Lifecycle Patterns

## Pattern A: Stream-Trained Baseline

- Input stream feeds training event collection.
- Model updates at configured thresholds.
- Inference output goes to one downstream topic.

## Pattern B: External Model Ingestion

- External portable model artifact is loaded into MAPS model store.
- Inference runs on live stream with selector/output routing.
- Include compatibility checks and fallback behavior.

## Pattern C: Hybrid Lifecycle

- Start with external model.
- Enable MAPS retraining from stream once confidence thresholds are met.
- Preserve rollback to prior model version.

## Artifact and Compatibility Rules

- Use portable artifact formats only.
- Validate Smile 4.3.0 compatibility assumptions.
- Avoid Java serialization formats.

## Verification Guidance

- Stage-wise verification: model load, inference output, retrain trigger, post-retrain inference.
- Include negative case for incompatible model artifact.

## Runnable Smoke Commands

Skill smoke gate:

```bash
bash skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_skill_smoke.sh
```

Vector checks (portable format + serialization constraints):

```bash
python3 skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_vectors.py
```

Runtime staged ML smoke:

```bash
bash skills/maps-ml-model-lifecycle-playbook/scripts/run_ml_lifecycle_mqtt_smoke.sh --source-topic /ml/in --stage1-topic /ml/stage1 --final-topic /ml/final --outlier-topic /ml/outlier
```
