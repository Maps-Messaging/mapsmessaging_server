# ML Pipeline Patterns

## Core Principle

Always propose a simple baseline first, then advanced chained variants.

## Baseline Pattern

Single-pass stream:
- Input topic filter
- One selector with one model expression
- One output topic (for outliers/decisions)

Use when:
- fast local validation is needed
- model behavior must be easy to explain

## Advanced Multi-Pass Pattern

Stage-by-stage chain using intermediate MAPS destinations:
- Stage 1: broad anomaly or classification filter to intermediate topic
- Stage 2: refinement model on intermediate topic
- Stage 3 (optional): specialized model or policy selector to final action topic

Guidance:
- keep stages explicit and named
- ensure each stage has a deterministic topic contract
- do not hide transformations between stages

## Example Chaining Skeleton

- `stage1-input`: `/sensor/raw/#`
- `stage1-output`: `/ml/intermediate/stage1`
- `stage2-input`: `/ml/intermediate/stage1`
- `stage2-output`: `/ml/intermediate/stage2`
- `stage3-input`: `/ml/intermediate/stage2`
- `stage3-output`: `/ml/final/actions`

## Validation Checklist

```bash
rg -n "eventStreams|selector|topicFilter|outlierTopic|schemaId|retrainThreshold" /Users/krital/dev/starsense/mapsmessaging_server/MLModelManager.yaml
rg -n "namespace: |namespaceMapping:" /Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml
```

## Failure Classes

- Selector expression does not match stream schema.
- Intermediate topics not provisioned/mapped.
- Multi-pass chain loops back unintentionally.
- Retraining threshold too sensitive and causes churn.
