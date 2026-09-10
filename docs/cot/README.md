# Cursor on Target protocol design

The `cot` protocol is a full-duplex Cursor on Target 2.0 XML bridge over an
existing TCP or TLS stream endpoint. The endpoint owns transport security and
connection lifecycle. See [`cot_config.md`](cot_config.md) for complete client
and listener examples.

## Listener endpoint

Listener-mode endpoints use `inboundTopicName` and `outboundTopicName` directly:

```yaml
- name: tak-listener
  url: ssl://0.0.0.0:8089
  protocol: cot
  inboundTopicName: /tak/cot/inbound
  outboundTopicName: /tak/cot/outbound
  maximumEventSize: 1048576
  qualityOfService: 1
  storeOffline: false
  appendNewLine: true
```

TLS keys, trust and client-certificate policy remain part of the existing SSL endpoint configuration.

## NetworkConnectionManager client

A client connection binds CoT through the standard `push` and `pull` links:

```yaml
NetworkConnectionManager:
  data:
    - name: remote-tak
      url: ssl://tak.example:8089
      protocol: cot
      maximumEventSize: 1048576
      appendNewLine: true
      suppressEchoes: true
      echoOrigin: maps-{interfaceName}
      presence:
        enabled: true
        uid: maps-{interfaceName}
        callsign: MAPS-{interfaceName}
        cotType: a-f-G-E
        how: m-g
        latitude: 0
        longitude: 0
        hae: 0
        ce: 9999999
        le: 9999999
        groupName: Cyan
        groupRole: Team Member
        device: MapsMessaging
        platform: MapsMessaging
        operatingSystem: Java
        softwareVersion: ""
        intervalSeconds: 60
        staleSeconds: 120

      links:
        - direction: push
          local_namespace: /tak/cot/outbound
          remote_namespace: /cot
          qos: 1

        - direction: pull
          remote_namespace: /cot
          local_namespace: /tak/cot/inbound
          qos: 1
```

For `push`, `local_namespace` is the broker subscription that supplies XML events for the socket. For `pull`, `local_namespace` is where received XML events are published.

CoT/TAK streams do not carry broker topic names and have no protocol-level subscribe request. Consequently, `remote_namespace` is a logical mapping key required by the common link configuration; it is not transmitted to the TAK server. One incoming event is published to every configured pull binding.

Use `tcp://` for an unencrypted connection or `ssl://` for TLS. The CoT protocol code is identical for both.

When `presence.enabled` is true, client connections send a presence event immediately after connecting and then every `intervalSeconds`. The event timestamps are generated for each send and expire after `staleSeconds`. OpenTAK uses the event UID and `contact` callsign to identify the client and establish its routed stream. The `uid` and `callsign` fields support the `{interfaceName}` placeholder. A configured latitude and longitude of zero with `ce` and `le` set to `9999999` represents an unknown gateway location rather than a precise position.

Presence is disabled by default and is ignored on listener endpoints. Set `staleSeconds` greater than `intervalSeconds` so the presence does not expire between refreshes.

With `suppressEchoes` enabled, Maps adds a namespaced `maps:origin` element under
`detail` to every outbound event. Origin markers from other bridges are preserved.
An event carrying this bridge's stable origin is dropped, and a configurable hop
limit stops indirect loops. A bounded, expiring semantic fingerprint cache also
recognises reformatted echoes; the fingerprint ignores attribute order,
formatting whitespace, and Maps origin markers while retaining event timestamps
and content. It never suppresses solely by UID.

## Payload behaviour

- Inbound stream data is buffered until complete CoT XML events are available.
- Each complete event is published as `text/xml`.
- Fragmented and adjacent events are supported by the CoT stream decoder.
- A bounded single-consumer inbound queue keeps broker processing off the socket
  read loop.
- One bounded outbound queue serializes concurrent publishers and retains partial
  socket writes without allowing event bytes to interleave.
- Queue overload coalesces state/map objects by UID and preserves alerts, chat,
  control, and deletions ahead of lower-value state where possible.
- Events are validated using a hardened streaming XML parser. DTDs, external
  entities, external resources, oversized documents, excessive nesting, invalid
  lifecycle attributes, and malformed XML are rejected without terminating the
  service when framing remains recoverable.
- Expired, not-yet-started, duplicate, echoed, over-hop, and out-of-order older
  events are dropped. Unknown detail extensions are preserved. Unsupported event
  types are passed through and counted/logged at a controlled rate.
- Outbound payloads are passed through configured link transformations, encoded
  as CoT XML and optionally followed by a newline. Newlines are not required for
  inbound framing.
- No CoT-to-Twin mapping is performed by this protocol.

## Delivery and reconnect semantics

Legacy CoT XML has no application acknowledgement. A completed local socket write
does not prove that TAK received or processed the event, and a failure during a
write leaves delivery uncertain. The protocol therefore provides best-effort,
at-most-once delivery per connection session while accepting duplicate/replayed
inbound state idempotently.

The CoT session is nonpersistent. Its bounded queues are discarded when the
connection closes; stale state is checked again before dispatch, and configured
presence is regenerated with fresh UTC timestamps after reconnect. Fingerprint
and per-UID ordering state are connection-local and reset on reconnect.

TCP keepalive and the existing endpoint timeouts detect ordinary termination and
many half-open paths. A write that makes no progress for `writeTimeoutSeconds`
closes the affected protocol so the existing connection manager can reconnect.
An otherwise healthy, quiet connection is not closed merely for receiving no
events because TAK routing permissions can legitimately produce silence.

## Scope boundaries

This protocol supports legacy XML streaming only, not TAK Protocol/Protobuf. The
shared Network Connection state model and lifecycle mechanism are unchanged.
Known shared-component follow-ups are MSG-244 (XML-aware framing recovery),
MSG-245 (superseded endpoint close callbacks), and MSG-246 (shared selector queue
bounds and instrumentation).
