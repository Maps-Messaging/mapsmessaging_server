# MAVLink bootstrap and twin state

## Scope
- Base branch: `development`
- Base commit: `9455754a92ffb5136e1d15f0efd749a5dc20d838`
- Agent branch: `review/state-mavlink-bootstrap`
- Production packages/classes reviewed:
  - `io.mapsmessaging.state.mavlink`: `MavlinkStateSubscriber`, `MavlinkTwinUpdater`, `MavlinkDroneMonitor`, `MavlinkSourceRegistry`
  - `io.mapsmessaging.state.mavlink.bootstrap`: `MavlinkBootstrapStateEngine`, `MavlinkBootstrapRequestTracker`, `MavlinkBootstrapProfile`, `MavlinkBootstrapState`, `MavlinkBootstrapEvent`, `MavlinkBootstrapEventType`, `MavlinkBootstrapRequestDefinition`, `MavlinkBootstrapRequestType`, `MavlinkBootstrapEventPublisher`, `DroneTwinReadinessEvaluator`, `DroneTwinReadinessEvaluation`, `DroneTwinReadinessResult`, `DroneTwinReadinessState`, `DroneTwinMissingState`
  - `io.mapsmessaging.state.mavlink.bootstrap.checks`: `IdentityReadinessCheck`, `AutopilotReadinessCheck`, `ConnectivityReadinessCheck`, `GlobalPositionReadinessCheck`, `GpsReadinessCheck`, `HomePositionReadinessCheck`, `BatteryReadinessCheck`, `SystemStateReadinessCheck`, `CapabilitiesReadinessCheck`, `LifecycleReadinessCheck`, `DroneTwinReadinessCheck`
- Existing tests reviewed:
  - No existing tests were found at the expected owned-scope paths for `MavlinkStateSubscriber`, `MavlinkTwinUpdater`, `MavlinkBootstrapStateEngine`, or `DroneTwinReadinessEvaluator` on the recorded base commit.
  - `MavlinkEventListSenderTest` was reviewed for the repository's current JUnit 5 and Mockito conventions only; sender logic remains excluded.
- Explicit exclusions:
  - `io.mapsmessaging.state.mavlink.listener*`
  - `io.mapsmessaging.state.mavlink.messages*`
  - `io.mapsmessaging.state.mavlink.packet*`
  - `io.mapsmessaging.state.mavlink.model*`
  - `io.mapsmessaging.state.mavlink.sender*`
  - MAVLink binary decoding, physical vehicles, live brokers, and live network services

The GitHub connector resolved the exact `development` head and created the assigned branch from that commit. A local `git rev-parse HEAD` was not possible because the execution environment could not resolve `github.com` and therefore could not clone the repository.

## Coverage
No JaCoCo report was generated after the changes. The execution environment contains Java 21 but does not contain Maven, and outbound DNS prevented obtaining a local checkout. The added tests therefore remain unexecuted in this environment; no coverage value is inferred from test count or source inspection.

| Metric | Baseline | After | Measurement scope |
|---|---:|---:|---|
| Lines | 0.0% | Not measured | Assigned packages; targeted tests authored but Maven unavailable |
| Branches | 0.0% | Not measured | Assigned packages; targeted tests authored but Maven unavailable |
| Methods | Not supplied | Not measured | Assigned packages; targeted tests authored but Maven unavailable |

## Logic reviewed
The review inspected the following invariants and paths:

- Bootstrap state starts at `UNKNOWN`, emits readiness changes once per actual transition, requests only configured missing-state messages, respects the two-second retry interval, stops after three attempts, emits a single timeout after 15 seconds, completes once on command readiness, and does not reopen a completed bootstrap session.
- Request trackers preserve the first request time, advance the last request time and attempt count, reject retries before the full interval, reject out-of-order clock values, and remain terminal after timeout.
- A resolved missing state must discard its request tracker. Otherwise an exhausted tracker survives recovery and prevents a later regression from being requested again.
- Registration requires MAVLink identity, vehicle class, autopilot type, and a fresh connected heartbeat. Command readiness additionally requires finite in-range position coordinates, a valid GPS flag and non-sentinel fix type, usable and fresh battery data, fresh position state, and autopilot capabilities.
- Missing home position and missing system-health data remain degraded advisory states and do not block command readiness under the current policy.
- Twin updates preserve the first non-empty response topic, update system/component and outbound-correlation state, refresh power freshness only for a valid battery packet, and ignore updates after closure.
- Readiness writes are observer-generated twin updates. They must not recursively re-evaluate and advance bootstrap state a second time for the same source update.
- Subscriber start and stop are idempotent, protocol cleanup still occurs when unsubscribe fails, an unstarted subscriber can be closed, monitor registration is removed once, and late messages after shutdown run only their completion task.
- Source matching is the exact MAVLink system/component pair. Duplicate configured keys retain the final configured entry, matching the current map construction semantics.

## Confirmed defects

| Severity | Class/method | Defect | Reproduction/regression test | Fix |
|---|---|---|---|---|
| High | `DroneTwinReadinessEvaluator.isCommandReady` | `STALE_POWER` was recorded but did not block command readiness, allowing stale battery telemetry to report `COMMAND_READY`. | `DroneTwinReadinessEvaluatorTest.missing_stale_or_invalid_battery_state_is_health_partial` | Added `STALE_POWER` to the command-readiness blockers. |
| High | `GlobalPositionReadinessCheck.evaluate` | Non-null `NaN`, infinite, or out-of-range coordinates were accepted as a global position. | `DroneTwinReadinessEvaluatorTest.non_finite_or_out_of_range_position_never_becomes_ready` | Added finite latitude/longitude validation and geographic range checks. |
| High | `GpsReadinessCheck.evaluate` | `gpsValid=true` combined with `NO_GPS`, `NO_FIX`, `UNKNOWN`, or blank fix text was accepted. | `DroneTwinReadinessEvaluatorTest.gps_flag_and_fix_type_must_both_represent_a_valid_fix` | Added explicit sentinel and blank fix rejection. |
| High | `BatteryReadinessCheck.evaluate` | Any non-null measurement, including `NaN`, infinity, zero voltage, or an out-of-range percentage, counted as usable battery state. | `DroneTwinReadinessEvaluatorTest.missing_stale_or_invalid_battery_state_is_health_partial` | Added finite and domain-specific measurement validation. |
| Medium | `MavlinkBootstrapStateEngine.update` | Request trackers were never removed when a state recovered. An exhausted tracker could therefore suppress requests after the same state regressed later. | `MavlinkBootstrapStateEngineTest.resolved_item_resets_exhausted_tracker_before_it_becomes_missing_again` | Remove trackers whose missing state is no longer present before processing the current evaluation. |
| Medium | `MavlinkDroneMonitor.updateReadinessIfChanged` | Writing readiness fields through `TwinManager.updateTwin` synchronously called the same observer again, causing duplicate readiness evaluation and bootstrap advancement. | `MavlinkDroneMonitorTest.repeated_registration_and_readiness_write_trigger_one_bootstrap_evaluation` | Added a per-twin re-entrancy guard around monitor-authored readiness updates. |
| Medium | `MavlinkTwinUpdater.updateTwinState` | The excluded `BatteryStatusListener` writes `operationalUpdatedAt`, while readiness checks `powerUpdatedAt`; valid battery traffic therefore did not establish battery freshness. | `MavlinkTwinUpdaterTest.valid_battery_packet_refreshes_power_and_preserves_existing_response_topic` | The owned updater now stamps `powerUpdatedAt` for valid `BatteryStatusPacket` events before observer evaluation. |
| Medium | `MavlinkStateSubscriber.stop`, `MavlinkTwinUpdater.close`, `MavlinkDroneMonitor.close` | Shutdown was not idempotent, unsubscribe failure skipped later cleanup, observers remained registered, and late callbacks could continue updating state. | `MavlinkStateSubscriberTest`, `MavlinkTwinUpdaterTest.close_is_idempotent_and_late_updates_are_ignored`, `MavlinkDroneMonitorTest.close_is_idempotent_and_late_callbacks_are_ignored` | Added deterministic lifecycle state, package-local dependency injection for subscriber tests, guaranteed cleanup, observer removal, and late-event guards. |

Regression tests were committed before the corresponding production fixes. They could not be executed in failing or passing form because no local checkout or Maven executable was available.

## Tests added

| Test class/method | Behaviour proved | Important branch or failure path |
|---|---|---|
| `MavlinkBootstrapStateEngineTest.initial_partial_state_emits_readiness_change_and_actionable_requests` | Initial partial state emits one readiness transition and the correct request-message/message-interval events. | Initial discovery, request definition mapping, target IDs. |
| `MavlinkBootstrapStateEngineTest.unchanged_partial_state_waits_for_the_full_retry_interval` | Repeated evaluations do not retry early. | Retry boundary. |
| `MavlinkBootstrapStateEngineTest.retries_exhaust_then_timeout_once_without_sleeps` | Three deterministic attempts are followed by one timeout event. | Exhaustion, timeout, duplicate timeout suppression. |
| `MavlinkBootstrapStateEngineTest.resolved_item_resets_exhausted_tracker_before_it_becomes_missing_again` | Recovery removes terminal request history and permits a later regression request. | Regression defect. |
| `MavlinkBootstrapStateEngineTest.remove_cancels_progress_and_next_update_starts_fresh` | Removal cancels stored bootstrap progress. | Cancellation/reset. |
| `MavlinkBootstrapStateEngineTest.command_ready_completes_once_and_late_regression_does_not_reopen_requests` | Completion is terminal and duplicate ready evaluations are idempotent. | Already-completed and late-event behaviour. |
| `MavlinkBootstrapStateEngineTest` remaining methods | Missing target IDs, unrelated missing states, independent timed-out requests, and null inputs. | Negative and unrelated paths. |
| `MavlinkBootstrapRequestTrackerTest` | Immediate first request, timestamp preservation, out-of-order time, exact retry/timeout boundaries, exhaustion, and terminal timeout. | Deterministic timing without sleeps. |
| `DroneTwinReadinessEvaluatorTest` | Identity, autopilot, connectivity, lifecycle, position, GPS, battery, capabilities, home, and system-health combinations. | Missing, stale, invalid, sentinel, and boundary states. |
| `MavlinkSourceRegistryTest` | Null/empty configuration, exact matching, unrelated IDs, and duplicate-key ordering. | Source rejection and replacement semantics. |
| `MavlinkDroneMonitorTest` | Registration deduplication, re-entrancy suppression, unchanged readiness, removal, close, late callbacks, and non-MAVLink twins. | Observer ordering and lifecycle. |
| `MavlinkStateSubscriberTest` | Repeated start/stop, unsubscribe failure cleanup, close-before-start, and late message completion. | Subscriber resource cleanup and shutdown. |
| `MavlinkTwinUpdaterTest` | Changed/unchanged response state, valid/invalid battery freshness, configured twin creation, close, and late update rejection. | Twin mutation, absent state, invalid packet, lifecycle. |

A total of 47 focused JUnit test methods were added.

## Suggested production changes

- Move the `powerUpdatedAt` assignment into `BatteryStatusListener` when the listener work package owns that package, then remove the compatibility stamp from `MavlinkTwinUpdater`. The current owned-scope fix is intentionally narrow but duplicates message-specific knowledge in the updater.
- Define and expose explicit bootstrap cancellation and failure transitions. `MavlinkBootstrapState.failed` and `BOOTSTRAP_FAILED` exist, but the reviewed engine has no path that sets or emits them.
- Add a small `Clock` seam to `MavlinkStateSubscriber` if `buildUpdateContext` timestamp behaviour needs deterministic direct testing. This work package did not change it because lifecycle tests do not depend on wall-clock time.
- Resolve the empty `batteryCapacityAh` branch in `MavlinkTwinUpdater.createTwin`, either by modelling amp-hour capacity on the twin or removing the unsupported configuration path. It was not changed because the intended conversion semantics are not defined.
- Consider per-twin locking if bootstrap evaluation becomes a high-rate fleet path. The engine currently serialises all twins through one synchronized `update` method; no correctness defect was confirmed in this review.

## Remaining risks and untestable areas

- The branch was written through the GitHub connector because the execution container could not clone the repository. Maven compilation, JUnit execution, JaCoCo generation, and formatting checks were therefore not available.
- MAVLink JSON parsing was reviewed but only subscriber lifecycle was tested. Binary decoding and packet construction remain WP30 concerns.
- Live loopback subscription, broker delivery, network teardown, vehicle hardware, and autopilot timing were not exercised.
- Concurrency coverage proves deterministic synchronous observer re-entrancy and late-callback guards, not high-contention stress behaviour.
- Model-specific detection interpretation and active sender acknowledgement interaction were not changed or tested because those packages are excluded.
- Request completion is inferred from readiness state rather than correlated response message IDs. The tests prove duplicate/out-of-order readiness evaluations and unrelated missing states, but the current architecture has no direct request-response correlation API to exercise.

## Commands run

| Command | Result |
|---|---|
| `git clone --branch development --single-branch https://github.com/Maps-Messaging/mapsmessaging_server.git /tmp/mapsmessaging_server-wp29` | Failed, exit 128: `Could not resolve host: github.com` |
| GitHub connector: resolve `development` and create `review/state-mavlink-bootstrap` | Passed; branch created from `9455754a92ffb5136e1d15f0efd749a5dc20d838` |
| `java -version` | Passed: OpenJDK `21.0.10` |
| `mvn -Dtest="io.mapsmessaging.state.mavlink.**,io.mapsmessaging.state.mavlink.bootstrap.**" test` | Not run, exit 127: `mvn: command not found` |
| `mvn test` | Not run, exit 127: `mvn: command not found` |
| GitHub connector compare: `development...review/state-mavlink-bootstrap` | Passed before README creation: branch ahead by 17 commits, behind by 0, 15 changed files |

## PR
- PR URL: https://github.com/Maps-Messaging/mapsmessaging_server/pull/2138
- Head: `review/state-mavlink-bootstrap`
- Base: `development`
