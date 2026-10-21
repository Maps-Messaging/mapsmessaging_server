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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2AckCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PublishCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import java.nio.file.Path;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/** CSD01 MQTT-SN 4.3.3-1, 4.3.4-1..8, and 4.4.2-1..7 behavior. */
class MqttSn2OutgoingDeliveryManagerTest {

  @TempDir(factory = TargetDataTempDirFactory.class) Path directory;

  @Test
  void pubrec_blocks_retry_and_pubcomp_while_engine_ack_is_being_submitted() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
      manager.attach(store);
      int identifier = publish(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, null, 1000)).packetIdentifier();
      CompletableFuture<Void> engineAck = new CompletableFuture<>();
      CompletableFuture<ByteBuffer> response = manager.acknowledgeAfterBrokerAck(
          new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, identifier, null), 1001, delivery -> {
            assertNull(manager.retryExpired(1101), "PUBREL cannot leave while its engine ACK is being submitted");
            assertThrows(java.io.IOException.class, () -> manager.acknowledgeAfterBrokerAck(
                new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, identifier, null), 1101, ignored -> engineAck));
            return engineAck;
          });
      assertFalse(response.isDone());
      engineAck.complete(null);
      assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(response.join()).type());
      assertNotNull(manager.retryExpired(1201));
      manager.acknowledgeAfterBrokerAck(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, identifier, null), 1202,
          ignored -> CompletableFuture.completedFuture(null)).join();
      assertEquals(0, store.size());
    }
  }

  @Test
  void failed_engine_ack_submission_keeps_pubrel_blocked_and_durable() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
      manager.attach(store);
      int identifier = publish(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, null, 1000)).packetIdentifier();
      CompletableFuture<ByteBuffer> response = manager.acknowledgeAfterBrokerAck(
          new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, identifier, null), 1001,
          delivery -> { throw new IllegalStateException("Engine ACK submission rejected"); });
      assertThrows(java.util.concurrent.CompletionException.class, response::join);
      assertNull(manager.retryExpired(1101));
      assertEquals(1, store.getStage(identifier));
      manager.disconnect();
      manager.attach(store);
      ByteBuffer recovered = manager.recoverAfterBrokerAck(delivery -> CompletableFuture.completedFuture(null), 1200).join();
      assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(recovered).type());
    }
  }

  @Test
  void closeWhileRecoveredEngineAckPendingDoesNotSendPubrelOrStartNewConnectionState() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    int id = store.allocate("subscription", 41).packetId();
    store.setStage(id, 1);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager();
    manager.attach(store);
    CompletableFuture<Void> engineAck = new CompletableFuture<>();
    CompletableFuture<ByteBuffer> recovery = manager.recoverAfterBrokerAck(delivery -> engineAck, 1000);
    manager.disconnect();
    manager.attach(store);
    engineAck.complete(null);
    assertNull(recovery.join());
    assertFalse(manager.hasInFlightDelivery());
    assertEquals(1, store.getStage(id));
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(manager.recover(2000)).type());
  }

  @Test
  void closeWhileNormalEngineAckPendingRetainsPubrelStageAndEmitsNoFrame() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager();
    manager.attach(store);
    int id = publish(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, null, 1000)).packetIdentifier();
    CompletableFuture<Void> engineAck = new CompletableFuture<>();
    CompletableFuture<ByteBuffer> response = manager.acknowledgeAfterBrokerAck(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, null), 1001,
        delivery -> engineAck);
    manager.disconnect();
    engineAck.complete(null);
    assertNull(response.join());
    assertFalse(manager.hasInFlightDelivery());
    assertEquals(1, store.getStage(id));
  }

  @Test
  void permanentRetirementDuringEngineAckDoesNotSendOldPubrelOrBlockNextRetry() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    manager.attach(store);
    int id = publish(manager.enqueue("retired-subscription", 41, "sensor/one", new byte[]{1}, 2, false, null, 1000)).packetIdentifier();
    manager.enqueue("other-subscription", 42, "sensor/one", new byte[]{2}, 1, false, null, 1001);
    CompletableFuture<Void> engineAck = new CompletableFuture<>();
    CompletableFuture<ByteBuffer> pending = manager.acknowledgeAfterBrokerAck(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, null), 1002,
        delivery -> engineAck);
    store.clear("retired-subscription", 41);
    MqttSn2PublishCodec.Publish next = publish(manager.retireDelivery("retired-subscription", 41, 1003));
    assertNotNull(manager.retryExpired(1103));
    engineAck.complete(null);
    assertNull(pending.join());
    assertEquals(next.packetIdentifier(), store.getPacketId("other-subscription", 42));
  }

  @Test
  void recoveredEngineAckSuppressesLegacyCallbacksFromLateBrokerReplay() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    int id = store.allocate("subscription", 41).packetId();
    store.setStage(id, 1);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager();
    manager.attach(store);
    ByteBuffer recovery = manager.recoverAfterBrokerAck(delivery -> CompletableFuture.completedFuture(null), 1000).join();
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(recovery).type());
    manager.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, id, null), 1001);
    AtomicInteger legacyCallback = new AtomicInteger();
    assertNull(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, legacyCallback::incrementAndGet, 1002));
    assertEquals(0, legacyCallback.get());
    assertEquals(0, store.size());
  }

  @Test
  void normalPubrelWaitsForActualEngineAckAndDoesNotRunLegacyCallbackAgain() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    manager.attach(store);
    AtomicInteger legacyCallback = new AtomicInteger();
    int id = publish(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, legacyCallback::incrementAndGet, 1000)).packetIdentifier();
    CompletableFuture<Void> engineAck = new CompletableFuture<>();
    CompletableFuture<ByteBuffer> response = manager.acknowledgeAfterBrokerAck(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, null), 1001,
        delivery -> {
          assertEquals(new PacketIdStore.Delivery("subscription", 41), delivery);
          try { assertEquals(1, store.getStage(id)); } catch (java.io.IOException failure) { throw new AssertionError(failure); }
          return engineAck;
        });
    assertFalse(response.isDone());
    assertNull(manager.retryExpired(1101), "PUBREL retries must also wait for the engine ACK");
    assertThrows(java.io.IOException.class, () -> manager.acknowledgeAfterBrokerAck(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, id, null), 1101,
        delivery -> CompletableFuture.completedFuture(null)));
    engineAck.complete(null);
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(response.join()).type());
    manager.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, id, null), 1102);
    assertEquals(0, legacyCallback.get(), "The exact engine ACK replaces the legacy completion task");
    assertEquals(0, store.size());
  }

  @Test
  void restoredPubrelWaitsForAllEngineAckFuturesAndFailureRetainsJournalStage() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    int first = store.allocate("subscription", 41).packetId();
    int second = store.allocate("subscription", 42).packetId();
    store.setStage(first, 1);
    store.setStage(second, 1);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    manager.attach(store);
    CompletableFuture<Void> firstAck = new CompletableFuture<>();
    CompletableFuture<Void> secondAck = new CompletableFuture<>();
    CompletableFuture<ByteBuffer> response = manager.recoverAfterBrokerAck(delivery -> delivery.messageId() == 41 ? firstAck : secondAck, 1000);
    assertFalse(response.isDone());
    assertFalse(manager.hasInFlightDelivery());
    firstAck.complete(null);
    assertFalse(response.isDone());
    secondAck.completeExceptionally(new java.io.IOException("Engine ACK failed"));
    assertThrows(java.util.concurrent.CompletionException.class, response::join);
    assertFalse(manager.hasInFlightDelivery());
    assertEquals(1, store.getStage(first));
    assertEquals(1, store.getStage(second));
    manager.disconnect();
    manager.attach(store);
    ByteBuffer recovered = manager.recoverAfterBrokerAck(delivery -> CompletableFuture.completedFuture(null), 2000).join();
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(recovered).type());
  }

  @Test
  void delayedRetirementDoesNotDropSameIdentityReallocatedBeforeNotification() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    manager.attach(store);
    int oldId = publish(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 1, false, null, 1000)).packetIdentifier();
    store.clear("subscription", 41);
    manager.disconnect();
    manager.attach(store);
    int currentId = publish(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 1, false, null, 2000)).packetIdentifier();
    assertNotEquals(oldId, currentId);
    assertNull(manager.retireDelivery("subscription", 41, 2001));
    assertTrue(manager.hasInFlightDelivery());
    assertEquals(currentId, publish(manager.retryExpired(2100)).packetIdentifier());
    assertEquals(currentId, store.getPacketId("subscription", 41));
    manager.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, currentId, null), 2101);
    assertEquals(0, store.size());
  }

  @Test
  void broadRetirementSkipsFreshMappedIdentityUnderSameKey() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    manager.attach(store);
    int id = publish(manager.enqueue("subscription", 42, "sensor/one", new byte[]{2}, 1, false, null, 1000)).packetIdentifier();
    assertNull(manager.retireDeliveries("subscription", 1001));
    assertTrue(manager.hasInFlightDelivery());
    assertEquals(id, publish(manager.retryExpired(1100)).packetIdentifier());
    assertEquals(id, store.getPacketId("subscription", 42));
  }

  @Test
  void reconnectPreservesOriginalQosOneWhenCurrentSubscriptionWouldSendQosZero() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    MqttSn2OutgoingDeliveryManager original = new MqttSn2OutgoingDeliveryManager();
    original.attach(store);
    int id = publish(original.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 1, false, null, 1000)).packetIdentifier();
    MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager();
    restored.attach(store);
    MqttSn2PublishCodec.Publish replay = publish(restored.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 0, false, null, 2000));
    assertEquals(1, replay.qos());
    assertEquals(id, replay.packetIdentifier());
    assertTrue(replay.duplicate());
    assertNull(restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, id, null), 2001));
    assertEquals(0, store.size());
  }

  @Test
  void restartPreservesOriginalWireQosWhenSubscriptionDowngradesToZero() throws Exception {
    Path file = directory.resolve("mqtt-sn2-original-qos.journal");
    int id;
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      MqttSn2OutgoingDeliveryManager original = new MqttSn2OutgoingDeliveryManager();
      original.attach(store);
      id = publish(original.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, null, 1000)).packetIdentifier();
      assertEquals(2, store.getQos(id));
      store.compact();
    }
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager();
      restored.attach(store);
      assertEquals(2, restored.getOriginalQos("subscription", 41));
      MqttSn2PublishCodec.Publish replay = publish(restored.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 0, false, null, 2000));
      assertEquals(2, replay.qos());
      assertTrue(replay.duplicate());
      assertEquals(id, replay.packetIdentifier());
      assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, null), 2001)).type());
    }
  }

  @Test
  void runtimeRetirementDropsExpiredInFlightAndStartsUnsentDeliveryWithoutClearingReusedId() throws Exception {
    PacketIdStore store = PacketIdStore.memory(1);
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager();
    manager.attach(store);
    AtomicInteger expiredCompletion = new AtomicInteger();
    int id = publish(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 1, false, expiredCompletion::incrementAndGet, 1000)).packetIdentifier();
    manager.enqueue("subscription", 42, "sensor/one", new byte[]{2}, 1, false, null, 1001);
    store.clear("subscription", 41);
    int reused = store.allocate("other-subscription", 99).packetId();
    assertEquals(id, reused);
    assertThrows(java.io.IOException.class, () -> manager.retireDelivery("subscription", 41, 1002));
    assertEquals(reused, store.getPacketId("other-subscription", 99));
    assertEquals(0, expiredCompletion.get());
    store.clear("other-subscription", 99);
    MqttSn2PublishCodec.Publish next = publish(manager.recover(1003));
    assertFalse(next.duplicate());
    assertEquals(id, next.packetIdentifier());
    assertEquals(id, store.getPacketId("subscription", 42));
  }

  @Test
  void bulkRuntimeRetirementDropsQueuedAndRecoveredDeliveriesForOnlyItsSubscription() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    int id = store.allocate("retired-subscription", 41).packetId();
    store.setStage(id, 1);
    MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager();
    restored.attach(store);
    restored.recover(1000);
    restored.enqueue("retired-subscription", 42, "sensor/one", new byte[]{2}, 1, false, null, 1001);
    restored.enqueue("other-subscription", 43, "sensor/one", new byte[]{3}, 1, false, null, 1001);
    store.clear("retired-subscription", 41);
    MqttSn2PublishCodec.Publish next = publish(restored.retireDeliveries("retired-subscription", 1002));
    assertFalse(next.duplicate());
    assertEquals(next.packetIdentifier(), store.getPacketId("other-subscription", 43));
    assertNull(store.getPacketId("retired-subscription", 42));
  }

  @Test
  void recoveredDeliveryBindsCompletionEvenWhenResubscriptionDowngradesQos() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    int id = store.allocate("subscription", 41).packetId();
    store.setStage(id, 1);
    MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager(100, 3);
    restored.attach(store);
    restored.recover(1000);
    AtomicInteger completed = new AtomicInteger();
    assertTrue(restored.bindRecoveredCompletion("subscription", 41, completed::incrementAndGet));
    assertFalse(restored.bindRecoveredCompletion("subscription", 42, completed::incrementAndGet));
    restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, id, null), 1001);
    assertEquals(1, completed.get());
  }

  @Test
  void recoveredPubrelCanFinishBeforeBrokerReplayAndCompletesLateDeliveryExactlyOnce() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    int id = store.allocate("subscription", 41).packetId();
    store.setStage(id, 1);
    MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager(100, 3);
    restored.attach(store);
    ByteBuffer recovery = restored.recover(1000);
    assertEquals(id, MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(recovery)).packetIdentifier());
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(recovery).type());
    assertNull(restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, id, null), 1001));
    AtomicInteger completed = new AtomicInteger();
    assertNull(restored.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, completed::incrementAndGet, 1002));
    assertEquals(1, completed.get());
    assertFalse(restored.hasInFlightDelivery());
    assertEquals(0, store.size());
  }

  @Test
  void recoveredPubrelsSerializeBeforeNewPublishesAndBindLiveEngineCallbacks() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    int firstId = store.allocate("subscription", 41).packetId();
    int secondId = store.allocate("subscription", 42).packetId();
    store.setStage(firstId, 1);
    store.setStage(secondId, 1);
    MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager(100, 3);
    restored.attach(store);
    ByteBuffer recovery = restored.recover(1000);
    int activeId = MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(recovery)).packetIdentifier();
    long activeMessage = store.getDelivery(activeId).messageId();
    AtomicInteger completed = new AtomicInteger();
    assertNull(restored.recover(1001), "Only one outstanding request may be sent");
    assertNull(restored.enqueue("subscription", activeMessage, "sensor/one", new byte[]{1}, 2, false, completed::incrementAndGet, 1001));
    assertNull(restored.enqueue("subscription", 43, "sensor/one", new byte[]{3}, 1, false, null, 1001));
    ByteBuffer second = restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, activeId, null), 1002);
    assertEquals(1, completed.get());
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(second).type());
    int otherId = MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(second)).packetIdentifier();
    assertNotEquals(activeId, otherId);
    MqttSn2PublishCodec.Publish next = publish(restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, otherId, null), 1003));
    assertFalse(next.duplicate());
    assertEquals(1, next.qos());
  }

  @Test
  void orphanedPubrelRetriesExhaustWithoutDeletingDurableStage() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    int id = store.allocate("expired-subscription", 41).packetId();
    store.setStage(id, 1);
    MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager(100, 1);
    restored.attach(store);
    restored.recover(1000);
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(restored.retryExpired(1100)).type());
    assertNull(restored.retryExpired(1200));
    assertTrue(restored.isRetryExhausted());
    restored.disconnect();
    assertEquals(id, store.getPacketId("expired-subscription", 41));
    assertEquals(1, store.getStage(id));
  }

  @Test
  void reconnectReusesStableDeliveryIdentifierAndDuplicatesOnlyPreviouslyStartedDelivery() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    MqttSn2OutgoingDeliveryManager original = new MqttSn2OutgoingDeliveryManager(100, 3);
    original.attach(store);
    MqttSn2PublishCodec.Publish first = publish(original.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 1, true, null, 1000));
    assertFalse(first.duplicate());
    assertNull(original.enqueue("subscription", 42, "sensor/one", new byte[]{2}, 1, false, null, 1000));
    assertNull(store.getPacketId("subscription", 42), "Queued deliveries must not acquire identifiers before their first send");

    MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager(100, 3);
    restored.attach(store);
    MqttSn2PublishCodec.Publish replay = publish(restored.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 1, true, null, 2000));
    assertEquals(first.packetIdentifier(), replay.packetIdentifier());
    assertTrue(replay.duplicate());
    restored.enqueue("subscription", 42, "sensor/one", new byte[]{2}, 1, false, null, 2000);
    MqttSn2PublishCodec.Publish next = publish(restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, replay.packetIdentifier(), null), 2001));
    assertFalse(next.duplicate());
    assertNull(store.getPacketId("subscription", 41));
    assertEquals(next.packetIdentifier(), store.getPacketId("subscription", 42));
  }

  @Test
  void serverRestartWhileAwaitingPubcompReplaysPubrelAndCleansMappingOnAck() throws Exception {
    Path file = directory.resolve("mqtt-sn2-packets.journal");
    int id;
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      MqttSn2OutgoingDeliveryManager original = new MqttSn2OutgoingDeliveryManager(100, 3);
      original.attach(store);
      id = publish(original.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, null, 1000)).packetIdentifier();
      original.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, null), 1001);
      assertEquals(1, store.getStage(id));
      store.compact();
    }
    AtomicInteger completed = new AtomicInteger();
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      MqttSn2OutgoingDeliveryManager restored = new MqttSn2OutgoingDeliveryManager(100, 3);
      restored.attach(store);
      ByteBuffer frame = restored.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 2, false, completed::incrementAndGet, 2000);
      assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(frame).type());
      assertEquals(id, MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(frame)).packetIdentifier());
      assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(restored.retryExpired(2100)).type());
      ByteBuffer repeatedPubrec = restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, null), 2100);
      assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(repeatedPubrec).type());
      assertEquals(id, MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(repeatedPubrec)).packetIdentifier());
      restored.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, id, null), 2101);
      assertEquals(1, completed.get());
      assertEquals(0, store.size());
    }
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      assertEquals(0, store.size());
    }
  }

  @Test
  void cleanSessionDoesNotReplayPriorDelivery() throws Exception {
    MqttSn2OutgoingDeliveryManager original = new MqttSn2OutgoingDeliveryManager();
    original.attach(PacketIdStore.memory(65535));
    original.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 1, false, null, 1000);
    MqttSn2OutgoingDeliveryManager clean = new MqttSn2OutgoingDeliveryManager();
    clean.attach(PacketIdStore.memory(65535));
    assertFalse(publish(clean.enqueue("subscription", 41, "sensor/one", new byte[]{1}, 1, false, null, 2000)).duplicate());
  }

  @Test
  void qosOneRetriesPreserveAllPublishProperties() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    PacketIdStore store = PacketIdStore.memory(65535);
    manager.attach(store);
    MqttSn2PublishCodec.Publish initial = publish(manager.enqueue("subscription", 41, "sensor/one", new byte[]{1, 2}, 1, true, null, 1000));
    MqttSn2PublishCodec.Publish retry = publish(manager.retryExpired(1100));
    assertTrue(retry.duplicate());
    assertEquals(initial.packetIdentifier(), retry.packetIdentifier());
    assertEquals(initial.qos(), retry.qos());
    assertEquals(initial.retained(), retry.retained());
    assertEquals(initial.topicName(), retry.topicName());
    assertArrayEquals(initial.payload(), retry.payload());
  }

  private static MqttSn2PublishCodec.Publish publish(ByteBuffer frame) throws Exception {
    return MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(frame));
  }

  @Test
  void qos_two_delivery_uses_pubrec_pubrel_pubcomp_before_releasing_next_publish() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    AtomicInteger firstCompleted = new AtomicInteger();
    AtomicInteger secondCompleted = new AtomicInteger();

    ByteBuffer first = manager.enqueue("sensor/one", new byte[]{1}, 2, false,
        firstCompleted::incrementAndGet, 1_000);
    ByteBuffer second = manager.enqueue("sensor/two", new byte[]{2}, 1, false,
        secondCompleted::incrementAndGet, 1_000);

    assertNotNull(first);
    assertNull(second, "CSD01 permits one outstanding request packet per sender");
    MqttSn2PublishCodec.Publish sent = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(first));
    assertEquals(2, sent.qos());
    int packetIdentifier = sent.packetIdentifier();
    assertTrue(packetIdentifier > 0);

    ByteBuffer pubRel = manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBREC, packetIdentifier, null), 1_010);
    assertNotNull(pubRel);
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(pubRel).type());
    assertEquals(0, firstCompleted.get());

    ByteBuffer next = manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBCOMP, packetIdentifier, null), 1_020);
    assertEquals(1, firstCompleted.get());
    assertNotNull(next);
    assertEquals(1, MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(next)).qos());
    assertEquals(0, secondCompleted.get());
  }

  @Test
  void qos_two_retries_pubrel_without_retransmitting_publish_after_pubrec() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    ByteBuffer publish = manager.enqueue("sensor/one", new byte[]{1}, 2, false, null, 1_000);
    int packetIdentifier = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(publish)).packetIdentifier();
    ByteBuffer pubRel = manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBREC, packetIdentifier, null), 1_001);

    ByteBuffer retry = manager.retryExpired(1_101);

    assertEquals(MqttSn2FrameCodec.decode(pubRel).type(), MqttSn2FrameCodec.decode(retry).type());
    assertEquals(packetIdentifier, MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(retry)).packetIdentifier());
  }

  @Test
  void failed_pubrec_completes_delivery_attempt() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    AtomicInteger completed = new AtomicInteger();
    ByteBuffer publish = manager.enqueue("sensor/one", new byte[]{1}, 2, false,
        completed::incrementAndGet, 1_000);
    int packetIdentifier = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(publish)).packetIdentifier();

    ByteBuffer next = manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBREC, packetIdentifier, 0x80), 1_001);

    assertNull(next);
    assertEquals(1, completed.get());
    assertFalse(manager.hasInFlightDelivery());
  }

  @Test
  void negative_puback_completes_delivery_attempt() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager();
    AtomicInteger completed = new AtomicInteger();
    ByteBuffer first = manager.enqueue("sensor/one", new byte[]{1}, 1, false,
        completed::incrementAndGet, 1_000);
    int id = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(first)).packetIdentifier();
    manager.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, id, 0x80), 1_001);
    assertEquals(1, completed.get());
    assertFalse(manager.hasInFlightDelivery());
  }

  @Test
  void qos_one_retry_sets_dup_and_reuses_packet_identifier() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 2);
    ByteBuffer initial = manager.enqueue("sensor/one", new byte[]{1}, 1, false, () -> {}, 1_000);
    MqttSn2PublishCodec.Publish sent = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(initial));

    ByteBuffer retry = manager.retryExpired(1_100);
    MqttSn2PublishCodec.Publish resent = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(retry));
    assertTrue(resent.duplicate());
    assertEquals(sent.packetIdentifier(), resent.packetIdentifier());
    assertArrayEquals(sent.payload(), resent.payload());
  }
  @Test
  void wrong_acknowledgement_and_identifier_leave_delivery_pending() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    AtomicInteger completed = new AtomicInteger();
    ByteBuffer publish = manager.enqueue("sensor/one", new byte[]{1}, 2, false,
        completed::incrementAndGet, 1_000);
    int id = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(publish)).packetIdentifier();
    assertThrows(java.io.IOException.class, () -> manager.acknowledge(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, id, null), 1_001));
    assertThrows(java.io.IOException.class, () -> manager.acknowledge(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id + 1, null), 1_002));
    assertTrue(manager.hasInFlightDelivery());
    assertEquals(0, completed.get());

    ByteBuffer rel = manager.acknowledge(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, null), 1_003);
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(rel).type());
    assertThrows(java.io.IOException.class, () -> manager.acknowledge(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, id, null), 1_004));
    assertEquals(0, completed.get());
    manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBCOMP, id, null), 1_005);
    assertEquals(1, completed.get());
  }

  @Test
  void retriesStopAtConfiguredLimitWithoutReportingSuccess() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 2);
    AtomicInteger completed = new AtomicInteger();
    manager.enqueue("sensor/one", new byte[]{1}, 1, false,
        completed::incrementAndGet, 1_000);
    assertNull(manager.retryExpired(1_099));
    assertNotNull(manager.retryExpired(1_100));
    assertNotNull(manager.retryExpired(1_200));
    assertNull(manager.retryExpired(1_300));
    assertTrue(manager.isRetryExhausted());
    assertEquals(0, completed.get());
  }

  @Test
  void negative_pubcomp_completes_delivery_attempt() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 2);
    AtomicInteger completed = new AtomicInteger();
    ByteBuffer initial = manager.enqueue("sensor/one", new byte[]{1}, 2, false,
        completed::incrementAndGet, 1_000);
    int id = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(initial)).packetIdentifier();
    manager.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, null), 1_001);
    manager.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBCOMP, id, 0x80), 1_002);
    assertEquals(1, completed.get());
    assertFalse(manager.hasInFlightDelivery());
  }

  @Test
  void terminalNegativePubackCompletesExactlyOnce() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 2);
    AtomicInteger completed = new AtomicInteger();
    ByteBuffer publish = manager.enqueue("sensor/one", new byte[]{1}, 1, false,
        completed::incrementAndGet, 1_000);
    int id = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(publish)).packetIdentifier();

    manager.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, id, 0x80), 1_001);

    assertEquals(1, completed.get());
    assertThrows(java.io.IOException.class, () -> manager.acknowledge(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, id, 0x80), 1_002));
    assertEquals(1, completed.get());
  }

  @Test
  void terminalNegativePubrecCompletesExactlyOnceWithoutPubrel() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 2);
    AtomicInteger completed = new AtomicInteger();
    ByteBuffer publish = manager.enqueue("sensor/one", new byte[]{1}, 2, false,
        completed::incrementAndGet, 1_000);
    int id = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(publish)).packetIdentifier();

    assertNull(manager.acknowledge(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, 0x80), 1_001));

    assertEquals(1, completed.get());
    assertThrows(java.io.IOException.class, () -> manager.acknowledge(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBREC, id, 0x80), 1_002));
    assertEquals(1, completed.get());
  }

}
