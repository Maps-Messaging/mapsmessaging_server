# Packet identifier storage — MSG-392

`storage.delivery.PacketIdStore` belongs to one session. It allocates unique
identifiers from 1 through a configured maximum (at most 65535), incrementing
and wrapping while skipping outstanding identifiers. Capacity exhaustion fails
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
- `outstanding()` returns an immutable recovery snapshot.
- `flush()` provides a durability barrier; `compact()` reclaims completed history.
- `close()` releases ownership and flushes; it does not delete persistent records.

`existing=true` means this delivery was allocated earlier. Protocols may use it
as a retransmission marker once allocation is placed at the correct send
boundary. It does not prove the network received the original packet. DUP
remains zero for QoS 0; QoS 2 handshake recovery requires additional protocol
state and is not implemented by this mapping component.

## Cost and durability

Lookups use in-memory hash indexes and perform no file reads. Existing
allocation and unknown clear perform no writes. A new persistent allocation
appends 29 bytes plus UTF-8 topic/key length; completion appends 17 bytes.
Channels remain open for the owning session's storage lifetime. Packet-ID
search is bounded by configured capacity; normal allocation is one lookup,
but near-full wraparound can scan the identifier range.

SYNC forces each mutation before returning. BATCHED appends without forcing;
the owner must call `flush()` before network actions requiring crash durability.
Batching reduces durability barriers, not the number of append writes. There
is no background scheduler or hidden periodic force policy.

The journal has a versioned header, protected record lengths and CRC32C.
Recovery discards an incomplete final append, and rejects complete corrupt or
inconsistent records. Compaction writes and forces a new journal, atomically
replaces the old one, then forces its directory. Ownership remains held by a
separate lock throughout replacement. Journal paths must not be symbolic
links; use one canonical path and do not create hard-link aliases, especially
across compaction generations. Atomic replacement and directory forcing must
be supported by the deployment filesystem.

History and startup work grow until the owner schedules compaction. No
per-ACK map rewrite or full identifier-space preallocation is performed.

## Integration boundary

This component is not yet connected to MQTT or MQTT-SN. Session expiry/clean
start must close and remove the owned journal; reconnect must retain it.
Allocation/completion ordering with engine interest persistence, packet-ID
reservation for control traffic, connection-generation ACK guards and QoS 2
stage persistence remain protocol integration work. Missing files are created
by `open`; the session owner must detect loss of an expected existing journal
rather than silently treating it as a new session.

Verification includes real abrupt JVM exit after durable allocation, partial
append recovery, corruption rejection, allocation/clear/reuse, ownership,
compaction failure and concurrent callers. This does not establish physical
power-cut guarantees or benchmark sustained throughput.
