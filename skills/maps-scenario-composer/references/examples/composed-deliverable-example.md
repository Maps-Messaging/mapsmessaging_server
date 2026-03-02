# Composed Deliverable Example

## Composition Matrix
- mode: hybrid
- sources:
  - maps-deployment-packager
  - maps-selector-rule-engineer
  - maps-geospatial-routing-builder

## Unified Assumptions
- one runtime, shared config surfaces, explicit precedence.

## Merged Deployable Entity
```yaml
mergedArtifacts:
  - sourceSkill: maps-deployment-packager
  - sourceSkill: maps-selector-rule-engineer
  - sourceSkill: maps-geospatial-routing-builder
```

## Unified Apply Sequence
```bash
# apply merged artifacts, restart once, verify startup
```

## Integrated Verification
- startup/listener checks
- selector checks
- geospatial near/far/invalid checks
- cross-scenario route checks

## Failure Domain and Rollback
- per-slice failure map with merged rollback steps.

## Traceability Map
- merged section -> `skills/maps-deployment-packager/SKILL.md`
- merged section -> `skills/maps-selector-rule-engineer/SKILL.md`
- merged section -> `skills/maps-geospatial-routing-builder/SKILL.md`

## Scenario Metrics and Dashboard
- combined metrics and cross-scenario health pane.

## C4 Architecture Diagram
```mermaid
graph LR
  A[Ingress] --> B[Composed Runtime] --> C[Egress]
```
