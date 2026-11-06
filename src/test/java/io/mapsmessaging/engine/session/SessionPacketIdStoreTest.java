/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.session;

import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionPacketIdStoreTest {
  @TempDir(factory = TargetDataTempDirFactory.class)
  Path tempDir;

  @Test
  void memoryStoreIsSharedByRepeatedTransportAttachmentsAndReleasedOnlyBySession() throws Exception {
    SessionImpl session = new SessionImpl(context(false), mock(SecurityContext.class), mock(DestinationManager.class), mock(SubscriptionController.class));
    PacketIdStore first = session.getOrCreatePacketIdStore(32);
    int packetId = first.allocate("sensor", 42).packetId();
    assertSame(first, session.getOrCreatePacketIdStore(32));
    assertEquals(packetId, session.getOrCreatePacketIdStore(32).getPacketId("sensor", 42));
    session.close();
    assertThrows(IOException.class, () -> first.size());
    assertThrows(IOException.class, () -> session.getOrCreatePacketIdStore(32));
  }

  @Test
  void persistentDisconnectRetainsRecordsReleasesOwnershipAndRestartRestoresDeliveryOnlySession() throws Exception {
    PersistentSessionManager manager = manager();
    PersistentSession session = persistent(manager, false);
    PacketIdStore store = session.getOrCreatePacketIdStore(32);
    int packetId = store.allocate("sensor", 42).packetId();
    assertSame(store, session.getOrCreatePacketIdStore(32));
    Path state = packetPath(session);
    assertThrows(IOException.class, () -> PacketIdStore.open(state, 32, PacketIdStore.Durability.BATCHED));
    session.close();
    assertTrue(Files.isRegularFile(state));
    PersistentSessionManager restoredManager = manager();
    assertNotNull(restoredManager.getSessionDetails("client"));
    PersistentSession restored = persistent(restoredManager, false);
    try {
      assertEquals(packetId, restored.getOrCreatePacketIdStore(32).getPacketId("sensor", 42));
      restored.setExpiryTime(System.currentTimeMillis() + 60000);
      assertTrue(Files.isRegularFile(Path.of(restored.getStoreName())));
    } finally {
      restored.close();
    }
  }

  @Test
  void cleanStartRetiresDeliveryStateEvenBeforeStoreIsAccessed() throws Exception {
    PersistentSessionManager manager = manager();
    PersistentSession old = persistent(manager, false);
    old.getOrCreatePacketIdStore(32).allocate("sensor", 42);
    Path path = packetPath(old);
    old.close();
    PersistentSession fresh = persistent(manager, true);
    try {
      assertFalse(Files.exists(path));
      assertNull(fresh.getOrCreatePacketIdStore(32).getPacketId("sensor", 42));
    } finally {
      fresh.close();
    }
  }

  @Test
  void expiredDeliveryOnlySessionDeletesItsJournalAndMetadata() throws Exception {
    PersistentSessionManager manager = manager();
    PersistentSession session = persistent(manager, false);
    session.getOrCreatePacketIdStore(32).allocate("sensor", 42);
    Path path = packetPath(session);
    session.setExpiryTime(1);
    session.close();
    SubscriptionController controller = mock(SubscriptionController.class);
    when(controller.getSessionId()).thenReturn("client");
    when(controller.isPersistent()).thenReturn(true);
    SubscriptionControllerFactory factory = mock(SubscriptionControllerFactory.class);
    when(factory.create(eq("client"), any(), any(), anyMap())).thenReturn(controller);
    LongAdder expired = new LongAdder();
    SessionManagerPipeLine pipeline = new SessionManagerPipeLine(mock(DestinationManager.class), manager,
        new LongAdder(), new LongAdder(), expired, mock(ExecutorService.class), mock(SessionExpiryScheduler.class),
        new SessionStateFileStore(), factory, mock(SessionFactory.class), mock(WillTaskManager.class));
    pipeline.addDisconnectedSession("client", manager.getSessionDetails("client"), Map.of());
    assertEquals(1, expired.sum());
    assertFalse(Files.exists(path));
    assertFalse(Files.exists(Path.of(session.getStoreName())));
    assertNull(manager.getSessionDetails("client"));
    verify(controller).close(false);
  }

  @Test
  void offlineEngineCleanupReopensExistingStateAndPreservesAwaitingPubCompUntilUnsubscribe() throws Exception {
    PersistentSession session = persistent(manager(), false);
    PacketIdStore store = session.getOrCreatePacketIdStore(32);
    int accepted = store.allocate("sensor", 42).packetId();
    store.setStage(accepted, 1);
    store.allocate("sensor", 43);
    store.allocate("other", 44);
    Path path = packetPath(session);
    session.close();
    assertNull(session.getPacketIdStore());
    session.clearPacketIdMapping("sensor", 42);
    session.clearPacketIdMapping("sensor", 43);
    try (PacketIdStore restored = PacketIdStore.open(path, 32, PacketIdStore.Durability.BATCHED)) {
      assertEquals(accepted, restored.getPacketId("sensor", 42));
      assertNull(restored.getPacketId("sensor", 43));
    }
    session.clearPacketIdMappings("sensor");
    try (PacketIdStore restored = PacketIdStore.open(path, 32, PacketIdStore.Durability.BATCHED)) {
      assertNull(restored.getPacketId("sensor", 42));
      assertNotNull(restored.getPacketId("other", 44));
    }
  }

  @Test
  void openingDeliveryStoreDoesNotTruncateExistingSessionMetadata() throws Exception {
    PersistentSession session = persistent(manager(), false);
    Path metadata = Path.of(session.getStoreName());
    byte[] existing = new byte[]{7, 8, 9, 10};
    Files.write(metadata, existing);
    try {
      session.getOrCreatePacketIdStore(32);
      assertArrayEquals(existing, Files.readAllBytes(metadata));
    } finally {
      session.close();
    }
  }

  private PersistentSession persistent(PersistentSessionManager manager, boolean reset) {
    return new PersistentSession(context(reset), mock(SecurityContext.class), mock(DestinationManager.class), mock(SubscriptionController.class), manager);
  }

  private SessionContext context(boolean reset) {
    SessionContext context = new SessionContext("client", mock(ClientConnection.class));
    context.setUniqueId("stable-session-id");
    context.setExpiry(60);
    context.setResetState(reset);
    return context;
  }

  private PersistentSessionManager manager() {
    return new PersistentSessionManager(tempDir + File.separator);
  }

  private Path packetPath(PersistentSession session) {
    return Path.of(session.getStoreName().replaceFirst("\\.bin$", ".packet-ids"));
  }
}
