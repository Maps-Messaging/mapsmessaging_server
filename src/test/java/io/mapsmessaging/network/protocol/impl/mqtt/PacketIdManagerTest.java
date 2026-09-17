/*
 *
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

package io.mapsmessaging.network.protocol.impl.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.mapsmessaging.api.SubscribedEventManager;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class PacketIdManagerTest {

  @Test
  void allocatesValidPacketIdentifiers() {
    PacketIdManager manager = new PacketIdManager();

    int packetIdentifier = manager.nextPacketIdentifier(null, 1L);

    assertEquals(1, packetIdentifier);
    assertEquals(1, manager.size());
    assertTrue(manager.hasAvailablePacketIdentifier());
  }

  @Test
  void fullConnectionWindowQueuesDestinationsAndWakesOnlyNext() {
    PacketIdManager manager = new PacketIdManager();
    manager.setMaximumOutstanding(2);

    SubscribedEventManager destinationA = mock(SubscribedEventManager.class);
    SubscribedEventManager destinationB = mock(SubscribedEventManager.class);
    SubscribedEventManager destinationC = mock(SubscribedEventManager.class);
    SubscribedEventManager destinationD = mock(SubscribedEventManager.class);

    assertTrue(manager.tryAcquireSendSlot(destinationA));
    int packetA = manager.nextPacketIdentifier(destinationA, 1L);
    assertTrue(manager.tryAcquireSendSlot(destinationB));
    int packetB = manager.nextPacketIdentifier(destinationB, 2L);

    assertFalse(manager.tryAcquireSendSlot(destinationC));
    assertFalse(manager.tryAcquireSendSlot(destinationD));

    manager.completePacketId(packetA);

    verify(destinationC, times(1)).resumeDelivery();
    verify(destinationD, never()).resumeDelivery();
    assertTrue(manager.tryAcquireSendSlot(destinationC));
    manager.nextPacketIdentifier(destinationC, 3L);

    manager.completePacketId(packetB);

    verify(destinationD, times(1)).resumeDelivery();
  }

  @Test
  void repeatedFullWindowChecksDoNotQueueDestinationTwice() {
    PacketIdManager manager = new PacketIdManager();
    manager.setMaximumOutstanding(1);

    SubscribedEventManager destinationA = mock(SubscribedEventManager.class);
    SubscribedEventManager destinationB = mock(SubscribedEventManager.class);

    assertTrue(manager.tryAcquireSendSlot(destinationA));
    int packetA = manager.nextPacketIdentifier(destinationA, 1L);

    assertFalse(manager.tryAcquireSendSlot(destinationB));
    assertFalse(manager.tryAcquireSendSlot(destinationB));

    manager.completePacketId(packetA);

    verify(destinationB, times(1)).resumeDelivery();
  }

  @Test
  void exhaustionWaitsWithoutBlockingPacketRelease()
      throws InterruptedException, ExecutionException, TimeoutException {
    PacketIdManager manager = createFullManager();
    int releasedPacketIdentifier = PacketIdManager.MAX_PACKET_IDENTIFIER / 2;

    try (ExecutorService allocator = Executors.newSingleThreadExecutor();
         ExecutorService acknowledger = Executors.newSingleThreadExecutor()) {
      Future<Integer> allocation = allocator.submit(
          () -> manager.nextPacketIdentifier(null, PacketIdManager.MAX_PACKET_IDENTIFIER + 1L));

      assertThrows(TimeoutException.class, () -> allocation.get(50, TimeUnit.MILLISECONDS));

      Future<?> release = acknowledger.submit(() -> manager.completePacketId(releasedPacketIdentifier));
      release.get(1, TimeUnit.SECONDS);

      assertEquals(releasedPacketIdentifier, allocation.get(1, TimeUnit.SECONDS));
    }
  }

  @Test
  void completingPacketIdentifierRestoresCapacity() {
    PacketIdManager manager = createFullManager();
    int releasedPacketIdentifier = PacketIdManager.MAX_PACKET_IDENTIFIER / 2;

    manager.completePacketId(releasedPacketIdentifier);

    assertTrue(manager.hasAvailablePacketIdentifier());
    assertEquals(
        releasedPacketIdentifier,
        manager.nextPacketIdentifier(null, PacketIdManager.MAX_PACKET_IDENTIFIER + 1L));
    assertFalse(manager.hasAvailablePacketIdentifier());
  }

  private PacketIdManager createFullManager() {
    PacketIdManager manager = new PacketIdManager();
    for (int messageId = 1; messageId <= PacketIdManager.MAX_PACKET_IDENTIFIER; messageId++) {
      int packetIdentifier = manager.nextPacketIdentifier(null, messageId);
      assertNotEquals(0, packetIdentifier);
    }
    return manager;
  }
}
