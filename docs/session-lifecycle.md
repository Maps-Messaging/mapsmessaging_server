# Session lifecycle ownership and expiry

This document describes the session lifecycle implemented by `SessionManager`, `SessionManagerPipeLine`, `SessionImpl`, `PersistentSessionManager`,
`SubscriptionController`, and `SessionExpiryTask`.

The rules in this document are invariants, not implementation suggestions. They exist to prevent reconnect, expiry, duplicate-session, shutdown, Will, and persistence races.

## Ownership model

A session ID can have several related objects, but each object represents a different fact.

| Structure or object | Meaning |
| --- | --- |
| `SessionManagerPipeLine.sessions` | The currently active `SessionImpl` for a session ID. |
| `SessionManagerPipeLine.persistentControllers` | The persistent `SubscriptionController` owned by a session ID. |
| `SessionManagerPipeLine.disconnectedControllers` | Persistent controllers that are currently disconnected and waiting for reconnect, reset, expiry, or administrative close. |
| `PersistentSessionManager` | Persisted `SessionDetails` and the backing state file identity. |
| `SessionExpiryTask` | The complete expiry operation, from timer wait through queued pipeline cleanup completion. |

Persistent ownership and disconnected state are deliberately separate. A persistent controller may exist while its session is active.

## Single teardown mechanism

`SessionManagerPipeLine` owns `SubscriptionController` destruction.

`SessionImpl.close()` closes only resources owned by the active session, such as authentication state, keep-alive scheduling, closure tasks, and namespace mappings.
It must not close or shut down the `SubscriptionController`.

All final controller destruction passes through `SessionManagerPipeLine.finaliseController(...)`.

Do not add another direct `SubscriptionController.close(false)` lifecycle path in `SessionImpl`, protocol code, JMX code, test helpers, expiry callbacks, or shutdown code.

This rule is intentionally covered by `SessionImplLifecycleTest`.

## Pipeline serialization

A session ID is assigned to one `SessionManagerPipeLine`. Creation, close, reconnect, reset, expiry cleanup, administrative close, and shutdown cleanup for that ID
must execute through that pipeline's serial executor.

The global expiry scheduler only determines when expiry becomes eligible to run. It does not own session cleanup.

`SessionExpiryTask` bridges the timer and the pipeline executor so its `Future` represents actual cleanup completion rather than merely the timer callback firing.

## Active session identity

Removing an active session must be identity-safe:

```java
sessions.remove(sessionImpl.getName(), sessionImpl)
```

A stale `SessionImpl` must never remove, close, or decrement accounting for a replacement instance with the same session ID.

Duplicate creation closes the previous active session through the normal pipeline lifecycle before registering the replacement.

## Persistent controller identity

Persistent controller removal must also be identity-safe:

```java
persistentControllers.remove(sessionId, controller)
```

A stale expiry callback must never remove a replacement controller that now owns the same session ID.

If ownership has moved, stale cleanup is a no-op.

## Lifecycle matrix

| Situation | Active session | Persistent controller | Disconnected | Persistence | Will |
| --- | --- | --- | --- | --- | --- |
| New transient session | present | none | no | none | active if configured |
| New persistent session | present | present | no | retained | active if configured |
| Transient close | removed | none | no | none | cleared or scheduled according to close reason |
| Persistent close with expiry | removed | retained | yes | retained | cleared or scheduled according to close reason |
| Reconnect before expiry | present again | same controller | no | retained | normal active lifecycle |
| Expiry wins before reconnect | absent | removed | no | deleted | pending Will is forced |
| Reset-state reconnect | present after replacement | replacement controller | no | retained with subscriptions cleared | not treated as expiry |
| Administrative close | absent after completion | removed | no | deleted | cleared or finalized according to request |
| Shutdown | absent after completion | removed | no | terminal cleanup through the same lifecycle | cleared for shutdown |

## Connected, disconnected, and expired counters

Counters follow successful ownership transitions. They are not independent state.

- `connectedSessions` increments when an active session is registered and decrements only when that exact active instance is removed.
- `disconnectedSessions` increments only when a controller first enters `disconnectedControllers`.
- `disconnectedSessions` decrements only when that controller is actually removed from `disconnectedControllers`.
- `expiredSessions` increments only when expiry actually owns and finalizes the controller.

Repeated or stale cleanup must not change counters.

## Persistent versus transient expiry

Only `PersistentSession` instances may enter disconnected expiry handling.

A transient session closes immediately even if a protocol supplied a positive expiry value.

This prevents a transient controller from being closed by `SessionImpl` and then incorrectly hibernated or scheduled for expiry.

## Reconnect versus expiry ordering

Two orderings are valid and explicitly tested.

### Reconnect wins

1. The session is disconnected and has a pending `SessionExpiryTask`.
2. The reconnect reaches the pipeline before expiry cleanup.
3. The expiry task is cancelled.
4. The same persistent controller is restored.
5. Disconnected accounting is cleared.
6. Queued stale cleanup cannot later remove the restored controller.

### Expiry wins

1. The expiry cleanup reaches the pipeline first.
2. The old persistent controller is finalized.
3. Persistence is removed.
4. Expired accounting increments.
5. A later reconnect creates a fresh controller from fresh session state.

Neither ordering is an error. The pipeline execution order decides ownership.

## Reset-state reconnect

Reset is not expiry and is not terminal session destruction.

Reset finalizes the old controller without:
- incrementing the expired counter;
- forcing a pending Will;
- deleting the persistent session identity.

Persisted subscriptions are cleared and a replacement controller is created.

## Will handling

Graceful and administrative lifecycle requests may clear the Will. Ungraceful disconnect may schedule it.

When a persistent disconnected session finally expires, final teardown forces any remaining pending Will by cancelling its delay and running it.

If `clearWillTask=true` is requested for an already disconnected session, the Will must be removed before terminal controller finalization and must not run.

## Persistence deletion

Terminal or expired controller finalization removes both:
- the in-memory `SessionDetails` from `PersistentSessionManager`;
- the corresponding `.bin` state file when a valid unique ID exists.

Reset does not delete the persistent identity.

File deletion failure must not prevent controller cleanup or corrupt lifecycle accounting.

## Shutdown

Shutdown does not have a special controller-destruction shortcut.

Active sessions and already-disconnected persistent sessions are closed through the same pipeline lifecycle before the pipeline executor is shut down.

This ensures shutdown cannot leave transient controllers open or bypass Will, persistence, and counter rules.

## Required regression coverage

The focused lifecycle tests must continue to cover:

- new session creation;
- duplicate active replacement without connected-count inflation;
- stale active close cannot remove a replacement;
- persistent disconnect and scheduled expiry;
- transient positive expiry closes immediately;
- reconnect before expiry;
- timer firing while reconnect is already ahead of queued cleanup;
- expiry winning before reconnect;
- reset-state reconnect;
- restored disconnected session expiry;
- already-expired restored session cleanup;
- duplicate cleanup idempotency;
- persistence deletion and deletion failure;
- persistent Will scheduling and forced final execution;
- clearing Will on an already-disconnected administrative close;
- shutdown of active transient and disconnected persistent sessions;
- `SessionImpl.close()` never destroying the `SubscriptionController`;
- thousands of independent transient sessions connecting and disconnecting without state or counter leakage;
- concurrent producers queueing thousands of lifecycle operations onto one pipeline without lost sessions;
- mass persistent expiry finalizing every controller, persistence record, and pending Will exactly once;
- repeated reconnect/disconnect churn on one persistent session without counter drift or stale expiry cleanup;
- `SessionExpiryTask` cancellation before timer, after timer but before cleanup, cleanup completion semantics, and rejected submission.

## Review checklist

When changing session lifecycle code, verify all of the following before merge:

1. There is still only one production controller-destruction mechanism.
2. Active and persistent ownership removals remain identity-safe.
3. No protocol, JMX operation, test helper, shutdown path, or expiry callback closes a controller directly.
4. Reconnect and expiry ordering remain deterministic on the pipeline executor.
5. Counter updates are tied to successful state transitions.
6. Reset remains distinct from expiry and terminal close.
7. Terminal cleanup removes persistence exactly once.
8. Will behavior is explicit for graceful close, ungraceful close, expiry, administrative close, and shutdown.
9. Focused session lifecycle tests pass.
10. The complete Jenkins suite passes before merge.

## Rationale

The lifecycle is intentionally centralized because the difficult failures are not simple close operations. They are ordering and ownership failures between reconnect, expiry, replacement,
shutdown, persistence, and delayed Will execution.

Adding a second teardown path may look simpler locally, but it creates another place that must correctly coordinate all of those concerns. The design therefore prefers one explicit lifecycle
mechanism with deterministic tests over multiple locally convenient cleanup paths.
