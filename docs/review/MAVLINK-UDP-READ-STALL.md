# MAVLink UDP read path stalls under sustained ingest

**Status:** suspected bug, not yet root-caused. Filed for triage.
**Observed on:** `maps 4.5.0-SNAPSHOT~20260804.1237` + `maps-drone 1.4.0`, Java 21
(eclipse-temurin:21-jre), Linux x86_64, 2 vCPU / 4 GB single-node cloud VM.

## Summary

After 20–40 minutes of sustained MAVLink ingest (~360 msg/s over UDP), the server
**stops reading its MAVLink UDP socket**. Datagrams accumulate unread in the
kernel receive queue while the broker stays otherwise responsive — REST, MQTT and
the web UIs continue to serve normally. Ingest does not recover on its own.
Restarting the *sender* does not clear it; only restarting the broker does.

Because MAVLink stops flowing, digital twins pass `retentionTimeoutMillis` and are
removed, so the operational symptom is "all vehicles disappear from the map while
the server looks healthy".

## Evidence

Captured while the fault was active.

**1. Kernel receive queue backed up — the server is not draining the socket:**

```
$ ss -u -a | grep '127.0.0.1.:14450'
UNCONN 188864 0   [::ffff:127.0.0.1]:14450   *:*
```

Recv-Q held between 170,000 and 211,000 bytes across three consecutive samples a
minute apart. In the healthy state this value is `0`.

**2. Every selector thread is idle — nothing is blocked:**

`kill -3 <pid>` during the stall shows all `SelectorThread` instances parked in
`EPoll.wait`, e.g.

```
"SelectorThread" #53 [817] prio=5 os_prio=0 cpu=0.34ms elapsed=2301.34s nid=817 runnable
   java.lang.Thread.State: RUNNABLE
        at sun.nio.ch.EPoll.wait(java.base@21.0.11/Native Method)
        at sun.nio.ch.EPollSelectorImpl.doSelect(java.base@21.0.11/EPollSelectorImpl.java:121)
        at sun.nio.ch.SelectorImpl.lockAndDoSelect(java.base@21.0.11/SelectorImpl.java:130)
        - locked <0x0000000763956960> (a sun.nio.ch.Util$2)
        - locked <0x0000000763956910> (a sun.nio.ch.EPollSelectorImpl)
```

No thread is blocked on a lock, on the store, or on I/O. There is no deadlock
(`jvm_threads_deadlocked` = 0).

**3. The host is idle** — `load average: 0.10`, ~95% CPU idle, no iowait. The
process is not starved; it simply is not being woken for that socket.

**4. No exception is logged.** Nothing appears at ERROR/WARN correlating with the
stall.

**5. Resources are healthy.** Heap ~150 MB used of a 2560 MB max; file descriptors
~730; disk under 15% used.

## Frequency (24.6 h continuous observation)

The supervisor described below logs every detection, which gives a measured
fault rate rather than an impression. Over **24.6 hours** of continuous replay at
~360 msg/s:

| | |
|---|---|
| Faults detected | **19** |
| Mean time between faults | **82 min** |
| Median | **56 min** |
| Shortest / longest gap | **22 min** / **288 min** |
| Recv-Q at detection | 163 KB – 214 KB (median 202 KB) |
| Total downtime | ~10 min (**0.6 %** of the window) |

Intervals in minutes, in order:

```
171, 37, 288, 55, 43, 68, 32, 62, 53, 228, 66, 69, 47, 55, 72, 49, 22, 56
```

The spread is wide and does not correlate with anything obvious — not with load
(constant), client connections (sporadic, often none), or the replay's own
capture-loop boundary (~13 min at this speed, so several passes complete inside
even the shortest gap). It reproduces with no external clients connected at all,
so it is not triggered by subscriber activity.

Note that the queue depth at detection is tightly clustered (163–214 KB) simply
because the supervisor samples every 60 s and fires on the third consecutive
reading; it is not a ceiling. Left alone the queue keeps growing.

## What has been ruled out

| Hypothesis | Result |
|---|---|
| Destination store back-pressure (`Partition`) | **Ruled out** — reproduces identically with `type: memory` |
| Store write failure | **Ruled out** — occurs with zero "Failed to store" log lines |
| Disk / IO saturation | **Ruled out** — host idle, load 0.10, no iowait |
| Heap exhaustion / GC death-spiral | **Ruled out** — heap ~150 MB of 2560 MB |
| File-descriptor exhaustion | **Ruled out** — ~730 open, limit 1,000,000 |
| Sender fault | **Ruled out** — sender holds one stable socket; restarting it does not clear the stall; the kernel queue keeps growing, so datagrams *are* arriving |
| Per-source protocol-instance churn | **Unlikely** — sender uses a single fixed source port for its lifetime |

The remaining suspicion is that the UDP endpoint's selector registration is lost
or cancelled without the endpoint being torn down, so the socket stays open and
buffered but is never selected for read again.

## Reproduction

1. Configure a MAVLink UDP listener:

   ```yaml
   - name: "Mavlink Interface"
     url: udp://0.0.0.0:14450/
     protocol: mavlink
     dialectName: "ardupilot/ardupilotmega"
     topicNameTemplate: "/mavlink/{systemId}/{messageName}"
     selectorThreadCount: "{processors}/2"
   ```

2. Replay a MAVLink capture at ~360 msg/s sustained (3 vehicles, all message
   types) into `udp://127.0.0.1:14450` in a continuous loop.

3. Watch `ss -u -a | grep ':14450'` and the destination publish rate.

Within 20–40 minutes Recv-Q begins to climb and never returns to zero; publish
rate on `/mavlink/#` falls to near zero.

## Notes that may be relevant

* `selectorThreadCount: "{processors}/2"` resolves to **1** on a 2-vCPU host.
  Whether the fault also occurs with a larger, explicitly-set selector pool has
  not yet been tested.
* The MAVLink listener creates a protocol instance per source endpoint; JMX
  registration for these emits `InstanceAlreadyExistsException` warnings
  (`Unable to register MBean … remoteHost=udp_/localhost/127.0.0.1_<port>`),
  which suggests instances are recreated for an endpoint that already exists.
  Possibly unrelated, but it is the only anomaly seen around the MAVLink path.

## Operational workaround in use

A supervisor samples the socket's receive queue every 60s and restarts the broker
after three consecutive readings above 100 KB:

```bash
RQ=$(ss -u -a | grep "127.0.0.1.:14450" | awk '{print $2}' | head -1)
# 3 strikes > 100000 bytes -> restart broker
```

Confirmed working: detection to re-armed took ~4 seconds, with roughly a 30 second
outage. Raising `TwinManager.retentionTimeoutMillis` above the detection window
keeps twins on the map across the restart instead of them being removed.

## Secondary observations

Two smaller items noticed during this investigation, both minor but worth a look:

1. **Swallowed exception.** `MavlinkProtocol` logs
   `Failed to store MAVLink message on topic '<topic>': {}` — the `{}` placeholder
   is never substituted, so the underlying exception is lost. This made an earlier
   line of investigation considerably harder.

2. **Log severity.** Routine lifecycle events are logged at ERROR/WARN — e.g.
   `ERROR … Destination /mavlink/3/TIMESYNC created`,
   `ERROR … anonymous successfully logged off`,
   `WARN … Created Protocol WS`. A healthy server produces a log that reads as
   though it is failing, which makes real faults hard to spot.
