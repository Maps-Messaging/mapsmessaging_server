# CoT/TAK Phase 1 configuration

Phase 1 supports legacy Cursor on Target 2.0 XML over a long-lived TCP or TLS
stream in both directions:

- `NetworkConnectionManager.yaml` creates an outbound client connection to a TAK
  server.
- `NetworkManager.yaml` opens a listener that accepts connections from CoT
  producers.

The same socket is used for reads and writes. There is no CoT subscribe command
and no separate read port: TAK routing, certificate identity, user/group
permissions, and the server's port configuration decide which events a client
receives. This implementation does not support TAK Protocol/Protobuf framing.

## Connect MapsMessaging to a TAK server

Add an entry under `NetworkConnectionManager.data`. This clear-text example uses
port 8088, which is a common OpenTAK legacy-CoT port; use the port configured by
the target server.

```yaml
NetworkConnectionManager:
  data:
    - name: tak-sydney
      url: tcp://opentak.syd.mapsmessaging.io:8088/
      protocol: cot

      # CoT protocol limits and routing
      maximumEventSize: 1048576
      maximumXmlDepth: 64
      qualityOfService: 1
      storeOffline: false
      appendNewLine: true
      inboundQueueDepth: 1024
      outboundQueueDepth: 1024
      writeTimeoutSeconds: 30
      clockSkewSeconds: 5

      # Loop and replay protection
      suppressEchoes: true
      echoOrigin: maps-sydney-01
      maximumHopCount: 4
      fingerprintCacheSize: 4096
      fingerprintCacheTtlSeconds: 120
      maximumTrackedUids: 10000

      # Regenerated on every successful connection and then refreshed
      presence:
        enabled: true
        uid: maps:gateway:sydney-01
        callsign: MAPS-SYDNEY-01
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

`push.local_namespace` supplies XML events written to TAK.
`pull.local_namespace` receives XML events read from TAK. The common connection
framework requires `remote_namespace`, but legacy CoT XML carries no broker topic,
so `/cot` is a local mapping key and is not sent as a subscription command.

Use a stable `presence.uid` for the represented gateway and a stable, unique
`echoOrigin` for each MapsMessaging bridge instance. Callsigns are display names
and are not safe unique identifiers.

### TLS and mutual TLS

TAK deployments commonly expose TLS legacy CoT on a different port (often 8089).
Use the actual TAK server port and provide a client key store plus the trust store
that validates the server:

```yaml
NetworkConnectionManager:
  data:
    - name: tak-sydney-tls
      url: ssl://tak.example.net:8089/
      protocol: cot
      security:
        tls:
          context: TLSv1.3
          hostnameVerificationEnabled: true
          clientCertificateRequired: false
          clientCertificateWanted: false
          keyStore:
            type: PKCS12
            managerFactory: SunX509
            path: /etc/maps/tak-client.p12
            passphrase: "<client-keystore-passphrase>"
          trustStore:
            type: PKCS12
            managerFactory: SunX509
            path: /etc/maps/tak-trust.p12
            passphrase: "<truststore-passphrase>"

      suppressEchoes: true
      echoOrigin: maps-sydney-01
      presence:
        enabled: true
        uid: maps:gateway:sydney-01
        callsign: MAPS-SYDNEY-01
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

Replace the passphrase placeholders through the deployment's secret provisioning
process; do not commit literal passphrases. A TAK client certificate determines
the authenticated connection identity. It is separate from the UID and callsign
inside each CoT event.

## Accept connections from CoT producers

Add a listener under `NetworkManager.data`:

```yaml
NetworkManager:
  data:
    - name: cot-producer-listener
      url: tcp://0.0.0.0:8088/
      protocol: cot
      auth: default

      inboundTopicName: /tak/cot/inbound
      outboundTopicName: /tak/cot/outbound
      maximumEventSize: 1048576
      maximumXmlDepth: 64
      qualityOfService: 1
      storeOffline: false
      appendNewLine: true
      suppressEchoes: true
      echoOrigin: maps-listener-{interfaceName}
      maximumHopCount: 4
      fingerprintCacheSize: 4096
      fingerprintCacheTtlSeconds: 120
      maximumTrackedUids: 10000
      clockSkewSeconds: 5
      inboundQueueDepth: 1024
      outboundQueueDepth: 1024
      writeTimeoutSeconds: 30
```

Every accepted connection has its own bounded queues, ordering state, fingerprint
cache, and broker session. Received events are published to
`inboundTopicName`. Events published to `outboundTopicName` are written to each
applicable connected producer session. Presence generation is client-only and is
ignored for listener endpoints.

For a TLS listener, change the URL to `ssl://0.0.0.0:<port>/`, configure the
listener key store under `security.tls`, and set
`clientCertificateRequired: true` plus an appropriate trust store when producers
must authenticate with certificates.

## Operational behaviour

- Events may span reads, and multiple events may arrive in one read. Newlines are
  optional separators, not the framing contract.
- Inbound XML is size/depth limited. DTDs, external entities, external resource
  access, malformed timestamps, and incomplete required CoT attributes are
  rejected without logging payload contents.
- `time`, `start`, and `stale` are interpreted as UTC instants. Expired events,
  events not yet active beyond `clockSkewSeconds`, duplicates, and older updates
  for the same UID are dropped.
- State and ordinary map-object traffic is coalesced by UID under load. Alerts,
  chat, control, and deletion traffic can evict lower-value queued state. Once no
  lower-value entry remains, the newest event is deterministically dropped.
- Echo identity is a semantic SHA-256 fingerprint, not raw XML or UID alone.
  Formatting and attribute order do not affect it. Origin markers survive other
  Maps bridges, and an event is not forwarded beyond `maximumHopCount`.
- Fingerprint caches are bounded and expire. They are connection-local and start
  empty after reconnect. UID ordering state is bounded and also connection-local.
- Disconnected client protocol sessions do not retain a CoT backlog. Presence is
  rebuilt after reconnect. Queued state is checked for staleness immediately
  before dispatch.
- Delivery is best-effort and at-most-once from this protocol session. If a socket
  fails during a write, delivery is uncertain because legacy CoT has no protocol
  acknowledgement. Consumers must tolerate duplicates and reconnect state replay.
- A connected client receiving no events may simply lack TAK routing/group
  permission; silence alone is not diagnosed as a transport failure.

The protocol exposes counters for malformed, expired, not-started, duplicate,
direct-echo, semantic-echo, hop-limit, older, unsupported, and queue-overflow
events, plus current inbound and outbound queue depth. Normal position traffic is
not logged at INFO.

## Phase 1 limitations

The shared Network Connection state model and connection-management mechanism are
unchanged. Follow-up work is tracked separately for XML-aware stream resynchrony
(MSG-244), old-generation close callbacks (MSG-245), and the shared selector
writer queue (MSG-246). TAK Protocol/Protobuf and application-level delivery
acknowledgements are not part of Phase 1.
