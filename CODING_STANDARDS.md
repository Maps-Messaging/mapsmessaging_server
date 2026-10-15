# MapsMessaging Java Coding Standards

**Status:** Agreed
**Scope:** Java production code, tests, and supporting infrastructure in MapsMessaging.
**Baseline:** The Java version configured by the repository build. Do not introduce features requiring a newer language level.

## Principles and scope

New code should look and behave as though it belongs in the existing MapsMessaging codebase. Prioritise correctness, clarity, maintainability, and predictable runtime behaviour over brevity or individual preferences.

Apply these standards to new code and code being substantively modified. Follow surrounding conventions where they do not contradict this document. Do not reformat the entire repository or unrelated sections of a file. Keep cosmetic-only changes in separate commits from behavioural changes.

## Explicit types

Do **not** use Java local-variable type inference (`var`) in production code or tests, including enhanced loops and local declarations. State the type explicitly and prefer interfaces when appropriate.

```java
DeliveryStateCoordinator coordinator = deliveryStateCoordinator;
List<Integer> packetIds = new ArrayList<>();
Map<Integer, Message> messages = new HashMap<>();
```

## Formatting and control flow

- Use two-space indentation and spaces rather than tabs.
- Use a **soft target of 180 columns**. Do not wrap merely because a line exceeds that target by one or a few characters. Break lines where it improves comprehension, not to meet a mechanical threshold.
- Put opening braces on the same line as the statement or declaration.
- Use braces for all `if`, `else`, `for`, `while`, `do`, and other control-flow bodies, even one-statement bodies.
- Expand `try`, `catch`, `finally`, and conditional blocks across multiple lines. Do not compress multiple independent statements onto one line.
- Use consistent spacing around operators, after commas, and between logical sections. Avoid drive-by formatting of unrelated code.

```java
if (message == null) {
  return;
}

try {
  processMessage(message);
} catch (IOException exception) {
  logger.error("Unable to process message", exception);
  throw exception;
}
```

## Imports and naming

Use normal imports and simple type names. Avoid fully qualified Java class names inside method bodies unless needed to disambiguate types. Prefer explicit imports to wildcard imports in new code.

Give classes, fields, methods, parameters, and local variables descriptive names. Avoid unclear one-letter names and unnecessary abbreviations. Conventional, tightly scoped identifiers are acceptable where unambiguous. Preserve established public API names and recognised protocol terminology.

## Methods, features, and class organisation

Create focused helper methods when they clarify a meaningful operation, express a domain concept, reduce duplication, or make resource ownership or control flow easier to understand. Do not introduce trivial wrappers solely to shorten methods. Keep related lifecycle operations grouped.

Modern Java features such as records, switch expressions, pattern matching, and text blocks are permitted where they improve readability and remain compatible with the configured Java version. Prefer loops over streams when ordering, mutation, exceptions, or debugging are clearer with a loop. Do not use `var`.

Use Lombok consistently with surrounding code; do not add it indiscriminately.

## Defensive programming

**Agreed policy:** Validate aggressively at external and untrusted boundaries, but rely on explicit, enforced contracts inside the application.

- Validate network/protocol input, configuration, persisted state, and other externally controlled values.
- Establish internal invariants through constructors, API contracts, and appropriate state transitions.
- Do not scatter redundant null checks and arbitrary defaults through internal code to hide broken invariants.
- Use explicit failures for violated internal contracts rather than silently continuing with invalid state. Choose a meaningful exception type and preserve context.
- Use fallback behaviour only when it is part of a defined protocol, compatibility, or recovery contract.
- Differentiate malformed external input (which should normally be handled and isolated) from programmer errors (which should remain visible).
- Do not add defensive branches merely to satisfy static analysis if they weaken the stated contract.

## Concurrency and lifecycle

Document important ownership, synchronization, reference publication, callback, shutdown, recovery, and replacement assumptions where they are not obvious.

Use `volatile` for visibility and safe publication when appropriate; it does not make compound operations atomic or mutable referents thread-safe. Use synchronization, atomic types, and concurrent collections only when they provide the actual required guarantees. Read a changeable reference once into a local variable if a null check and later operation must act on the same object.

Prefer invoking externally supplied callbacks outside internal locks when safe. Capture the necessary state under lock, preserve lifecycle semantics, and test ordering, closure, recovery, and replacement paths.

## Exceptions and resources

Preserve original exceptions and their causes. Never silently swallow meaningful operational errors or mask the primary exception during cleanup. Use explicit error handling at a level where the failure can be handled meaningfully.

Prefer try-with-resources for locally owned resources. When ownership transfers to a longer-lived component, document the hand-off and close the resource on failed initialization. Never close a resource after a successful ownership transfer merely to satisfy a static-analysis warning.

## Tests

Prefer constructor-valid instances, existing test builders, and reusable fixtures for stateful components. Mock external collaborators as needed, but avoid partial mocks that bypass essential initialization (including `CALLS_REAL_METHODS`) unless specifically justified and fully initialized.

Tests should verify observable behaviour and preserve meaningful assertions. Regression tests should reproduce the defect and verify the fix; do not disable or weaken failing tests to make a build green.

Use established test data locations such as `target` or `target/data`. For concurrent behaviour, test lifecycle, ordering, and completion rather than depending solely on fixed sleeps.

## Comments and documentation

Explain purpose, non-obvious decisions, invariants, ownership, compatibility requirements, and concurrency contracts. Do not narrate self-explanatory statements. Write Javadoc for public APIs and complex contracts where useful. Preserve the existing source licensing and copyright conventions.

## Static analysis

Treat SonarCloud and other static-analysis results as findings to investigate, not automatic proof of defects. Fix actual issues. For justified false positives, use a narrowly scoped suppression or accepted finding with a concise technical explanation. Do not distort working architecture merely to achieve a passing grade.

## Changes, commits, and validation

- Keep formatting-only refactors separate from behaviour changes. Preserve interfaces, execution order, exceptions, concurrency, and lifecycle semantics when formatting.
- Keep commits focused and use the repository's established commit-message and Jira-reference conventions.
- Inspect diffs for accidental unrelated changes.
- Verify compilation and run focused tests for relevant changes; run broader regression suites when warranted.
- Report tests and builds as passing only when results have been observed.
- Do not create branches, Jira issues, run Jenkins, or merge without the required approval.

## Adoption

These standards govern future and substantively modified Java code. Clean up existing code gradually when touching the relevant area; do not initiate a repository-wide reformat. If an existing public interface or compatibility requirement conflicts with a style preference, preserve behaviour and document the exception.
