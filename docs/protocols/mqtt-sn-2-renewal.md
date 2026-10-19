# MQTT-SN 2.0 authentication renewal

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

## Tests and dependency

The server test dependency is `io.mapsmessaging:mqtt-sn-2-client:0.2.0-SNAPSHOT`
(and matching protection module). Build/install the updated client before running
server tests; the earlier 0.1.0 snapshot lacks the renewal API and state transitions.

`MqttSn2RenewalInteropTest` uses the real Java UDP client and SASL adapter against
the server: repeated SCRAM renewal, preserved subscription/session, paused delivery,
wrong credentials, changed method and timeout. UDP writer tests cover deferral
and failed-write notification; paused-write tests cover ordered replay. MQTT-SN 1.2 behaviour is outside this implementation
change and requires its existing regression suite.

Executed validation is recorded with the implementation commits. This document
is not a claim of full CSD01 conformance or a substitute for running integration
tests in an environment with all server dependencies.
