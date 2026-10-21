# MQTT-SN 2.0 authentication and sleep renewal

Tracking: MSG-325. Baseline: MQTT-SN 2.0 CSD01 (14 August 2026), OASIS commit
`0dae5066d123d068ddca2ddb861ce8858aafee92`.

## Authentication renewal

An authenticated Active client can initiate renewal with AUTH `0x19`. The server
uses a fresh SASL mechanism, requires the original CONNECT method, and exchanges
AUTH `0x18` until it can send AUTH `0x00` with final mechanism data. Replies echo
the identifier of the latest client AUTH; continuations need not reuse CONNECT's
identifier. Initial authentication still ends with CONNACK.

Renewal preserves the broker session, subscriptions, topic aliases, Will and QoS
state. The authenticated identity must remain unchanged because the existing
session's authorization context belongs to that identity. A new identity requires
CONNECT. Other outbound packet traffic is buffered during renewal, including
packets already queued to the UDP writer. A write-time guard serializes the actual
write against renewal; a deferred protected packet is re-encoded on resumption so
its counter remains valid. Datagrams already transmitted remain subject to UDP
loss/reordering. Retry processing pauses until renewal succeeds.

The entire renewal has a deadline of the negotiated Keep Alive interval. Failure
or timeout sends DISCONNECT when possible, disposes SASL and deletes the Virtual
Connection. Write failure invokes cleanup directly; a stalled writer also has an
independent five-second cleanup deadline. AUTH is never retransmitted.

## Sleep renewal

Repeated SLEEPREQ while ASLEEP starts a new cycle with the requested duration,
returns the corresponding SLEEPRESP and applies the alias-retention flag. It does
not wake or reconnect the client. Predefined aliases, subscriptions and queued
messages remain intact. The UDP idle monitor suspends Keep Alive in ASLEEP/AWAKE;
sleep expiry and QoS retry handling own those states' deadlines.

PINGRESP uses the CSD01 one-byte Application Messages Remaining field: 0 means
empty, 1–254 is an exact count, and 255 means an unspecified positive count.
This corrects the previous two-byte server encoding rejected by the client.

The server retains its existing grace period of 1.5 times the requested duration.
Expiry begins after SLEEPRESP/PINGRESP is written, including empty wakes. Each
cycle has a generation, and both delayed write completions and cancelled expiry
callbacks check that generation before changing the current connection.

## Tests and dependency

The server test dependency is `io.mapsmessaging:mqtt-sn-2-client:0.2.0-SNAPSHOT`
(and matching protection module). Build/install the updated client before running
server tests; the earlier 0.1.0 snapshot lacks these APIs and state transitions.

`MqttSn2RenewalInteropTest` uses the real Java UDP client and SASL adapter against
the server: repeated SCRAM renewal, preserved subscription/session, paused delivery,
wrong credentials, changed method, timeout, sleep renewal, lost SLEEPRESP, queued
QoS 1 delivery after wake, and expiry after return to sleep. Lifecycle tests cover
stale timer generations deterministically; UDP writer tests cover deferral and
failed-write notification. MQTT-SN 1.2 behaviour is outside this implementation
change and requires its existing regression suite.

Executed validation is recorded with the implementation commits. This document
is not a claim of full CSD01 conformance or a substitute for running integration
tests in an environment with all server dependencies.

## Executed validation — 19 October 2026

- Focused selection: 25 passed, including all 7 real client/server renewal scenarios,
  lifecycle generations, ordered paused writes, UDP write failure/deferral and
  exact PINGRESP byte vectors.
- MQTT-SN 1.2/2.0 and shared write-path regression selection: 492 tests, initially
  489 passed and 3 DTLS certificate-validation errors. The local generated key
  and trust stores were mismatched; a matching test-only pair resolved the setup
  error. The focused `DTLSConnectPacketTest` rerun passed all 3 tests. No test was
  disabled and no certificate verification was weakened.
- The authentication-only server source/test projection also compiled independently.
- Client Maven validation: 120 core and 32 protection-provider tests passed;
  Python: 115 passed; C/C++: all 7 CTest executables passed; Arduino forwarding
  sources passed both host-compiled canonical C auth/session test programs.

Regression command (from the server directory):

```sh
mvn -Dexec.skip=true '-Dtest=io.mapsmessaging.network.protocol.impl.mqtt_sn.**,UDPWriteTaskGuardTest,FrameHandlerTest,FrameHandlerDeadlockTest,WriteBufferLifecycleTest,UdpDatagramBoundaryTest' test
mvn -Dexec.skip=true -Dtest=DTLSConnectPacketTest test
```

`exec.skip` preserves the already prepared local test certificates on repeat
runs. This restricted runtime also used Byte Buddy's startup agent because
Mockito's dynamic self-attachment was unavailable; this requires no source or
POM change. The full server-wide test suite and Jenkins were not run.
