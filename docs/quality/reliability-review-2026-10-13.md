# Reliability review — 13 October 2026

Reviewed the 65 OPEN/CONFIRMED reliability findings in
`Maps-Messaging_mapsmessaging_server`, analyzed as `development` after Jenkins #219.
Implementation branch: `fix/MSG-374-udp-protocol-compliance`.
Sonar statuses have not been changed, and these assessments are not confirmed
issue closures. REST permissions remain outside this cleanup.

## Null and delivery lifecycle protections

- Capture nullable subscription contexts once in aggregator shutdown, MQTT 3/5
  delivery, satellite delivery, Semtech delivery/shutdown, REST delivery and AMQP
  send/link-close tasks. Complete abandoned deliveries instead of dereferencing
  removed contexts. Preserve MQTT send-slot release.
- MQTT parsing and frame creation use the same captured context. A transformer
  that drops an event does not pass null into the analyser.
- Protect MQTT 3/5, MQTT-SN 1.2, MAVLink and satellite metadata access before a
  session exists, while retaining active-session details.
- Reject missing MQTT-SN factory configuration with IOException. State, connection
  setup and pipeline use the configuration already bound to the protocol.
- MQTT-SN removes stale queued entries without recursive draining. Network writes,
  abandonment and retransmissions share an event-local once-only completion guard.
  QoS>0 entries are not inserted twice while REGISTER is pending. Publish identifiers
  are allocated only after a topic alias is available.
- Browser subscription construction reports an inactive parent with IOException.
- Modem command encoding reports missing payload/format with a descriptive state
  error instead of an incidental null dereference.
- Two original null findings do not need guards: MQTT Publish constructors already
  normalize null payloads to empty arrays, and Protocol's endpoint is final and
  validated non-null by its constructors. Do not invent an empty authentication
  configuration fallback for a malformed Protocol instance.

## Concurrency review

| Area | Ownership and monitor decision |
| --- | --- |
| Subscription contexts | All reads, snapshots, additions and removals use the same context-list monitor. Reproduced the former empty-check/get race. No callbacks occur under this monitor. |
| NATS pending writes | Registration and close coordinate on the tracker monitor. Frame callback replacement retains the same frame monitor used by complete(). Close snapshots first, releases the tracker, then completes frames. Callbacks execute outside both locks. |
| JetStream consumer creation/deletion and NATS subscription snapshots | Retain the connection's SessionState monitor, also used by its synchronized methods. A private lock in an individual handler would cease coordinating with other handlers. |
| TwinManager and TwinJsonPublisher | Retain the same EntityTwin monitor for updates and serialization. Do not replace these independently. |
| DroneTwin missionTransmission | Its MavlinkTransmissionState publishes immutable Snapshot records through a volatile reference. AtomicReference alone would add no protection to snapshot contents. |
| DroneTwin taskTransmission | A volatile mutable map still permits mutation by external holders. An immutable snapshot contract needs a separate API/serialization review; no live writer was found in this repository. |
| Connection state/endpoints/protocol and Disconnected activeEndPoint | Retain publication visibility and existing stateLock. A broader handoff review must test cancellation against late callbacks and transfer of endpoint ownership; replacing the fields mechanically does not establish safety. |
| CAN device | Retain deviceLock for binding/replacement/close and volatile publication to readers. The endpoint owns the device; individual readers must not close it. |
| MAVLink factory | Retain volatile publication when replacing dialect factories. Decoder/internal factory ownership requires library review before asserting general thread safety. |

The MQTT-SN changes protect completion and claiming stale entries; they do not
claim to make every live-send/ACK/timeout operation in the pipeline atomic.

## Lifetime ownership

- AuthDbStoreManager owns its live MapDB database and maps until owner close.
  Migration owns both temporary databases until copying is finished. Individual
  maps are not closed merely because copyAllMaps returns.
- Fixed a distinct failure path: if live-store initialization/validation fails,
  construction cannot transfer ownership to a manager, so the database is closed.
  Successful construction leaves it open. Tests use real MapDB maps and invalid
  mapping metadata, and also check the successful lifetime.
- Auth0TokenGenerator and the optional ChatGPT selector client create an HttpClient
  per operation. Those clients are closed after their operation, including failure.
  The ML regression stays under src/test/java-ml and does not alter default builds.

## Time zones

Seven calls now explicitly use ZoneId.systemDefault(): schema lookup, both legacy
schema timestamps, instance creation date, the system timestamp message, YAML
headers and configuration lint reports. Existing formats and local-zone behavior
are preserved; no UTC conversion or persisted timestamp migration is introduced.

## Verification

Regression tests reproduce null/lifecycle errors, the subscription read/remove
race, the NATS close/register race, duplicate MQTT-SN completion before and after
writing, publish-ID allocation before registration, invalid modem state, and
resource cleanup on failed operations/construction.

- Java 21 compilation: 2,218 production sources (including the changed optional
  ML client) and 1,212 test sources compiled successfully.
- Final focused selection: 82 classes, 863 tests found, 860 passed, three existing
  skips, zero failures. Run with Australia/Sydney as the default time zone.
- Added 45 regression cases, including two under the optional ML test source tree.
- Independent source review: no remaining blockers after the MQTT-SN completion
  and identifier ownership corrections.
- git diff --check passed.

The normal Maven test command remains blocked during dependency collection by
unavailable cached repository metadata; POMs and dependency versions are unchanged.
No Jenkins run or Jira update is part of this cleanup.
