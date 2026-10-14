/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.state.LimitedMessageStateManager;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.queue.EventReaperQueue;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import java.io.File;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionDeliveryCleanupTest {
  @TempDir(factory = TargetDataTempDirFactory.class) Path tempDir;

  @Test void offlineExpiryRetiresBothIndexesDurablyWithoutTransport() throws Exception {
    PersistentSession owner = persistent();
    DestinationSubscription subscription = subscription(owner, state());
    String key = subscription.getDeliveryKey();
    PacketIdStore store = owner.getOrCreatePacketIdStore(32);
    int packetId = store.allocate(key, 42).packetId();
    subscription.hibernate();
    owner.close();
    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      subscription.expired(42);
    }
    try (PacketIdStore restored = PacketIdStore.open(packetPath(owner), 32, PacketIdStore.Durability.BATCHED)) {
      assertNull(restored.getPacketId(key, 42));
      assertNull(restored.getDelivery(packetId));
    }
  }

  @Test void capacityEvictionWhileHibernatingClearsItsDeliveryMapping() throws Exception {
    PersistentSession owner = persistent();
    LimitedMessageStateManager state = new LimitedMessageStateManager("limited", 2,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE), 1, new EventReaperQueue());
    DestinationSubscription subscription = subscription(owner, state);
    state.register(42);
    String key = subscription.getDeliveryKey();
    PacketIdStore store = owner.getOrCreatePacketIdStore(32);
    int packetId = store.allocate(key, 42).packetId();
    subscription.hibernate();
    owner.close();
    state.register(43);
    try (PacketIdStore restored = PacketIdStore.open(packetPath(owner), 32, PacketIdStore.Durability.BATCHED)) {
      assertNull(restored.getDelivery(packetId));
      assertNull(restored.getPacketId(key, 42));
    }
  }

  @Test void hibernationPreservesReplayAndPermanentContextRemovalRetiresIt() throws Exception {
    PersistentSession owner = persistent();
    DestinationSubscription subscription = subscription(owner, state());
    String key = subscription.getDeliveryKey();
    int packetId = owner.getOrCreatePacketIdStore(32).allocate(key, 42).packetId();
    subscription.hibernate();
    assertEquals(packetId, owner.getPacketIdStore().getPacketId(key, 42));
    subscription.removeContext(subscription.getContext());
    assertNull(owner.getPacketIdStore().getDelivery(packetId));
    owner.close();
  }

  @Test void restartBeforeFirstReconnectRetiresExpiryAndCapacityButPreservesPubcompStage() throws Exception {
    Path journal = tempDir.resolve("restored.packet-ids");
    LimitedMessageStateManager state = new LimitedMessageStateManager("restored", 3,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE), 1, new EventReaperQueue());
    DestinationSubscription restored = subscription(null, state);
    restored.setDeliveryJournalPath(journal);
    String key = restored.getDeliveryKey();
    state.register(42);
    int evicted;
    int expired;
    int awaitingPubcomp;
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      evicted = store.allocate(key, 42).packetId();
      expired = store.allocate(key, 43).packetId();
      awaitingPubcomp = store.allocate(key, 44).packetId();
      store.setStage(awaitingPubcomp, 1);
    }
    // The restored subscription has never held a live SessionImpl.
    state.register(43);
    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      restored.expired(43);
      restored.expired(44);
    }
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      assertNull(store.getDelivery(evicted));
      assertNull(store.getPacketId(key, 42));
      assertNull(store.getDelivery(expired));
      assertNull(store.getPacketId(key, 43));
      assertEquals(awaitingPubcomp, store.getPacketId(key, 44));
      assertEquals(1, store.getStage(awaitingPubcomp));
    }
    restored.removeContext(restored.getContext());
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      assertEquals(0, store.size());
    }
  }

  @Test void controllerAttachesJournalToExistingAndFuturePhysicalSubscriptions() throws Exception {
    SessionContext context = new SessionContext("restored-controller", mock(ClientConnection.class));
    context.setSecurityContext(mock(SecurityContext.class));
    TestController controller;
    // Management registration requires the running daemon; delivery state does not.
    try (var ignored = mockConstruction(io.mapsmessaging.admin.SubscriptionControllerJMX.class)) {
      controller = new TestController(context);
    }
    DestinationSubscription existing = subscription(null, state());
    controller.addPhysical(existing);
    Path journal = tempDir.resolve("controller.packet-ids");
    controller.setDeliveryJournalPath(journal);
    DestinationSubscription later = subscription(null, state());
    controller.addPhysical(later);
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      store.allocate(existing.getDeliveryKey(), 42);
      store.allocate(later.getDeliveryKey(), 43);
    }
    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      existing.expired(42);
      later.expired(43);
    }
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      assertEquals(0, store.size());
    }
    controller.shutdown();
  }

  private static class TestController extends SubscriptionController {
    TestController(SessionContext context) {
      super(context, mock(DestinationManager.class), new java.util.HashMap<>());
    }
    void addPhysical(DestinationSubscription subscription) {
      updateSubscriptionManager(subscription.getContext(), subscription.getDestinationImpl(), subscription);
    }
  }

  @Test void concurrentOfflineDestinationsAndReconnectShareJournalOwnership() throws Exception {
    Path journal = tempDir.resolve("concurrent.packet-ids");
    DeliveryStateCoordinator coordinator = new DeliveryStateCoordinator();
    coordinator.setPath(journal);
    DestinationSubscription first = subscription(null, state(), "sensor/first");
    DestinationSubscription second = subscription(null, state(), "sensor/second");
    first.setDeliveryStateCoordinator(coordinator);
    second.setDeliveryStateCoordinator(coordinator);
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      store.allocate(first.getDeliveryKey(), 41);
      store.allocate(second.getDeliveryKey(), 42);
      store.allocate(first.getDeliveryKey(), 43);
      store.allocate(second.getDeliveryKey(), 44);
    }
    var workers = java.util.concurrent.Executors.newFixedThreadPool(3);
    try {
      var a = workers.submit(() -> first.completeMessage(41));
      var b = workers.submit(() -> second.completeMessage(42));
      a.get(5, java.util.concurrent.TimeUnit.SECONDS);
      b.get(5, java.util.concurrent.TimeUnit.SECONDS);
      ClientConnection connection = mock(ClientConnection.class);
      doAnswer(call -> {
        assertFalse(Thread.holdsLock(coordinator));
        return null;
      }).when(connection).deliveryRetired(anyString(), anyLong());
      var opening = new java.util.concurrent.CountDownLatch(1);
      var proceed = new java.util.concurrent.CountDownLatch(1);
      var attached = workers.submit(() -> coordinator.attach(65535, () -> {
        opening.countDown();
        try {
          if (!proceed.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new java.io.IOException("latch timeout");
        } catch (InterruptedException error) {
          Thread.currentThread().interrupt();
          throw new java.io.IOException(error);
        }
        return PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED);
      }, connection));
      assertTrue(opening.await(5, java.util.concurrent.TimeUnit.SECONDS));
      a = workers.submit(() -> first.completeMessage(43));
      b = workers.submit(() -> second.completeMessage(44));
      proceed.countDown();
      PacketIdStore live = attached.get(5, java.util.concurrent.TimeUnit.SECONDS);
      a.get(5, java.util.concurrent.TimeUnit.SECONDS);
      b.get(5, java.util.concurrent.TimeUnit.SECONDS);
      assertEquals(0, live.size());
      verify(connection).deliveryRetired(first.getDeliveryKey(), 43);
      verify(connection).deliveryRetired(second.getDeliveryKey(), 44);
      coordinator.close(live);
      try (PacketIdStore recovered = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
        assertEquals(0, recovered.size());
      }
    } finally {
      workers.shutdownNow();
    }
  }

  @Test void restoredSharedMemberUsesCoordinatorForExpiryWithoutSession() throws Exception {
    Path journal = tempDir.resolve("shared-restored.packet-ids");
    DeliveryStateCoordinator coordinator = new DeliveryStateCoordinator();
    coordinator.setPath(journal);
    var shared = mock(io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscription.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("sensor/shared");
    when(shared.getDestinationImpl()).thenReturn(destination);
    SubscriptionContext context = new SubscriptionContext("sensor/#");
    context.setSharedName("workers");
    var member = new io.mapsmessaging.engine.destination.subscription.impl.shared.SessionSharedSubscription(
        shared, null, "restored", context, mock(AcknowledgementController.class));
    member.setDeliveryStateCoordinator(coordinator);
    String key = member.getDeliveryKey();
    int packetId;
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      packetId = store.allocate(key, 42).packetId();
    }
    member.expired(42);
    try (PacketIdStore recovered = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      assertNull(recovered.getPacketId(key, 42));
      assertNull(recovered.getDelivery(packetId));
    }
  }

  @Test void bulkRetirementNotifiesOnlySnapshotAndPreservesNewDelivery() throws Exception {
    DeliveryStateCoordinator coordinator = new DeliveryStateCoordinator();
    ClientConnection connection = mock(ClientConnection.class);
    PacketIdStore store = coordinator.attach(65535, () -> PacketIdStore.memory(65535), connection);
    String key = "exact-subscription";
    store.allocate(key, 41);
    store.allocate(key, 42);
    java.util.Set<Long> runtime = new java.util.HashSet<>(java.util.Set.of(41L, 42L));
    doAnswer(call -> {
      assertFalse(Thread.holdsLock(coordinator));
      assertFalse(Thread.holdsLock(store));
      if (!runtime.contains(43L)) {
        store.allocate(key, 43);
        runtime.add(43L);
      }
      runtime.remove(call.<Long>getArgument(1));
      return null;
    }).when(connection).deliveryRetired(eq(key), anyLong());
    coordinator.retireAll(key);
    assertEquals(java.util.Set.of(43L), runtime);
    assertNotNull(store.getPacketId(key, 43));
    verify(connection).deliveryRetired(key, 41);
    verify(connection).deliveryRetired(key, 42);
    verify(connection, never()).deliveriesRetired(anyString());
    verify(connection, never()).deliveryRetired(key, 43);
    coordinator.close(store);
  }

  @Test void recoveredAcknowledgementWaitsForActualTaskRetiresRestAndKeepsOtherDestination() throws Exception {
    SessionContext context = new SessionContext("recovered-controller", mock(ClientConnection.class));
    context.setSecurityContext(mock(SecurityContext.class));
    TestController controller;
    try (var ignored = mockConstruction(io.mapsmessaging.admin.SubscriptionControllerJMX.class)) {
      controller = new TestController(context);
    }
    Path interestFile = tempDir.resolve("recovered-interest.bin");
    var interestFactory = new io.mapsmessaging.utilities.collections.bitset.ConcurrentSharedFileBitSetFactoryImpl(
        interestFile.toString(), 4, Constants.BITSET_BLOCK_SIZE);
    DestinationSubscription first = subscription(null,
        new MessageStateManagerImpl("recovered", 1, interestFactory), "sensor/first");
    DestinationSubscription second = subscription(null, state(), "sensor/second");
    controller.addPhysical(first);
    controller.addPhysical(second);
    Path journal = tempDir.resolve("recovered-ack.packet-ids");
    controller.setDeliveryJournalPath(journal);
    first.getMessageStateManager().register(42);
    second.getMessageStateManager().register(42);
    java.util.List<io.mapsmessaging.engine.tasks.EngineTask> tasks = new java.util.ArrayList<>();
    when(first.getDestinationImpl().submit(any(io.mapsmessaging.engine.tasks.EngineTask.class))).thenAnswer(call -> {
      tasks.add(call.getArgument(0));
      return java.util.concurrent.CompletableFuture.completedFuture(new io.mapsmessaging.engine.tasks.VoidResponse());
    });
    int packetId;
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      packetId = store.allocate(first.getDeliveryKey(), 42).packetId();
      store.setStage(packetId, 1);
    }
    var delivery = new PacketIdStore.Delivery(first.getDeliveryKey(), 42);
    var completion = controller.acknowledgeRecoveredDelivery(delivery);
    assertFalse(completion.isDone());
    assertTrue(first.getMessageStateManager().hasMessage(42));
    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      tasks.removeFirst().taskCall();
    }
    completion.join();
    assertFalse(first.getMessageStateManager().hasMessage(42));
    assertTrue(second.getMessageStateManager().hasMessage(42));
    var repeated = controller.acknowledgeRecoveredDelivery(delivery);
    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      tasks.removeFirst().taskCall();
    }
    repeated.join();
    try (PacketIdStore store = PacketIdStore.open(journal, 65535, PacketIdStore.Durability.BATCHED)) {
      assertEquals(packetId, store.getPacketId(delivery.topic(), 42));
      assertEquals(1, store.getStage(packetId));
    }
    controller.shutdown();
    interestFactory.close();
    var reopenedFactory = new io.mapsmessaging.utilities.collections.bitset.ConcurrentSharedFileBitSetFactoryImpl(
        interestFile.toString(), 4, Constants.BITSET_BLOCK_SIZE);
    var recoveredInterest = new MessageStateManagerImpl("recovered", 1, reopenedFactory);
    assertFalse(recoveredInterest.hasMessage(42));
    recoveredInterest.close();
    reopenedFactory.close();
  }

  @Test void engineFailureFailsRecoveredAcknowledgementFuture() throws Exception {
    var state = mock(MessageStateManagerImpl.class);
    doThrow(new IllegalStateException("interest commit failed")).when(state).commit(42);
    DestinationSubscription subscription = subscription(null, state);
    java.util.List<io.mapsmessaging.engine.tasks.EngineTask> tasks = new java.util.ArrayList<>();
    when(subscription.getDestinationImpl().submit(any(io.mapsmessaging.engine.tasks.EngineTask.class))).thenAnswer(call -> {
      tasks.add(call.getArgument(0));
      return java.util.concurrent.CompletableFuture.completedFuture(new io.mapsmessaging.engine.tasks.VoidResponse());
    });
    var completion = subscription.acknowledgeRecoveredDelivery(42);
    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      assertThrows(IllegalStateException.class, () -> tasks.removeFirst().taskCall());
    }
    assertTrue(completion.isCompletedExceptionally());
    assertThrows(java.util.concurrent.CompletionException.class, completion::join);
  }

  @Test void ackRetiresDeliveryWhileQos2AwaitingPubcompSurvivesEngineCommit() throws Exception {
    PersistentSession owner = persistent();
    DestinationSubscription subscription = subscription(owner, state());
    String key = subscription.getDeliveryKey();
    PacketIdStore store = owner.getOrCreatePacketIdStore(32);
    int qos1 = store.allocate(key, 42).packetId();
    int qos2 = store.allocate(key, 43).packetId();
    store.setStage(qos2, 1);
    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      subscription.ackReceived(42);
      subscription.ackReceived(43);
      subscription.expired(43);
    }
    assertNull(store.getPacketId(key, 42));
    assertNull(store.getDelivery(qos1));
    assertEquals(qos2, store.getPacketId(key, 43));
    owner.close();
    try (PacketIdStore restored = PacketIdStore.open(packetPath(owner), 32, PacketIdStore.Durability.BATCHED)) {
      assertNull(restored.getDelivery(qos1));
      assertEquals(qos2, restored.getPacketId(key, 43));
      assertEquals(1, restored.getStage(qos2));
    }
  }

  @Test void cleanupForAnotherProtocolNeverInitializesPacketStorage() throws Exception {
    PersistentSession owner = persistent();
    DestinationSubscription subscription = subscription(owner, state());
    try (MockedStatic<ThreadLocalContext> ignored = mockStatic(ThreadLocalContext.class)) {
      subscription.expired(42);
      subscription.completeMessage(43);
    }
    assertNull(owner.getPacketIdStore());
    assertFalse(java.nio.file.Files.exists(packetPath(owner)));
    owner.close();
  }

  private PersistentSession persistent() {
    SessionContext context = new SessionContext("cleanup", mock(ClientConnection.class));
    context.setUniqueId("cleanup-session");
    context.setExpiry(60);
    return new PersistentSession(context, mock(SecurityContext.class), mock(DestinationManager.class),
        mock(SubscriptionController.class), new PersistentSessionManager(tempDir + File.separator));
  }
  private MessageStateManagerImpl state() {
    return new MessageStateManagerImpl("state", 1, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
  }
  private DestinationSubscription subscription(SessionImpl owner, MessageStateManagerImpl state) {
    return subscription(owner, state, "sensor/one");
  }
  private DestinationSubscription subscription(SessionImpl owner, MessageStateManagerImpl state, String name) {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn(name);
    return new DestinationSubscription(destination, new SubscriptionContext("sensor/#"), owner,
        "cleanup", mock(AcknowledgementController.class), state, false);
  }
  private Path packetPath(PersistentSession owner) {
    return Path.of(owner.getStoreName().replaceFirst("\\.bin$", ".packet-ids"));
  }
}
