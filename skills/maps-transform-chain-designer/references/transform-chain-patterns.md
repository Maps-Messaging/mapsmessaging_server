# Transform Chain Patterns

## Core Principle

Transformation chains are ordered and deterministic. Each stage must define:
- Input format / contentType
- Transformation action
- Output format / contentType
- Optional schema impact

## Common Stage Types

1. CloudEvent wrapper
- Wrap payload to CloudEvent 1.0.2 structure.
- Usually early in chain when downstream expects envelope semantics.

2. JSON mutation
- Add, remove, or rename JSON fields.
- Keep idempotent where possible.

3. Format conversion
- JSON to XML, XML to JSON, or schema-based conversion.
- Must declare target contentType explicitly.

4. Enrichment
- Add derived fields (for example geohash).
- Requires source fields to exist before stage executes.

## Recommended Ordering Heuristics

- Validate/decode first.
- Envelope/wrap second.
- Mutate/enrich next.
- Final format conversion last.

If user mandates a different order, evaluate compatibility and flag impossible handoffs.

## Validation Commands

```bash
rg -n "contentType|schemaId|messageDefaults|messageOverride" NetworkManager.yaml DestinationManager.yaml
rg -n "transform|Transformation|CloudEvent|JSONToXML|XMLToJSON" src/main/java
```

## Failure Patterns

- contentType mismatch between stages.
- Schema validation failure after format conversion.
- Enrichment stage missing prerequisite fields.
- Destination expects transformed format but ingress defaults remain unchanged.
