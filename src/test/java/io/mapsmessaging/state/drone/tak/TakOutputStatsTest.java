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

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TakOutputStats is process-wide, so every assertion here is on a before/after delta. */
class TakOutputStatsTest {

  @Test
  void latency_isBucketedCumulativelyWithCountAndSum() {
    long before0005 = TakOutputStats.getLatencyCumulativeCount(0);
    long before005 = TakOutputStats.getLatencyCumulativeCount(3);
    long before10 = TakOutputStats.getLatencyCumulativeCount(10);
    long beforeCount = TakOutputStats.getLatencyCount();
    double beforeSum = TakOutputStats.getLatencySumSeconds();

    TakOutputStats.recordLatencyMillis(3);
    TakOutputStats.recordLatencyMillis(30);
    TakOutputStats.recordLatencyMillis(20_000);

    assertEquals(1, TakOutputStats.getLatencyCumulativeCount(0) - before0005, "3ms lands in the 5ms bucket");
    assertEquals(2, TakOutputStats.getLatencyCumulativeCount(3) - before005, "50ms bucket is cumulative");
    assertEquals(2, TakOutputStats.getLatencyCumulativeCount(10) - before10, "20s exceeds every bucket (+Inf only)");
    assertEquals(3, TakOutputStats.getLatencyCount() - beforeCount);
    assertEquals(20.033, TakOutputStats.getLatencySumSeconds() - beforeSum, 1e-9);
  }

  @Test
  void latency_negativeValuesFromClockSkewCountAsZero() {
    long before = TakOutputStats.getLatencyCumulativeCount(0);
    double beforeSum = TakOutputStats.getLatencySumSeconds();

    TakOutputStats.recordLatencyMillis(-250);

    assertEquals(1, TakOutputStats.getLatencyCumulativeCount(0) - before);
    assertEquals(0.0, TakOutputStats.getLatencySumSeconds() - beforeSum, 1e-9);
  }

  @Test
  void socket_connectWriteAndCloseAreCounted() throws Exception {
    long connects = TakOutputStats.CONNECT_COUNT.sum();
    long disconnects = TakOutputStats.DISCONNECT_COUNT.sum();

    try (ServerSocket server = new ServerSocket(0)) {
      server.setSoTimeout(5000);
      TakSocketConnection connection = new TakSocketConnection("127.0.0.1", server.getLocalPort(), 2000, 2000, true, 4);
      try {
        // The writer only dials out once it has something to send.
        connection.accept("<event/>");
        try (Socket accepted = server.accept()) {
          awaitIncrease(() -> TakOutputStats.CONNECT_COUNT.sum(), connects);
          awaitTrue(() -> TakOutputStats.getLastWriteAgeMillis() >= 0);
        }
      } finally {
        connection.close();
      }
    }

    assertEquals(1, TakOutputStats.CONNECT_COUNT.sum() - connects);
    assertEquals(1, TakOutputStats.DISCONNECT_COUNT.sum() - disconnects, "close() tears down the established connection");
  }

  @Test
  void socket_refusedConnectionsAreCountedAsFailures() throws Exception {
    int closedPort;
    try (ServerSocket probe = new ServerSocket(0)) {
      closedPort = probe.getLocalPort();
    }
    long failures = TakOutputStats.CONNECT_FAILURE_COUNT.sum();

    TakSocketConnection connection = new TakSocketConnection("127.0.0.1", closedPort, 500, 500, true, 4);
    try {
      connection.accept("<event/>");
      awaitIncrease(() -> TakOutputStats.CONNECT_FAILURE_COUNT.sum(), failures);
    } finally {
      connection.close();
    }
  }

  @Test
  void socket_queueOverflowIsCountedAsDropped() {
    long dropped = TakOutputStats.SOCKET_DROPPED_COUNT.sum();
    // Nothing listens on port 1; the writer thread holds at most one event while it retries,
    // so pushing capacity + 2 events must overflow the 2-slot queue at least once.
    TakSocketConnection connection = new TakSocketConnection("127.0.0.1", 1, 200, 200, true, 2);
    try {
      for (int i = 0; i < 4; i++) {
        connection.accept("<event id=\"" + i + "\"/>");
      }
      assertTrue(TakOutputStats.SOCKET_DROPPED_COUNT.sum() - dropped >= 1);
    } finally {
      connection.close();
    }
  }

  private static void awaitIncrease(LongSupplier value, long from) throws InterruptedException {
    awaitTrue(() -> value.getAsLong() > from);
  }

  private static void awaitTrue(java.util.function.BooleanSupplier condition) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (!condition.getAsBoolean()) {
      assertTrue(System.currentTimeMillis() < deadline, "condition not met within 5s");
      Thread.sleep(20);
    }
  }
}
