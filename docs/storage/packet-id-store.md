# Packet identifier storage — MSG-392

`storage.delivery.PacketIdStore` belongs to one session. It allocates unique
identifiers from 1 through a configured maximum (at most 65535), incrementing
and wrapping while skipping outstanding and reserved identifiers. Capacity exhaustion fails
immediately; allocating an existing delivery still succeeds at capacity.

The key is an exact topic string plus a signed long message identifier. Where
multiple independent subscription deliveries share a topic/message pair, pass
a stable subscription key instead of the topic. The owning protocol controls
that identity; the storage has no dependency on subscription objects.

## API

- `memory(maximum)` or `open(path, maximum, durability)`.
- `allocate(topic, messageId)` returns `Allocation(packetId, existing)`.
- `getPacketId(topic, messageId)` returns the identifier, or null.
- `getDelivery(packetId)` returns `Delivery(topic, messageId)`, or null.
- `clear(packetId)` / `clear(topic, messageId)` remove completion/expiry/eviction state.
- `getStage(packetId)` returns 0 (PUBLISH) or 1 (awaiting PUBCOMP), defaulting to 0.
- `setStage(packetId, stage)` advances an outstanding delivery from 0 to 1;
  repeats are idempotent and invalid, missing-delivery or backwards updates fail.
- `getQos(packetId)` returns immutable original wire QoS, or 0 when unspecified;
  legacy stage 1 implies QoS 2.
- `setQos(packetId, qos)` selects QoS 1 or 2 for an outstanding delivery;
  repeated selections do not append, and subsequent changes fail.
- `clearPendingDelivery(key, messageId)` atomically clears only stage 0.
- `clearDeliveries(key)` atomically removes all deliveries for an exact key,
  including stage 1, and returns the number removed.
- `reservePacketId()` reserves a control identifier in the same nonzero space.
- `releaseReservedPacketId(packetId)` releases only a control reservation.
- `outstanding()` returns an immutable recovery snapshot of deliveries only.
- `flush()` provides a durability barrier; `compact()` reclaims completed history.
- `close()` releases ownership and flushes; it does not delete persistent records.

`existing=true` means this delivery was allocated earlier. Protocols may use it
as a retransmission marker once allocation is placed at the correct send
boundary. It does not prove the network received the original packet. DUP
remains zero for QoS 0. QoS 2 stage 1 survives restart and compaction, allowing
the owning protocol to replay PUBREL while awaiting PUBCOMP.

## Cost and durability

Lookups use in-memory hash indexes and perform no file reads. Existing
allocation and unknown clear perform no writes. A new persistent allocation
appends 29 bytes plus UTF-8 topic/key length; completion appends 17 bytes and
a stage advance or original QoS selection appends 18 bytes. Repeated stage updates do not append.
Control reservations are memory-only and never appear in journal records or
recovery snapshots; connection owners must release them on ACK or disconnect.
Channels remain open for the owning session's storage lifetime. Packet-ID
search is bounded by configured capacity; normal allocation is one lookup,
but near-full wraparound can scan the identifier range.

SYNC forces each mutation before returning. BATCHED appends without forcing;
the owner must call `flush()` before network actions requiring crash durability.
Batching reduces durability barriers, not the number of append writes. There
is no background scheduler or hidden periodic force policy.

The journal has a versioned header, protected record lengths and CRC32C.
Version 1 journals without stage/QoS records remain readable, with defaults.
Recovery discards an incomplete final append, and rejects complete corrupt or
inconsistent records. Compaction writes and forces a new journal, atomically
replaces the old one, then forces its directory. Ownership remains held by a
separate lock throughout replacement. Journal paths must not be symbolic
links; use one canonical path and do not create hard-link aliases, especially
across compaction generations. Atomic replacement must be supported by the deployment filesystem.
Directory forcing is used where supported by the platform. Windows Java NIO
cannot open directory channels, so Windows forces journal file contents but
cannot promise the same power-loss durability for directory entries/renames.

On `flush()`, journals of at least 1 MiB automatically compact when their
mutation record count reaches four times a conservative live-record estimate
(`3 * outstanding deliveries + 1`). The gate uses in-memory counts and a file
size check; it never scans outstanding entries below the threshold. Recovery
restores the count, and compaction resets it to the retained record count.
Explicit `compact()` remains available. No per-ACK map rewrite or full
identifier-space preallocation is performed.

## MQTT and MQTT-SN ownership

MQTT 3.1.1/5 and MQTT-SN 1.2/2 share one store per session for outbound
QoS 1/2 deliveries. Memory sessions use an in-memory store; persistent sessions
retain a journal across disconnects and reload it after restart. Clean Start,
session expiry and permanent removal close and delete the owned journal.
Stable delivery keys distinguish subscription filters, shared groups and actual
physical destinations, including wildcard subscriptions with equal message IDs.

Reliable PUBLISH allocation persists its packet ID and original wire QoS before
transmission. Re-delivery reuses that ID and sets DUP. QoS 2 PUBREC advances the
journal before sending PUBREL. Normal exchanges and recovery complete exact
broker acknowledgement tasks before sending PUBREL. The durable PUBREL stage survives message expiry
until PUBCOMP, and cannot become a fresh PUBLISH after a QoS downgrade.
Terminal acknowledgements retire mappings; expiry/capacity cleanup retires only
PUBLISH-stage records. Control traffic reserves IDs in the same session space
without persisting connection-scoped reservations. Runtime retirement callbacks
run outside journal locks and preserve subsequent allocations.

The alias store remains a topic-to-alias registry. The counter store remains
MQTT-SN 2 protection replay/nonce state; neither replaces this delivery journal.

Verification includes real abrupt JVM exit after durable allocation, partial
append recovery, corruption rejection, allocation/clear/reuse, ownership,
compaction failure and concurrent callers. This does not establish physical
power-cut guarantees or benchmark sustained throughput.
