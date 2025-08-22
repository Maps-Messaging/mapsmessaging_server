# Satellite Modem & Website Integration — Overview

## 1) Operational Model

- **Device Online**: The modem is always reachable from the server.  
- **Satellite TX Capability**: Determined solely from modem status events.  
  - `canSend = true` → modem may transmit to the satellite.  
  - `canSend = false` → modem cannot currently transmit (weak signal, jamming, network condition).  
- **Firmware Detection**: On startup, the modem’s response format is detected:  
  - **OGx** (e.g., `AT%NETINFO`)  
  - **IDP** (e.g., `ATS54`)  

This ensures the correct parsing of modem-specific status information.

---

## 2) Modem Status Event

An optional periodic event conveys modem state that is **published to a configuration topic**:

```json
{
  "satellite": {
    "receivedMsgs": 18,
    "sentMsgs": 41,
    "bytesSent": 34660,
    "bytesRead": 47410
  },
  "location": {
    "latitude": "27° 25' 34.968 S",
    "longitude": "153° 16' 33.042 E",
    "satellites": "6"
  },
  "jamming": {
    "jammingIndicator": 0,
    "jammingStatus": "OK"
  },
  "satelliteTxStatus": {
    "canSend": true
  },
  "temperature": 25
}
```

### Key sections:
- **Satellite** – over-the-air traffic counters (messages and bytes sent/received via satellite).  
- **Location** – GPS-derived position, with visible satellites.  
- **Jamming** – interference detection.  
- **SatelliteTxStatus** – the authoritative transmission state (`canSend`).  
- **Temperature** – modem health metric.

---

## 3) Data Aggregation & Transfer

### Aggregation
Events are aggregated into:

```
Map<String, List<byte[]>>
```

- **Key** = topic name.  
- **Value** = rolling list of the most recent payloads (depth is configurable).  

This differs from standard messaging (per-event forwarding) — here we create **topic-scoped rolling snapshots**, bundled for efficient satellite transfer.

---

### Outbound Flow
1. Build batch from aggregated map.  
2. Append **CRC** to the raw batch.  
3. **Compress**.  
4. **Encrypt** (optional).  
5. Transmit if `canSend = true`; otherwise buffer with backoff.  
6. Discard policy is governed by configuration (e.g., time-to-live).

### Inbound Flow
1. **Decrypt** (if used).  
2. **Decompress**.  
3. **Verify CRC** (on raw, original data).  
4. Rehydrate map and republish by topic.

### Batch Semantics
- **Batch creation** can be triggered by:
  - **Size threshold** (batch too large → cut and send).  
  - **Time window** (periodic flush).  
  - **Explicit flush event**.  
- Each batch is uniquely identified:  
  - **SIN** = batch number.  
  - **MIN** = position within the batch.

---

### Delivery Guarantees
- Provides **“at-most once, latest state per topic”** semantics.  
- Optimized for:
  - **Freshness** (most recent view of each topic).  
  - **Efficiency** (compression, coalescing).  
- Not designed for **per-event guaranteed delivery** — that would be wasteful and unnecessary on constrained satellite links.

---

## 4) Website / Session Handling

- Each modem endpoint is assigned a **dedicated session**.  
- A session represents a remote Maps Server accessible via satellite.  
- Benefits:  
  - Direct addressing of individual modems.  
  - Per-endpoint isolation of stats and events.  
  - Clean linkage between the UI, modem, and remote server.

---

## 5) Integration with Server-to-Server Framework

- The modem integration builds on the **existing server-to-server message framework**.  
- Topics and filters are configured in the same way as any server-to-server link.  
- **Difference**:  
  - Satellite transport adds **poll intervals** and **priority handling**.  
  - High-priority events can be sent immediately, while normal traffic is polled and batched.  
- Configuration specifics (poll times, priority flags, discard windows) are covered in the **configuration document**, not here.
