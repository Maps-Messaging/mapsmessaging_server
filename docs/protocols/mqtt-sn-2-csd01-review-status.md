# MQTT-SN 2.0 CSD01 architecture and interoperability status

Reference: MQTT-SN 2.0 CSD01 (August 2026), OASIS source revision `0dae5066d123d068ddca2ddb861ce8858aafee92`.

Tracking: MSG-324. This document distinguishes implementation changes from executed validation. **It is not a declaration of full CSD01 conformance.**

| Requirement | CSD01 trace | Implementation status | Validation still required |
| --- | --- | --- | --- |
| Dual-version sessionless PUBWOS safety | §3.6, topic-alias clauses | Unprotected predefined-alias form rejected on shared dual-version listener; direct-topic form retained | MQTT-SN 1.2 colliding SUBSCRIBE and allowed/unallowed PUBWOS integration tests |
| Protected sender/Client ID session isolation | §3.17-3 to §3.17-5 | Protection decoder carries the verified sender; MAPS session key combines sender and wire Client ID; mismatched subsequent packets fail closed | Two-sender/two-Client-ID, reconnect, replay and authentication-failure integration tests |
| Outbound persistent QoS 1/2 recovery | §4.1.1, §4.4.1-1 | **Not implemented.** The outgoing manager still retains the original protocol's in-flight callbacks and packet state. | Durable session-scoped state, three QoS stages, Clean Start, expiry and restart |
| Existing virtual-connection CONNECT takeover | §4.14-4 | A verified CONNECT replaces the old v2 protocol context; close and failed-start cleanup now check protocol identity before removing a virtual connection | Connected/asleep/awake takeover and Will/session-expiry lifecycle tests |
| Malformed/protocol error removes connection | §4.12.1-2, §4.12.2-1 | Established v2 IOException path closes and removes the affected virtual connection; v1.2 path unchanged | Malformed frame/state-transition and multi-client isolation tests |
| Duplicate QoS 2 PUBREL | §4.3.4-12 | Committed identifiers and PUBCOMP-write-pending identifiers guard one MAPS transaction commit; completed transaction references now leave the protocol map after PUBCOMP write completion | Concurrent duplicate before PUBCOMP callback; after-completion protected UDP regression added but not executed |
| Sleeping-message enqueue/wake race | §3.15, §4 wake semantics | ASLEEP/AWAKE state decisions and queue insertion coordinate under the queue monitor; network delivery is outside that monitor | Deterministic latches/barriers test, ordering and Remaining Messages accounting |
| Post-CONNACK enhanced-authentication reauthentication | §4.11.1.1 | **Not supported.** Lifecycle currently permits AUTH only during initial authentication, not after CONNECT completes. Do not claim complete enhanced-authentication support. | Specification/product decision before implementation |
| MQTT-SN 1.2 interoperability | 1.2 standard | Shared listener and legacy 1.2 path retained | MQTT-SN 1.2 full regression suite |
| Protection schemes and negative interoperability | §3.17 | Previously passed 15 reference-client protection schemes, before this change block | Re-run after current modifications, including identity and reconnect cases |

## Cleanup and deployment gates

The obsolete `MQTT_SNProtocolV2`, `PacketFactoryV2`, and the legacy v2 packet/state/listener hierarchy have been removed (32 files). The active `MqttSn2Protocol`, its CSD01 codecs, security providers and the MQTT-SN 1.2 implementation are retained. Compile and regression-test the removals before merging; absence of references in repository searches is not proof of runtime compatibility.

Format all remaining active Java sources using the repository's Google Java conventions with a 180-character line limit, and use the standard Maps Messaging copyright/license header. Formatting must not alter packet handling or protocol semantics.

Do not merge, change Jira status, claim full CSD01 conformance, or queue Jenkins until local 1.2 and 2.0 regression tests are confirmed. A green pre-change Jenkins run does not validate these changes.

## Outstanding MSG-324 correctness gates (11 October 2026)

- **Outbound QoS persistence:** Still outstanding. The protocol-instance delivery queue and callback references must be integrated with MAPS session persistence before recovery can be claimed.
- **Negative acknowledgements:** Existing tests deliberately forbid success callbacks on failed PUBACK/PUBREC/PUBCOMP. The broker's failure/requeue/credit accounting contract requires an explicit design and validation; dropping the in-flight item alone is not an acceptable final resolution.
- **QoS 2 packet identifier lifecycle:** Completed IDs remain in a finite (16-bit) set until fresh non-DUP reuse or protocol close; verify reconnect, bounded retention and rollback semantics. The completed transaction object is now removed from the local map on PUBCOMP completion.
- **Asynchronous ownership:** Address-scoped virtual connection removal is now guarded by protocol identity; asynchronous session creation and pending destination callbacks still need race tests.
- **Build gate:** No compile or targeted MQTT-SN 1.2/2.0 suite was executed for this change block; Jenkins must remain unqueued until these items are resolved.
