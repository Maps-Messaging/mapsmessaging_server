/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PubAckListener;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PubCompListener;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PubRecListener;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PacketListenerFactory;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubComp;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubRec;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubRel;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.UnsubAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.PubAckListener5;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.PubCompListener5;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.PubRecListener5;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.PacketListenerFactory5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubComp5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRec5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRel5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Publish5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.UnsubAck5;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MqttSessionDeliveryRecoveryTest {
  private static final AtomicInteger IDS = new AtomicInteger();
  @TempDir(factory = TargetDataTempDirFactory.class)
  Path tempDir;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void restartRetransmitsSameIdentifierWithDupAndAckDurablyRemovesIt(boolean mqtt5) throws Exception {
    Path path = tempDir.resolve("qos1.packet-ids");
    int packetId;
    try (PacketIdStore store = PacketIdStore.open(path, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture first = fixture(mqtt5, store);
      first.protocol.sendMessage(event(subscription(QualityOfService.AT_LEAST_ONCE), 71));
      ServerPacket publish = first.frames.getFirst();
      packetId = identifier(publish);
      assertFalse(duplicate(publish));
      first.manager.close();
      assertEquals(packetId, store.getPacketId("sensor", 71));
    }
    try (PacketIdStore store = PacketIdStore.open(path, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture next = fixture(mqtt5, store);
      SubscribedEventManager subscription = subscription(QualityOfService.AT_LEAST_ONCE);
      next.protocol.sendMessage(event(subscription, 71));
      ServerPacket replay = next.frames.getFirst();
      assertEquals(packetId, identifier(replay));
      assertTrue(duplicate(replay));
      Packet wire = new Packet(ByteBuffer.allocate(256));
      replay.packFrame(wire);
      wire.flip();
      assertEquals(8, wire.get() & 8);
      if (mqtt5) new PubAckListener5().handlePacket(new PubAck5(packetId, StatusCode.SUCCESS), next.session, next.endpoint, next.protocol);
      else new PubAckListener().handlePacket(new PubAck(packetId), next.session, next.endpoint, next.protocol);
      verify(subscription).ackReceived(71);
      assertEquals(0, store.size());
    }
    try (PacketIdStore store = PacketIdStore.open(path, 65535, PacketIdStore.Durability.BATCHED)) {
      assertEquals(0, store.size());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void pubrecPersistsStageBeforeEngineAckAndRestartRecoversPubrelWithoutPublish(boolean mqtt5) throws Exception {
    Path path = tempDir.resolve("qos2.packet-ids");
    int packetId;
    try (PacketIdStore store = PacketIdStore.open(path, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture first = fixture(mqtt5, store);
      SubscribedEventManager subscription = subscription(QualityOfService.EXACTLY_ONCE);
      first.protocol.sendMessage(event(subscription, 91));
      packetId = identifier(first.frames.getFirst());
      int acceptedId = packetId;
      doAnswer(invocation -> { assertEquals(1, store.getStage(acceptedId)); return null; }).when(subscription).ackReceived(91);
      when(first.session.ackRecoveredDelivery(new PacketIdStore.Delivery("sensor", 91))).thenAnswer(invocation -> {
        subscription.ackReceived(91);
        return java.util.concurrent.CompletableFuture.completedFuture(null);
      });
      Object response = mqtt5
          ? new PubRecListener5().handlePacket(new PubRec5(packetId), first.session, first.endpoint, first.protocol)
          : new PubRecListener().handlePacket(new PubRec(packetId), first.session, first.endpoint, first.protocol);
      assertNull(response);
      assertTrue(first.frames.getLast() instanceof PubRel || first.frames.getLast() instanceof PubRel5);
      if (mqtt5) new PubRecListener5().handlePacket(new PubRec5(packetId), first.session, first.endpoint, first.protocol);
      else new PubRecListener().handlePacket(new PubRec(packetId), first.session, first.endpoint, first.protocol);
      verify(subscription, times(1)).ackReceived(91);
      first.manager.close();
    }
    try (PacketIdStore store = PacketIdStore.open(path, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture next = fixture(mqtt5, store);
      if (mqtt5) ((MQTT5Protocol) next.protocol).recoverOutgoingDeliveries();
      else ((MQTTProtocol) next.protocol).recoverOutgoingDeliveries();
      assertEquals(1, next.frames.size());
      ServerPacket replay = next.frames.getFirst();
      assertEquals(packetId, mqtt5 ? ((PubRel5) replay).getPacketIdentifier() : ((PubRel) replay).getPacketIdentifier());
      SubscribedEventManager residual = subscription(QualityOfService.EXACTLY_ONCE);
      next.protocol.sendMessage(event(residual, 91));
      verify(residual, never()).ackReceived(91);
      assertEquals(2, next.frames.size());
      assertTrue(next.frames.getLast() instanceof PubRel || next.frames.getLast() instanceof PubRel5);
      assertEquals(1, store.getStage(packetId));
      if (mqtt5) new PubCompListener5().handlePacket(new PubComp5(packetId), next.session, next.endpoint, next.protocol);
      else new PubCompListener().handlePacket(new PubComp(packetId), next.session, next.endpoint, next.protocol);
      assertEquals(0, store.size());
      verify(residual, never()).ackReceived(91);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void allocationFailureClosesConnectionWithoutPublishing(boolean mqtt5) throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    Fixture fixture = fixture(mqtt5, store);
    store.close();
    fixture.protocol.sendMessage(event(subscription(QualityOfService.AT_LEAST_ONCE), 13));
    assertTrue(fixture.frames.isEmpty());
    verify(fixture.endpoint).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void qosZeroNeverAllocatesAnIdentifierOrSetsDup(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture fixture = fixture(mqtt5, store);
      fixture.protocol.sendMessage(event(subscription(QualityOfService.AT_MOST_ONCE), 31));
      fixture.protocol.sendMessage(event(subscription(QualityOfService.AT_MOST_ONCE), 31));
      assertEquals(0, store.size());
      for (ServerPacket frame : fixture.frames) {
        assertEquals(0, identifier(frame));
        assertFalse(duplicate(frame));
      }
    }
  }

  @Test
  void mqtt5RejectedPubrecCompletesDeliveryWithoutPubrel() throws Exception {
    try (PacketIdStore store = PacketIdStore.open(tempDir.resolve("rejected.packet-ids"), 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture fixture = fixture(true, store);
      SubscribedEventManager subscription = subscription(QualityOfService.EXACTLY_ONCE);
      fixture.protocol.sendMessage(event(subscription, 99));
      int packetId = identifier(fixture.frames.getFirst());
      PubRec5 rejected = new PubRec5(packetId);
      rejected.setStatusCode(StatusCode.UNSPECIFIED_ERROR);
      assertNull(new PubRecListener5().handlePacket(rejected, fixture.session, fixture.endpoint, fixture.protocol));
      verify(subscription).ackReceived(99);
      assertEquals(0, store.size());
      assertEquals(1, fixture.frames.size());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void unsubackReleasesControlReservationAndCannotClearOutstandingDelivery(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(2)) {
      Fixture fixture = fixture(mqtt5, store);
      fixture.protocol.sendMessage(event(subscription(QualityOfService.AT_LEAST_ONCE), 51));
      int deliveryId = identifier(fixture.frames.getFirst());
      int controlId = fixture.manager.nextControlPacketIdentifier();
      if (mqtt5) {
        var listener = new PacketListenerFactory5().getListener(MQTTPacket.UNSUBACK);
        listener.handlePacket(new UnsubAck5(deliveryId), fixture.session, fixture.endpoint, fixture.protocol);
        assertEquals(deliveryId, store.getPacketId("sensor", 51));
        listener.handlePacket(new UnsubAck5(controlId), fixture.session, fixture.endpoint, fixture.protocol);
      } else {
        var listener = new PacketListenerFactory().getListener(MQTTPacket.UNSUBACK);
        listener.handlePacket(new UnsubAck(deliveryId), fixture.session, fixture.endpoint, fixture.protocol);
        assertEquals(deliveryId, store.getPacketId("sensor", 51));
        listener.handlePacket(new UnsubAck(controlId), fixture.session, fixture.endpoint, fixture.protocol);
      }
      assertNull(fixture.manager.receivedPacket(controlId));
      assertEquals(controlId, fixture.manager.nextControlPacketIdentifier());
      assertEquals(1, store.size());
      fixture.manager.close();
      assertEquals(controlId, store.reservePacketId());
      store.releaseReservedPacketId(controlId);
      assertEquals(deliveryId, store.getPacketId("sensor", 51));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void outstandingPublishRetainsOriginalWireQosAfterSubscriptionDowngrade(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture first = fixture(mqtt5, store);
      first.protocol.sendMessage(event(subscription(QualityOfService.EXACTLY_ONCE), 111));
      int packetId = identifier(first.frames.getFirst());
      assertEquals(2, store.getQos(packetId));
      first.manager.close();
      Fixture restored = fixture(mqtt5, store);
      restored.protocol.sendMessage(event(subscription(QualityOfService.AT_MOST_ONCE), 111));
      ServerPacket publish = restored.frames.getFirst();
      assertEquals(packetId, identifier(publish));
      assertTrue(duplicate(publish));
      assertEquals(QualityOfService.EXACTLY_ONCE, publish instanceof Publish packet ? packet.getQos() : ((Publish5) publish).getQos());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void wrongAcknowledgementTypesCannotChangePersistentDeliveryState(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture fixture = fixture(mqtt5, store);
      SubscribedEventManager qos1 = subscription(QualityOfService.AT_LEAST_ONCE);
      fixture.protocol.sendMessage(event(qos1, 121));
      int qos1Id = identifier(fixture.frames.getFirst());
      if (mqtt5) {
        new PubRecListener5().handlePacket(new PubRec5(qos1Id), fixture.session, fixture.endpoint, fixture.protocol);
        new PubCompListener5().handlePacket(new PubComp5(qos1Id), fixture.session, fixture.endpoint, fixture.protocol);
      } else {
        new PubRecListener().handlePacket(new PubRec(qos1Id), fixture.session, fixture.endpoint, fixture.protocol);
        new PubCompListener().handlePacket(new PubComp(qos1Id), fixture.session, fixture.endpoint, fixture.protocol);
      }
      assertEquals(0, store.getStage(qos1Id));
      assertEquals(qos1Id, store.getPacketId("sensor", 121));
      verify(qos1, never()).ackReceived(anyLong());
      SubscribedEventManager qos2 = subscription(QualityOfService.EXACTLY_ONCE);
      fixture.protocol.sendMessage(event(qos2, 122));
      int qos2Id = identifier(fixture.frames.getLast());
      if (mqtt5) {
        new PubAckListener5().handlePacket(new PubAck5(qos2Id, StatusCode.SUCCESS), fixture.session, fixture.endpoint, fixture.protocol);
        new PubCompListener5().handlePacket(new PubComp5(qos2Id), fixture.session, fixture.endpoint, fixture.protocol);
      } else {
        new PubAckListener().handlePacket(new PubAck(qos2Id), fixture.session, fixture.endpoint, fixture.protocol);
        new PubCompListener().handlePacket(new PubComp(qos2Id), fixture.session, fixture.endpoint, fixture.protocol);
      }
      assertEquals(qos2Id, store.getPacketId("sensor", 122));
      verify(qos2, never()).ackReceived(anyLong());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void pubcompBeforeResidualEngineEventSuppressesNewPublishDespiteQosDowngrade(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int packetId = store.allocate("sensor", 131).packetId();
      store.setQos(packetId, 2);
      store.setStage(packetId, 1);
      Fixture fixture = fixture(mqtt5, store);
      if (mqtt5) ((MQTT5Protocol) fixture.protocol).recoverOutgoingDeliveries().join();
      else ((MQTTProtocol) fixture.protocol).recoverOutgoingDeliveries().join();
      if (mqtt5) new PubCompListener5().handlePacket(new PubComp5(packetId), fixture.session, fixture.endpoint, fixture.protocol);
      else new PubCompListener().handlePacket(new PubComp(packetId), fixture.session, fixture.endpoint, fixture.protocol);
      SubscribedEventManager residual = subscription(QualityOfService.AT_MOST_ONCE);
      fixture.protocol.sendMessage(event(residual, 131));
      verify(residual).ackReceived(131);
      assertEquals(1, fixture.frames.size());
      assertEquals(0, store.size());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void actualEngineRetrievalBypassesFullWindowOnlyForRecoveredPhysicalInterest(boolean mqtt5) throws Exception {
    SubscriptionContext context = new SubscriptionContext("sensor");
    context.setQualityOfService(QualityOfService.AT_MOST_ONCE);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("$schema/sensor");
    String key = SubscribedEventManager.deliveryKey(context, "$schema/sensor");
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int packetId = store.allocate(key, 141).packetId();
      store.setQos(packetId, 2);
      store.setStage(packetId, 1);
      Fixture fixture = fixture(mqtt5, store);
      ProtocolClientConnection connection = new ProtocolClientConnection(fixture.protocol);
      SessionImpl engineSession = mock(SessionImpl.class);
      when(engineSession.getClientConnection()).thenReturn(connection);
      MessageStateManager state = mock(MessageStateManager.class);
      EngineSubscription engine = new EngineSubscription(destination, context, engineSession, state);
      when(fixture.session.ackRecoveredDelivery(new PacketIdStore.Delivery(key, 141))).thenAnswer(invocation -> {
        engine.events().ackReceived(141);
        return java.util.concurrent.CompletableFuture.completedFuture(null);
      });
      Message expected = event(engine.events(), 141).getMessage();
      when(state.nextMessageId()).thenReturn(141L);
      when(destination.getMessage(141)).thenReturn(expected);
      fixture.manager.setMaximumOutstanding(1);
      assertFalse(connection.tryAcquireSendSlot(subscription(QualityOfService.AT_LEAST_ONCE), event(subscription(QualityOfService.AT_LEAST_ONCE), 142).getMessage()));
      assertSame(expected, engine.retrieve());
      verify(state).allocate(expected);
      fixture.protocol.sendMessage(new MessageEvent("$schema/sensor", engine.events(), expected, () -> {}));
      assertEquals(1, fixture.frames.size());
      assertTrue(fixture.frames.getFirst() instanceof PubRel || fixture.frames.getFirst() instanceof PubRel5);
      assertEquals(1, store.getStage(packetId));
      verify(destination).submit(any(io.mapsmessaging.engine.destination.subscription.tasks.SubscriptionTransactionTask.class));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void actualEngineLimitsDowngradedStageZeroReplaysUntilFirstAck(boolean mqtt5) throws Exception {
    SubscriptionContext context = new SubscriptionContext("sensor");
    context.setQualityOfService(QualityOfService.AT_MOST_ONCE);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("$schema/sensor");
    String key = SubscribedEventManager.deliveryKey(context, "$schema/sensor");
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int firstId = store.allocate(key, 181).packetId();
      int secondId = store.allocate(key, 182).packetId();
      store.setQos(firstId, 1);
      store.setQos(secondId, 1);
      Fixture fixture = fixture(mqtt5, store);
      fixture.manager.setMaximumOutstanding(1);
      SessionImpl engineSession = mock(SessionImpl.class);
      when(engineSession.getClientConnection()).thenReturn(new ProtocolClientConnection(fixture.protocol));
      MessageStateManager state = mock(MessageStateManager.class);
      EngineSubscription engine = new EngineSubscription(destination, context, engineSession, state);
      Message first = event(engine.events(), 181).getMessage();
      Message second = event(engine.events(), 182).getMessage();
      when(state.nextMessageId()).thenReturn(181L, 182L, 182L);
      when(destination.getMessage(181)).thenReturn(first);
      when(destination.getMessage(182)).thenReturn(second);
      assertSame(first, engine.retrieve());
      fixture.protocol.sendMessage(new MessageEvent("$schema/sensor", engine.events(), first, () -> {}));
      assertEquals(firstId, identifier(fixture.frames.getFirst()));
      assertNull(engine.retrieve());
      verify(state, never()).allocate(second);
      assertEquals(1, fixture.frames.size());
      if (mqtt5) new PubAckListener5().handlePacket(new PubAck5(firstId, StatusCode.SUCCESS), fixture.session, fixture.endpoint, fixture.protocol);
      else new PubAckListener().handlePacket(new PubAck(firstId), fixture.session, fixture.endpoint, fixture.protocol);
      assertSame(second, engine.retrieve());
      fixture.protocol.sendMessage(new MessageEvent("$schema/sensor", engine.events(), second, () -> {}));
      assertEquals(2, fixture.frames.size());
      assertEquals(secondId, identifier(fixture.frames.getLast()));
      assertTrue(duplicate(fixture.frames.getLast()));
    }
  }

  private static final class EngineSubscription extends DestinationSubscription {
    EngineSubscription(DestinationImpl destination, SubscriptionContext context, SessionImpl session, MessageStateManager state) {
      super(destination, context, session, "client", mock(AcknowledgementController.class), state, false);
    }

    Message retrieve() throws IOException { return retrieveNextMessageWithFlowControl(); }
    SubscribedEventManager events() { return eventStateManager; }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void outgoingConnackWaitsForDurableEngineAckBeforePubrelAndResume(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int packetId = store.allocate("sensor", 151).packetId();
      store.setQos(packetId, 2);
      store.setStage(packetId, 1);
      Fixture fixture = fixture(mqtt5, store);
      var committed = new java.util.concurrent.CompletableFuture<Void>();
      when(fixture.session.ackRecoveredDelivery(new PacketIdStore.Delivery("sensor", 151))).thenReturn(committed);
      if (mqtt5) new io.mapsmessaging.network.protocol.impl.mqtt5.listeners.ConnAckListener5().handlePacket(
          new io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5(), fixture.session, fixture.endpoint, fixture.protocol);
      else new io.mapsmessaging.network.protocol.impl.mqtt.listeners.ConnAckListener().handlePacket(
          new io.mapsmessaging.network.protocol.impl.mqtt.packet.ConnAck(), fixture.session, fixture.endpoint, fixture.protocol);
      assertTrue(fixture.frames.isEmpty());
      verify(fixture.session, never()).resumeState();
      committed.complete(null);
      assertEquals(1, fixture.frames.size());
      assertTrue(fixture.frames.getFirst() instanceof PubRel || fixture.frames.getFirst() instanceof PubRel5);
      verify(fixture.session).resumeState();
      assertEquals(1, store.getStage(packetId));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void failedRecoveredEngineAckClosesConnectionWithoutPubrel(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int packetId = store.allocate("sensor", 161).packetId();
      store.setQos(packetId, 2);
      store.setStage(packetId, 1);
      Fixture fixture = fixture(mqtt5, store);
      var committed = new java.util.concurrent.CompletableFuture<Void>();
      when(fixture.session.ackRecoveredDelivery(new PacketIdStore.Delivery("sensor", 161))).thenReturn(committed);
      var recovered = mqtt5 ? ((MQTT5Protocol) fixture.protocol).recoverOutgoingDeliveries() : ((MQTTProtocol) fixture.protocol).recoverOutgoingDeliveries();
      committed.completeExceptionally(new IOException("engine commit failed"));
      assertTrue(recovered.isCompletedExceptionally());
      assertTrue(fixture.frames.isEmpty());
      verify(fixture.endpoint).close();
      verify(fixture.session, never()).resumeState();
      assertEquals(1, store.getStage(packetId));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void retiredSessionRecoveryCallbackCannotWriteOrCloseItsReplacement(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int packetId = store.allocate("sensor", 171).packetId();
      store.setQos(packetId, 2);
      store.setStage(packetId, 1);
      Fixture fixture = fixture(mqtt5, store);
      var committed = new java.util.concurrent.CompletableFuture<Void>();
      when(fixture.session.ackRecoveredDelivery(new PacketIdStore.Delivery("sensor", 171))).thenReturn(committed);
      if (mqtt5) new io.mapsmessaging.network.protocol.impl.mqtt5.listeners.ConnAckListener5().handlePacket(
          new io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5(), fixture.session, fixture.endpoint, fixture.protocol);
      else new io.mapsmessaging.network.protocol.impl.mqtt.listeners.ConnAckListener().handlePacket(
          new io.mapsmessaging.network.protocol.impl.mqtt.packet.ConnAck(), fixture.session, fixture.endpoint, fixture.protocol);
      Session replacement = mock(Session.class);
      when(replacement.getOrCreatePacketIdStore(65535)).thenReturn(store);
      fixture.protocol.setSession(replacement);
      when(fixture.session.isClosed()).thenReturn(true);
      committed.complete(null);
      assertTrue(fixture.frames.isEmpty());
      verify(fixture.endpoint, never()).close();
      verify(fixture.session, never()).resumeState();
      verify(replacement, never()).resumeState();
    }
  }

  @Test
  void mqtt5FailedStoreBindingCannotOverwriteRejectedConnackWithSuccess() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture fixture = fixture(true, store);
      Session rejectedSession = mock(Session.class);
      when(rejectedSession.getOrCreatePacketIdStore(65535)).thenThrow(new IOException("cannot open journal"));
      io.mapsmessaging.api.SessionManager manager = mock(io.mapsmessaging.api.SessionManager.class);
      when(manager.create(any(), same(fixture.protocol))).thenReturn(rejectedSession);
      try (var staticManager = mockStatic(io.mapsmessaging.api.SessionManager.class)) {
        staticManager.when(io.mapsmessaging.api.SessionManager::getInstance).thenReturn(manager);
        var listener = new io.mapsmessaging.network.protocol.impl.mqtt5.listeners.ConnectListener5();
        var acknowledgement = new io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5();
        var builder = new io.mapsmessaging.api.SessionContextBuilder("client", new ProtocolClientConnection(fixture.protocol));
        var method = listener.getClass().getDeclaredMethod("handleSessionCreation", Protocol.class, String.class,
            io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5.class, io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5.class,
            io.mapsmessaging.api.SessionContextBuilder.class, EndPoint.class);
        method.setAccessible(true);
        var connect = new io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5();
        connect.setWillProperties(new io.mapsmessaging.network.protocol.impl.mqtt5.packet.MessageProperties());
        method.invoke(listener, fixture.protocol, "client", connect, acknowledgement, builder, fixture.endpoint);
        assertNotEquals(StatusCode.SUCCESS, acknowledgement.getStatusCode());
        verify(rejectedSession, never()).login();
        verify(fixture.endpoint).close();
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void normalPubrecWaitsForBrokerCommitAndIgnoresPrematurePubcomp(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.open(tempDir.resolve("normal-qos2.packet-ids"), 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture fixture = fixture(mqtt5, store);
      fixture.protocol.sendMessage(event(subscription(QualityOfService.EXACTLY_ONCE), 201));
      int packetId = identifier(fixture.frames.getFirst());
      var committed = new java.util.concurrent.CompletableFuture<Void>();
      PacketIdStore.Delivery identity = new PacketIdStore.Delivery("sensor", 201);
      when(fixture.session.ackRecoveredDelivery(identity)).thenReturn(committed);
      receivePubrec(fixture, mqtt5, packetId);
      receivePubrec(fixture, mqtt5, packetId);
      assertEquals(1, store.getStage(packetId));
      assertEquals(1, fixture.frames.size());
      verify(fixture.session, times(1)).ackRecoveredDelivery(identity);
      receivePubcomp(fixture, mqtt5, packetId);
      assertEquals(1, store.size());
      committed.complete(null);
      assertEquals(2, fixture.frames.size());
      assertTrue(fixture.frames.getLast() instanceof PubRel || fixture.frames.getLast() instanceof PubRel5);
      receivePubcomp(fixture, mqtt5, packetId);
      assertEquals(0, store.size());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void normalPubrecCommitFailureRetainsDurableStageWithoutSendingPubrel(boolean mqtt5) throws Exception {
    Path path = tempDir.resolve("failed-normal-qos2.packet-ids");
    int packetId;
    try (PacketIdStore store = PacketIdStore.open(path, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture fixture = fixture(mqtt5, store);
      fixture.protocol.sendMessage(event(subscription(QualityOfService.EXACTLY_ONCE), 211));
      packetId = identifier(fixture.frames.getFirst());
      var committed = new java.util.concurrent.CompletableFuture<Void>();
      when(fixture.session.ackRecoveredDelivery(new PacketIdStore.Delivery("sensor", 211))).thenReturn(committed);
      receivePubrec(fixture, mqtt5, packetId);
      committed.completeExceptionally(new IOException("broker commit failed"));
      assertEquals(1, fixture.frames.size());
      receivePubcomp(fixture, mqtt5, packetId);
      assertEquals(1, store.getStage(packetId));
      verify(fixture.endpoint).close();
    }
    try (PacketIdStore restored = PacketIdStore.open(path, 65535, PacketIdStore.Durability.BATCHED)) {
      assertEquals(1, restored.getStage(packetId));
      assertEquals(packetId, restored.getPacketId("sensor", 211));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void retiredPendingGateCannotSendOrBlockReusedIdentifier(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(1)) {
      Fixture fixture = fixture(mqtt5, store);
      fixture.protocol.sendMessage(event(subscription(QualityOfService.EXACTLY_ONCE), 221));
      int packetId = identifier(fixture.frames.getFirst());
      var oldCommit = new java.util.concurrent.CompletableFuture<Void>();
      when(fixture.session.ackRecoveredDelivery(new PacketIdStore.Delivery("sensor", 221))).thenReturn(oldCommit);
      receivePubrec(fixture, mqtt5, packetId);
      store.clear(packetId);
      fixture.manager.retireDelivery("sensor", 221);
      fixture.protocol.sendMessage(event(subscription(QualityOfService.EXACTLY_ONCE), 222));
      assertEquals(packetId, identifier(fixture.frames.getLast()));
      var newCommit = new java.util.concurrent.CompletableFuture<Void>();
      when(fixture.session.ackRecoveredDelivery(new PacketIdStore.Delivery("sensor", 222))).thenReturn(newCommit);
      receivePubrec(fixture, mqtt5, packetId);
      oldCommit.complete(null);
      assertEquals(2, fixture.frames.size());
      verify(fixture.endpoint, never()).close();
      assertEquals(packetId, store.getPacketId("sensor", 222));
      newCommit.complete(null);
      assertEquals(3, fixture.frames.size());
      receivePubcomp(fixture, mqtt5, packetId);
      assertEquals(0, store.size());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void pubcompArrivingDuringPubrelEnqueueCanCompleteReadyDelivery(boolean mqtt5) throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture fixture = fixture(mqtt5, store);
      fixture.protocol.sendMessage(event(subscription(QualityOfService.EXACTLY_ONCE), 231));
      int packetId = identifier(fixture.frames.getFirst());
      if (mqtt5) doAnswer(invocation -> {
        fixture.frames.add(invocation.getArgument(0));
        receivePubcomp(fixture, true, packetId);
        return null;
      }).when((MQTT5Protocol) fixture.protocol).writeFrame(any());
      else doAnswer(invocation -> {
        fixture.frames.add(invocation.getArgument(0));
        receivePubcomp(fixture, false, packetId);
        return null;
      }).when((MQTTProtocol) fixture.protocol).writeFrame(any());
      receivePubrec(fixture, mqtt5, packetId);
      assertEquals(0, store.size());
      assertEquals(2, fixture.frames.size());
    }
  }

  private void receivePubrec(Fixture fixture, boolean mqtt5, int packetId) throws Exception {
    assertNull(mqtt5 ? new PubRecListener5().handlePacket(new PubRec5(packetId), fixture.session, fixture.endpoint, fixture.protocol)
        : new PubRecListener().handlePacket(new PubRec(packetId), fixture.session, fixture.endpoint, fixture.protocol));
  }

  private void receivePubcomp(Fixture fixture, boolean mqtt5, int packetId) throws Exception {
    if (mqtt5) new PubCompListener5().handlePacket(new PubComp5(packetId), fixture.session, fixture.endpoint, fixture.protocol);
    else new PubCompListener().handlePacket(new PubComp(packetId), fixture.session, fixture.endpoint, fixture.protocol);
  }

  private Fixture fixture(boolean mqtt5, PacketIdStore store) throws IOException {
    EndPoint endpoint = mock(EndPoint.class);
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    config.setName("session-delivery");
    config.setEndPointConfig(new EndPointConfigDTO("tcp"));
    config.setProtocolConfigs(List.of(new MqttConfig()));
    int id = IDS.incrementAndGet();
    when(endpoint.getConfig()).thenReturn(config);
    when(endpoint.getName()).thenReturn("session-delivery-" + id);
    when(endpoint.getJMXTypePath()).thenReturn(List.of("Delivery=" + id));
    when(endpoint.getEndPointStatus()).thenReturn(mock(EndPointStatus.class));
    Session session = mock(Session.class);
    when(session.getName()).thenReturn("client");
    when(session.getReceiveMaximum()).thenReturn(1);
    when(session.getOrCreatePacketIdStore(65535)).thenReturn(store);
    when(session.getPacketIdStore()).thenReturn(store);
    when(session.ackRecoveredDelivery(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    List<ServerPacket> frames = new ArrayList<>();
    if (mqtt5) {
      MQTT5Protocol protocol = spy(new MQTT5Protocol(endpoint));
      doAnswer(invocation -> { frames.add(invocation.getArgument(0)); return null; }).when(protocol).writeFrame(any());
      protocol.setSession(session);
      return new Fixture(protocol, protocol.getPacketIdManager(), session, endpoint, frames);
    }
    MQTTProtocol protocol = spy(new MQTTProtocol(endpoint));
    doAnswer(invocation -> { frames.add(invocation.getArgument(0)); return null; }).when(protocol).writeFrame(any());
    protocol.setSession(session);
    return new Fixture(protocol, protocol.getPacketIdManager(), session, endpoint, frames);
  }

  private SubscribedEventManager subscription(QualityOfService qos) {
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("sensor");
    context.setQualityOfService(qos);
    when(subscription.getContext()).thenReturn(context);
    when(subscription.getContexts()).thenReturn(List.of(context));
    when(subscription.getDeliveryKey()).thenReturn("sensor");
    return subscription;
  }

  private MessageEvent event(SubscribedEventManager subscription, long id) {
    Message message = mock(Message.class);
    QualityOfService qos = subscription.getContext().getQualityOfService();
    when(message.getIdentifier()).thenReturn(id);
    when(message.getQualityOfService()).thenReturn(qos);
    when(message.getOpaqueData()).thenReturn(new byte[]{1, 2});
    when(message.getDataMap()).thenReturn(Map.of());
    return new MessageEvent("$schema/sensor", subscription, message, () -> {});
  }

  private int identifier(ServerPacket packet) {
    return packet instanceof Publish publish ? publish.getPacketId() : ((Publish5) packet).getPacketId();
  }

  private boolean duplicate(ServerPacket packet) {
    return packet instanceof Publish publish ? publish.isDuplicate() : ((Publish5) packet).isDuplicate();
  }

  private record Fixture(Protocol protocol, PacketIdManager manager, Session session, EndPoint endpoint, List<ServerPacket> frames) {}
}
