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

package io.mapsmessaging.state.drone.tak;

import java.util.concurrent.atomic.LongAdder;

/**
 * Counters for one TAK server connection. {@link TakOutputStats} aggregates across every
 * connection; these are kept per server so a failing secondary TAK server is visible on its own
 * instead of being mixed into the primary server's figures.
 */
final class TakServerStats {

  private final LongAdder connectCount = new LongAdder();
  private final LongAdder connectFailureCount = new LongAdder();
  private final LongAdder disconnectCount = new LongAdder();
  private final LongAdder droppedCount = new LongAdder();
  private final LongAdder writeCount = new LongAdder();
  private volatile long lastWriteMillis = 0L;

  void recordConnect() {
    connectCount.increment();
  }

  void recordConnectFailure() {
    connectFailureCount.increment();
  }

  void recordDisconnect() {
    disconnectCount.increment();
  }

  void recordDrop() {
    droppedCount.increment();
  }

  void recordWrite() {
    writeCount.increment();
    lastWriteMillis = System.currentTimeMillis();
  }

  long getConnectCount() {
    return connectCount.sum();
  }

  long getConnectFailureCount() {
    return connectFailureCount.sum();
  }

  long getDisconnectCount() {
    return disconnectCount.sum();
  }

  long getDroppedCount() {
    return droppedCount.sum();
  }

  long getWriteCount() {
    return writeCount.sum();
  }

  /** -1 if nothing has been written to this server yet. */
  long getLastWriteAgeMillis() {
    long at = lastWriteMillis;
    return at == 0L ? -1L : System.currentTimeMillis() - at;
  }
}
