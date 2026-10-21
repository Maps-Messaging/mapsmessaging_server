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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketIdManagerRecoveryTest {
  @TempDir(factory = TargetDataTempDirFactory.class) Path directory;

  @Test void reconnect_reuses_original_id_across_topics_and_marks_only_old_deliveries() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(5)) {
      var a = subscription("a"); var b = subscription("b");
      PacketIdManager initial = new PacketIdManager(5); initial.bindStore(store);
      int first = initial.nextPacketIdentifier(a, 1);
      int second = initial.nextPacketIdentifier(b, 1);
      assertNotEquals(first, second); assertFalse(initial.isExistingPacketId(first));
      initial.close();
      PacketIdManager resumed = new PacketIdManager(5); resumed.bindStore(store);
      assertEquals(second, resumed.nextPacketIdentifier(subscription("b"), 1));
      assertTrue(resumed.isExistingPacketId(second));
      assertEquals(first, resumed.nextPacketIdentifier(subscription("a"), 1));
      assertTrue(resumed.isExistingPacketId(first));
      int offline = resumed.nextPacketIdentifier(a, 2);
      assertFalse(resumed.isExistingPacketId(offline));
      resumed.completePacketId(first);
      assertNull(store.getPacketId(a.getDeliveryKey(), 1));
    }
  }

  @Test void restart_preserves_id_and_pubrel_stage_after_engine_interest_completed() throws Exception {
    Path file = directory.resolve("delivery"); int packetId;
    try (PacketIdStore store = PacketIdStore.open(file, 5, PacketIdStore.Durability.BATCHED)) {
      PacketIdManager manager = new PacketIdManager(5); manager.bindStore(store);
      packetId = manager.nextPacketIdentifier(subscription("a"), 7);
      manager.markAwaitingPubComp(packetId);
    }
    try (PacketIdStore store = PacketIdStore.open(file, 5, PacketIdStore.Durability.BATCHED)) {
      PacketIdManager manager = new PacketIdManager(5); manager.bindStore(store);
      assertEquals(java.util.List.of(packetId), manager.getAwaitingPubCompPacketIds());
      assertNull(manager.receivedPacket(packetId).getSubscription());
      assertEquals(7, manager.receivedPacket(packetId).getMessageId());
      manager.completePacketId(packetId); assertEquals(0, store.size());
    }
  }

  @Test void control_reservations_do_not_collide_and_close_releases_only_controls() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      PacketIdManager manager = new PacketIdManager(3); manager.bindStore(store);
      int delivery = manager.nextPacketIdentifier(subscription("a"), 1);
      int control = manager.nextControlPacketIdentifier();
      assertNotEquals(delivery, control);
      manager.close(); assertEquals(1, store.size());
      int reusedControl = store.reservePacketId();
      assertNotEquals(delivery, reusedControl);
    }
  }

  @Test void unknown_ack_must_not_erase_an_unbound_recovered_mapping() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      int identifier = store.allocate("unbound", 1).packetId();
      PacketIdManager manager = new PacketIdManager(3); manager.bindStore(store);
      assertNull(manager.completePacketId(identifier));
      assertEquals(identifier, store.getPacketId("unbound", 1));
    }
  }

  @Test void discard_with_removed_context_uses_active_mapping_identity() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      var subscription = subscription("a");
      PacketIdManager manager = new PacketIdManager(3); manager.bindStore(store);
      manager.nextPacketIdentifier(subscription, 1);
      when(subscription.getContext()).thenReturn(null);
      manager.discard(subscription, 1);
      assertEquals(0, store.size());
    }
  }

  @Test void recovered_identity_bypasses_capacity_but_new_deliveries_remain_blocked() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      var old = subscription("a");
      int identifier = store.allocate(old.getDeliveryKey(), 7).packetId();
      store.setQos(identifier, 2); store.setStage(identifier, 1);
      PacketIdManager manager = new PacketIdManager(3); manager.bindStore(store); manager.setMaximumOutstanding(1);
      assertTrue(manager.tryAcquireSendSlot(subscription("a"), 7));
      assertFalse(manager.tryAcquireSendSlot(subscription("b"), 8));
      manager.completePacketId(identifier);
      assertTrue(manager.tryAcquireSendSlot(old, 7));
      assertTrue(manager.consumeCompletedRecovery(old, 7));
      assertFalse(manager.consumeCompletedRecovery(old, 7));
    }
  }

  @Test void expiry_releases_runtime_capacity_and_wakes_waiting_subscription() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      PacketIdManager manager = new PacketIdManager(3); manager.bindStore(store); manager.setMaximumOutstanding(1);
      var expired = subscription("a"); var waiting = subscription("b");
      manager.nextPacketIdentifier(expired, 1);
      assertFalse(manager.tryAcquireSendSlot(waiting));
      assertTrue(store.clearPendingDelivery(expired.getDeliveryKey(), 1));
      manager.retireDelivery(expired.getDeliveryKey(), 1);
      assertEquals(0, manager.size()); verify(waiting).resumeDelivery();
      assertTrue(manager.tryAcquireSendSlot(waiting));
      manager.nextPacketIdentifier(waiting, 2);
      manager.retireDelivery(expired.getDeliveryKey(), 1);
      assertEquals(1, manager.size());
    }
  }

  @Test void stale_endpoint_close_after_store_owner_close_is_safe() throws Exception {
    PacketIdStore store = PacketIdStore.memory(3);
    PacketIdManager manager = new PacketIdManager(3); manager.bindStore(store); manager.nextControlPacketIdentifier();
    store.close(); assertDoesNotThrow(manager::close); assertEquals(0, manager.size());
  }

  @Test void original_wire_qos_is_preserved_on_reconnect() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      PacketIdManager first = new PacketIdManager(3); first.bindStore(store);
      int identifier = first.nextPacketIdentifier(subscription("a"), 1); first.setPacketQos(identifier, 2); first.close();
      PacketIdManager next = new PacketIdManager(3); next.bindStore(store);
      assertEquals(identifier, next.getPacketIdentifier(subscription("a"), 1));
      assertEquals(2, next.getPacketQos(identifier));
      assertThrows(IllegalArgumentException.class, () -> next.setPacketQos(identifier, 1));
    }
  }

  @Test void delayed_retirement_callback_preserves_reallocated_identity() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      PacketIdManager manager = new PacketIdManager(3); manager.bindStore(store);
      var subscription = subscription("a");
      manager.nextPacketIdentifier(subscription, 1);
      store.clearPendingDelivery(subscription.getDeliveryKey(), 1);
      int replacement = manager.nextPacketIdentifier(subscription, 1);
      manager.retireDelivery(subscription.getDeliveryKey(), 1);
      assertNotNull(manager.receivedPacket(replacement));
      assertEquals(1, manager.size());
      assertEquals(replacement, store.getPacketId(subscription.getDeliveryKey(), 1));
    }
  }

  @Test void restored_publish_records_acquire_new_connection_receive_quota() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(3)) {
      var first = subscription("a"); var second = subscription("b");
      int firstId = store.allocate(first.getDeliveryKey(), 1).packetId(); store.setQos(firstId, 1);
      int secondId = store.allocate(second.getDeliveryKey(), 2).packetId(); store.setQos(secondId, 1);
      PacketIdManager manager = new PacketIdManager(3); manager.bindStore(store); manager.setMaximumOutstanding(1);
      assertTrue(manager.tryAcquireSendSlot(first, 1));
      assertEquals(firstId, manager.nextPacketIdentifier(first, 1, 1));
      assertFalse(manager.tryAcquireSendSlot(second, 2));
      manager.completePacketId(firstId);
      verify(second).resumeDelivery();
      assertTrue(manager.tryAcquireSendSlot(second, 2));
      assertEquals(secondId, manager.nextPacketIdentifier(second, 2, 1));
    }
  }

  private SubscribedEventManager subscription(String topic) {
    var subscription = mock(SubscribedEventManager.class);
    when(subscription.getContext()).thenReturn(new SubscriptionContext(topic));
    when(subscription.getDeliveryKey()).thenCallRealMethod();
    return subscription;
  }
}
