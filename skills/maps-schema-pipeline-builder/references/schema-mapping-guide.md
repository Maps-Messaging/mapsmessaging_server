# Schema Mapping Guide

## Scope

Use this guide when a request requires schema registration, schema assignment, content-type controls, validation, or format conversion.

Supported payload families in MAPS context:
- JSON
- Protobuf
- Avro
- CSV
- Raw binary
- Raw text

## Configuration Surfaces

- `/Users/krital/dev/starsense/mapsmessaging_server/SchemaManager.yaml`
  - schema catalog and registration behavior.
- `/Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml`
  - namespace-level message overrides (`schemaId`, `contentType`, QoS, retain).
- `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml`
  - protocol-level message defaults and content-type baselines.

## Mapping Patterns

1. Schema registration request
- Target `SchemaManager.yaml` entries.
- Ensure schema IDs are deterministic and referenced where needed.

2. Namespace-specific schema enforcement
- Target `DestinationManager.data[].messageOverride.schemaId`.
- Pair with `contentType` when format is explicit.

3. Protocol ingress default format
- Target `NetworkManager...protocolConfigs[].messageDefaults.contentType`.
- Keep this as protocol default; override at destination only when required.

4. Format conversion pipeline
- Use configured transformation chain in explicit order.
- Validate resulting `contentType` and `schemaId` at egress destination.

## Validation Checklist

```bash
rg -n "schemaId:" /Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml
rg -n "contentType:" /Users/krital/dev/starsense/mapsmessaging_server/DestinationManager.yaml /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
rg -n "schema|Schema" /Users/krital/dev/starsense/mapsmessaging_server/SchemaManager.yaml
```

## Common Failure Classes

- Schema reference present but not registered in schema manager.
- Content type mismatch between ingress default and destination override.
- Transformation order mismatch causes invalid payload shape at destination.
