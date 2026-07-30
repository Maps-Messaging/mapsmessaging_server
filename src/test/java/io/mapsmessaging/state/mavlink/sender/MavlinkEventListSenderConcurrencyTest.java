/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.state.mavlink.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.model.UxvModelCommandSet;
import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MavlinkEventListSenderConcurrencyTest {

  @Test
  void concurrentInboundPacketsAreProcessedSerially() throws Exception {
    MavlinkMessage message = mock(MavlinkMessage.class);
    AtomicInteger activeCalls = new AtomicInteger();
    AtomicInteger maximumActiveCalls = new AtomicInteger();
    CountDownLatch firstEntered = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);

    MavlinkAcknowledgementHandler handler = new MavlinkAcknowledgementHandler() {
      @Override
      public boolean requiresAcknowledgement(MavlinkMessage sentMessage) {
        return true;
      }

      @Override
      public Acknowledgement acknowledge(MavlinkMessage sentMessage, MavlinkPacket receivedMessage) {
        int active = activeCalls.incrementAndGet();
        maximumActiveCalls.accumulateAndGet(active, Math::max);
        firstEntered.countDown();
        try {
          assertTrue(releaseFirst.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS));
          return Acknowledgement.notRelated();
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw new AssertionError(exception);
        } finally {
          activeCalls.decrementAndGet();
        }
      }
    };

    MavlinkEventListSender sender =
        new MavlinkEventListSender(
            UxvModelCommandSet.of(UxvOperation.BUILD_MISSION, "test-model", List.of(message)),
            ignored -> {},
            handler,
            ignored -> {},
            0,
            10_000L);
    sender.start();

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> first = executor.submit(() -> sender.onMavlinkMessage(mock(MavlinkPacket.class)));
      assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
      Future<?> second = executor.submit(() -> sender.onMavlinkMessage(mock(MavlinkPacket.class)));

      Thread.sleep(50L);
      assertEquals(1, maximumActiveCalls.get());

      releaseFirst.countDown();
      first.get(2, TimeUnit.SECONDS);
      second.get(2, TimeUnit.SECONDS);
      assertEquals(1, maximumActiveCalls.get());
    } finally {
      releaseFirst.countDown();
      sender.cancel();
      executor.shutdownNow();
    }
  }
}
