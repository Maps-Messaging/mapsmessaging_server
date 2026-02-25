# Selector Patterns

## Pattern A: Simple Routing Selector

- One source topic.
- One condition.
- Two destination paths: matched and unmatched.

## Pattern B: Subscriber Selector

- Shared source topic.
- Distinct selectors per subscriber class.
- Verify each subscriber receives only intended messages.

## Pattern C: Chained Selectors

- Selector stage 1 routes into intermediate destination.
- Selector stage 2 refines output to final destinations.
- Keep stage boundaries explicit and testable.

## Conflict Handling

- Define precedence when selectors overlap.
- Document expected behavior for ambiguous payloads.

## Verification Guidance

- Provide positive vectors (should pass).
- Provide negative vectors (should fail).
- Include timeout-bound consume checks.

## Runnable Smoke Commands

Skill smoke gate:

```bash
bash skills/maps-selector-rule-engineer/scripts/run_selector_skill_smoke.sh
```

Runtime selector smoke (subscribe before publish):

```bash
bash skills/maps-selector-rule-engineer/scripts/run_selector_mqtt_smoke.sh --source-topic /selector/in --match-topic /selector/match --nonmatch-topic /selector/nonmatch
```
