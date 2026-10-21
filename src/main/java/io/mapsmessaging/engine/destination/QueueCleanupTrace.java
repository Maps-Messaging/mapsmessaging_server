/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.engine.destination;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/** Temporary, bounded diagnostic recorder. Only the selected test destination is recorded. */
public final class QueueCleanupTrace implements AutoCloseable {

  private static final int CAPACITY = 32768;
  private static volatile QueueCleanupTrace active;

  private final String destination;
  private final long started = System.nanoTime();
  private final Queue<Runnable> finalSnapshots = new ConcurrentLinkedQueue<>();
  private final AtomicLong sequence = new AtomicLong();
  private final AtomicReferenceArray<String> entries = new AtomicReferenceArray<>(CAPACITY);

  private QueueCleanupTrace(String destination) {
    this.destination = destination;
  }

  public static QueueCleanupTrace start(String destination) {
    QueueCleanupTrace trace = new QueueCleanupTrace(destination);
    active = trace;
    return trace;
  }

  public static boolean isEnabled(String destination) {
    QueueCleanupTrace trace = active;
    return trace != null && trace.destination.equals(destination);
  }

  public static void record(String destination, String action, long messageId, Object... details) {
    QueueCleanupTrace trace = active;
    if (trace == null || !trace.destination.equals(destination)) {
      return;
    }
    long index = trace.sequence.getAndIncrement();
    String entry = index + " +" + (System.nanoTime() - trace.started) + "ns [" + Thread.currentThread().getName()
        + "] " + action + " id=" + messageId + " " + Arrays.toString(details);
    trace.entries.set((int) (index % CAPACITY), entry);
  }

  public static void captureOnFinish(String destination, Runnable snapshot) {
    QueueCleanupTrace trace = active;
    if (trace != null && trace.destination.equals(destination)) {
      trace.finalSnapshots.add(snapshot);
    }
  }

  public void captureFinalState() {
    for (Runnable snapshot : finalSnapshots) {
      try {
        snapshot.run();
      } catch (RuntimeException diagnosticFailure) {
        record(destination, "SNAPSHOT-FAILED", -1, diagnosticFailure.toString());
      }
    }
  }

  public void writeTo(Path path) throws IOException {
    Files.createDirectories(path.getParent());
    long end = sequence.get();
    StringBuilder output = new StringBuilder("destination=" + destination + " events=" + end + " capacity=" + CAPACITY + "\n");
    for (long index = Math.max(0, end - CAPACITY); index < end; index++) {
      String entry = entries.get((int) (index % CAPACITY));
      if (entry != null) {
        output.append(entry).append('\n');
      }
    }
    Files.writeString(path, output, StandardCharsets.UTF_8);
  }

  @Override
  public void close() {
    if (active == this) {
      active = null;
    }
  }
}
