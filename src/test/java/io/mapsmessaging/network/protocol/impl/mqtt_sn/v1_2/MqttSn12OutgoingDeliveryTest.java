/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.RegisteredTopicConfiguration;
import java.net.InetSocketAddress;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.pipeline.MessagePipeline;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PubAckListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PubCompListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PubRecListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.RegisterAckListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.TopicAliasManager;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqttSn12OutgoingDeliveryTest {
  @TempDir Path directory;

  @Test
  void firstDeliveryAndOfflineQueueDoNotSetDup() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture f = fixture(store, QualityOfService.AT_LEAST_ONCE);
      f.pipeline.pause();
      f.pipeline.queue(f.event);
      assertTrue(f.frames.isEmpty());
      assertEquals(0, store.size());
      f.pipeline.resume();
      Publish first = (Publish) f.frames.getFirst();
      assertFalse(first.dup());
      assertFalse(roundTrip(first).dup());
      assertNotEquals(0, first.getMessageId());
      f.pipeline.ackReceived();
      Publish retry = (Publish) f.frames.getLast();
      assertTrue(retry.dup());
      Publish wireRetry = roundTrip(retry);
      assertTrue(wireRetry.dup());
      assertEquals(first.getMessageId(), wireRetry.getMessageId());
      assertEquals(QualityOfService.AT_LEAST_ONCE, wireRetry.getQoS());
      assertEquals(first.getMessageId(), retry.getMessageId());
    }
  }

  @Test
  void reconnectAndRestartPreserveIdentifierAndDupUntilAck() throws Exception {
    Path file = directory.resolve("mqtt-sn.packet-ids");
    int id;
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture f = fixture(store, QualityOfService.AT_LEAST_ONCE);
      f.pipeline.queue(f.event);
      Publish first = (Publish) f.frames.getFirst();
      assertFalse(first.dup());
      id = first.getMessageId();
      f.manager.close();
      assertEquals(1, store.size());
      Fixture reconnected = fixture(store, QualityOfService.AT_LEAST_ONCE);
      reconnected.pipeline.queue(reconnected.event);
      assertEquals(id, ((Publish) reconnected.frames.getFirst()).getMessageId());
      assertTrue(((Publish) reconnected.frames.getFirst()).dup());
      reconnected.manager.close();
    }
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture restarted = fixture(store, QualityOfService.AT_LEAST_ONCE);
      restarted.pipeline.queue(restarted.event);
      Publish retry = (Publish) restarted.frames.getFirst();
      assertEquals(id, retry.getMessageId());
      assertTrue(retry.dup());
      new PubAckListener().handlePacket(new PubAck(4, id, ReasonCodes.SUCCESS), null, null, restarted.protocol, restarted.engine);
      verify(restarted.subscription).ackReceived(41L);
      assertEquals(0, store.size());
    }
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      assertTrue(store.outstanding().isEmpty());
    }
  }

  @Test
  void qosTwoKeepsInterestThroughPubCompAndRestartsWithPubRel() throws Exception {
    Path file = directory.resolve("qos-two.packet-ids");
    int id;
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture f = fixture(store, QualityOfService.EXACTLY_ONCE);
      f.pipeline.queue(f.event);
      id = ((Publish) f.frames.getFirst()).getMessageId();
      assertNull(new PubRecListener().handlePacket(new PubRec(id), null, null, f.protocol, f.engine));
      verify(f.subscription, never()).ackReceived(anyLong());
      assertEquals(1, store.size());
      // Repeated PUBREC is safe and must retain the identifier.
      assertNull(new PubRecListener().handlePacket(new PubRec(id), null, null, f.protocol, f.engine));
      f.manager.close();
    }
    try (PacketIdStore store = PacketIdStore.open(file, 65535, PacketIdStore.Durability.BATCHED)) {
      Fixture f = fixture(store, QualityOfService.AT_MOST_ONCE);
      f.pipeline.queue(f.event);
      PubRel resumed = assertInstanceOf(PubRel.class, f.frames.getFirst());
      assertEquals(id, resumed.getMessageId());
      new PubCompListener().handlePacket(new PubComp(id), null, null, f.protocol, f.engine);
      verify(f.subscription).ackReceived(41L);
      assertEquals(0, store.size());
    }
  }

  @Test
  void rejectedAckAndPrematurePubCompDoNotDeleteDelivery() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture f = fixture(store, QualityOfService.EXACTLY_ONCE);
      f.pipeline.queue(f.event);
      int id = ((Publish) f.frames.getFirst()).getMessageId();
      assertThrows(MalformedException.class, () -> new PubAckListener().handlePacket(
          new PubAck(4, id, ReasonCodes.CONGESTION), null, null, f.protocol, f.engine));
      assertThrows(MalformedException.class, () -> new PubAckListener().handlePacket(
          new PubAck(4, id, ReasonCodes.SUCCESS), null, null, f.protocol, f.engine));
      assertThrows(MalformedException.class, () -> new PubCompListener().handlePacket(
          new PubComp(id), null, null, f.protocol, f.engine));
      assertEquals(1, store.size());
      verify(f.subscription, never()).ackReceived(anyLong());
      verify(f.engine, never()).sendNextPublish();
    }
  }

  @Test
  void registerReservesIdentifierOutsideDurablePublishIds() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int publishId = store.allocate("other", 19).packetId();
      Fixture f = fixture(store, QualityOfService.AT_LEAST_ONCE);
      TopicAliasManager aliases = f.engine.getTopicAliasManager();
      when(aliases.findTopicAlias("topic")).thenReturn((short) -1);
      when(aliases.findRegisteredTopicAlias(any(), eq("topic"))).thenReturn(-1);
      when(aliases.getTopicAlias("topic")).thenReturn((short) 4);
      List<MQTT_SNPacket> control = new ArrayList<>();
      doAnswer(invocation -> { control.add(invocation.getArgument(0)); return null; }).when(f.protocol).writeFrame(any());
      f.pipeline.queue(f.event);
      Register register = assertInstanceOf(Register.class, control.getFirst());
      assertNotEquals(0, register.getMessageId());
      assertNotEquals(publishId, register.getMessageId());
      assertEquals(1, store.size());
      new RegisterAckListener().handlePacket(new RegisterAck(4, register.getMessageId(), ReasonCodes.SUCCESS), null, null, f.protocol, f.engine);
      assertNull(f.manager.receivedPacket(register.getMessageId()));
      assertNotNull(store.getDelivery(publishId));
      verify(f.engine).ackReceived();
    }
  }

  @Test
  void expiredRetryRemovesPersistedIdentifier() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture f = fixture(store, QualityOfService.AT_LEAST_ONCE);
      f.pipeline.queue(f.event);
      assertEquals(1, store.size());
      when(f.event.getMessage().getCreation()).thenReturn(0L);
      f.pipeline.ackReceived();
      assertEquals(0, store.size());
      assertEquals(1, f.frames.size());
    }
  }

  @Test
  void qosZeroNeverSetsDupOrAllocatesIdentifier() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture f = fixture(store, QualityOfService.AT_MOST_ONCE);
      f.pipeline.queue(f.event);
      Publish publish = (Publish) f.frames.getFirst();
      assertFalse(publish.dup());
      assertEquals(0, publish.getMessageId());
      assertEquals(0, store.size());
    }
  }

  @Test
  void cleanSessionStartsNewDeliveryWithoutDup() throws Exception {
    try (PacketIdStore prior = PacketIdStore.memory(65535); PacketIdStore clean = PacketIdStore.memory(65535)) {
      Fixture original = fixture(prior, QualityOfService.AT_LEAST_ONCE);
      original.pipeline.queue(original.event);
      Fixture reset = fixture(clean, QualityOfService.AT_LEAST_ONCE);
      reset.pipeline.queue(reset.event);
      assertFalse(((Publish) reset.frames.getFirst()).dup());
    }
  }

  @Test
  void restoredPubRelWithoutBrokerEventCompletesBeforeBrokerResume() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int first = store.allocate("mqtt-sn-topic", 41L).packetId();
      int second = store.allocate("expired-topic", 52L).packetId();
      store.setQos(first, 2);
      store.setQos(second, 2);
      store.setStage(first, 1);
      store.setStage(second, 1);
      Fixture events = fixture(store, QualityOfService.AT_MOST_ONCE);
      TestProtocol protocol = new TestProtocol();
      Session session = mock(Session.class);
      when(session.isClosed()).thenReturn(false);
      protocol.setSession(session);
      when(session.getPacketIdStore()).thenReturn(store);
      when(session.ackRecoveredDelivery(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
      protocol.getPacketIdManager().bindStore(store);
      try {
        protocol.resumeSession(session);
        assertEquals(1, protocol.frames.size());
        verify(session, never()).resumeState();
        int active = ((PubRel) protocol.frames.getFirst()).getMessageId();
        int inactive = active == first ? second : first;
        assertThrows(MalformedException.class, () -> new PubCompListener().handlePacket(new PubComp(inactive), session, null, protocol, protocol.stateEngine));
        assertEquals(2, store.size());
        assertEquals(1, protocol.frames.size());
        new PubCompListener().handlePacket(new PubComp(active), session, null, protocol, protocol.stateEngine);
        assertEquals(2, protocol.frames.size());
        verify(session, never()).resumeState();
        int remaining = ((PubRel) protocol.frames.getLast()).getMessageId();
        assertNotEquals(active, remaining);
        new PubCompListener().handlePacket(new PubComp(remaining), session, null, protocol, protocol.stateEngine);
        verify(session).resumeState();
        assertEquals(0, store.size());
        // Broker interest can arrive after PUBCOMP: acknowledge it without publishing again.
        protocol.sendMessage(events.event);
        verify(events.subscription).ackReceived(41L);
        assertEquals(2, protocol.frames.size());
      } finally {
        when(session.isClosed()).thenReturn(true);
        protocol.close();
      }
    }
  }

  @Test
  void recoveryWaitsForEngineAckAndKeepsStageOnFailure() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int identifier = store.allocate("mqtt-sn-topic", 41L).packetId();
      store.setQos(identifier, 2);
      store.setStage(identifier, 1);
      TestProtocol protocol = new TestProtocol();
      Session session = mock(Session.class);
      when(session.isClosed()).thenReturn(true);
      when(session.getPacketIdStore()).thenReturn(store);
      java.util.concurrent.CompletableFuture<Void> engineAck = new java.util.concurrent.CompletableFuture<>();
      when(session.ackRecoveredDelivery(new PacketIdStore.Delivery("mqtt-sn-topic", 41L))).thenReturn(engineAck);
      protocol.setSession(session);
      protocol.getPacketIdManager().bindStore(store);
      protocol.resumeSession(session);
      assertTrue(protocol.frames.isEmpty());
      assertEquals(1, store.getStage(identifier));
      verify(session, never()).resumeState();
      engineAck.completeExceptionally(new java.io.IOException("broker ACK failed"));
      assertTrue(protocol.frames.isEmpty());
      assertEquals(1, store.getStage(identifier));
      verify(protocol.getEndPoint()).close();
      verify(session, never()).resumeState();
    }
  }

  @Test
  void recoverySendsPubRelOnlyAfterEngineAckCompletes() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      int identifier = store.allocate("mqtt-sn-topic", 41L).packetId();
      store.setQos(identifier, 2);
      store.setStage(identifier, 1);
      TestProtocol protocol = new TestProtocol();
      Session session = mock(Session.class);
      when(session.isClosed()).thenReturn(false);
      when(session.getPacketIdStore()).thenReturn(store);
      java.util.concurrent.CompletableFuture<Void> engineAck = new java.util.concurrent.CompletableFuture<>();
      when(session.ackRecoveredDelivery(any())).thenReturn(engineAck);
      protocol.setSession(session);
      protocol.getPacketIdManager().bindStore(store);
      try {
        protocol.resumeSession(session);
        assertTrue(protocol.frames.isEmpty());
        verify(session).ackRecoveredDelivery(new PacketIdStore.Delivery("mqtt-sn-topic", 41L));
        engineAck.complete(null);
        assertEquals(identifier, assertInstanceOf(PubRel.class, protocol.frames.getFirst()).getMessageId());
        assertEquals(1, store.getStage(identifier));
        new PubCompListener().handlePacket(new PubComp(identifier), session, null, protocol, protocol.stateEngine);
        assertEquals(0, store.size());
        verify(session).resumeState();
      } finally {
        when(session.isClosed()).thenReturn(true);
        protocol.close();
      }
    }
  }

  @Test
  void pubRecWaitsForEngineAcknowledgementBeforePubRel() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture events = fixture(store, QualityOfService.EXACTLY_ONCE);
      TestProtocol protocol = new TestProtocol();
      Session session = mock(Session.class);
      when(session.isClosed()).thenReturn(false);
      when(session.getPacketIdStore()).thenReturn(store);
      java.util.concurrent.CompletableFuture<Void> engineAck = new java.util.concurrent.CompletableFuture<>();
      when(session.ackRecoveredDelivery(any())).thenReturn(engineAck);
      protocol.setSession(session);
      protocol.getPacketIdManager().bindStore(store);
      int identifier = protocol.getPacketIdManager().nextPacketIdentifier(events.subscription, 41L);
      protocol.getPacketIdManager().setPacketQos(identifier, 2);
      try {
        assertNull(new PubRecListener().handlePacket(new PubRec(identifier), session, null, protocol, protocol.stateEngine));
        assertTrue(protocol.frames.isEmpty());
        assertEquals(1, store.getStage(identifier));
        new PubRecListener().handlePacket(new PubRec(identifier), session, null, protocol, protocol.stateEngine);
        verify(session, times(1)).ackRecoveredDelivery(any());
        assertThrows(MalformedException.class, () -> new PubCompListener().handlePacket(new PubComp(identifier), session, null, protocol, protocol.stateEngine));
        engineAck.complete(null);
        assertEquals(identifier, assertInstanceOf(PubRel.class, protocol.frames.getFirst()).getMessageId());
        new PubRecListener().handlePacket(new PubRec(identifier), session, null, protocol, protocol.stateEngine);
        assertEquals(2, protocol.frames.size());
        verify(session, times(1)).ackRecoveredDelivery(any());
        new PubCompListener().handlePacket(new PubComp(identifier), session, null, protocol, protocol.stateEngine);
        verify(events.subscription, never()).ackReceived(anyLong());
        assertEquals(0, store.size());
      } finally {
        when(session.isClosed()).thenReturn(true);
        protocol.close();
      }
    }
  }

  @Test
  void failedPubRecEngineAckClosesTransportWithoutPubRel() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture events = fixture(store, QualityOfService.EXACTLY_ONCE);
      TestProtocol protocol = new TestProtocol();
      Session session = mock(Session.class);
      when(session.isClosed()).thenReturn(true);
      when(session.getPacketIdStore()).thenReturn(store);
      when(session.ackRecoveredDelivery(any())).thenReturn(java.util.concurrent.CompletableFuture.failedFuture(new java.io.IOException("ACK failed")));
      protocol.setSession(session);
      protocol.getPacketIdManager().bindStore(store);
      int identifier = protocol.getPacketIdManager().nextPacketIdentifier(events.subscription, 41L);
      protocol.getPacketIdManager().setPacketQos(identifier, 2);
      new PubRecListener().handlePacket(new PubRec(identifier), session, null, protocol, protocol.stateEngine);
      assertTrue(protocol.frames.isEmpty());
      assertEquals(1, store.getStage(identifier));
      verify(protocol.getEndPoint()).close();
    }
  }

  @Test
  void restoredPublishKeepsOriginalWireQosAfterSubscriptionDowngrade() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      Fixture first = fixture(store, QualityOfService.AT_LEAST_ONCE);
      first.pipeline.queue(first.event);
      int identifier = ((Publish) first.frames.getFirst()).getMessageId();
      first.manager.close();
      Fixture downgraded = fixture(store, QualityOfService.AT_MOST_ONCE);
      downgraded.pipeline.queue(downgraded.event);
      Publish retry = assertInstanceOf(Publish.class, downgraded.frames.getFirst());
      assertEquals(identifier, retry.getMessageId());
      assertEquals(QualityOfService.AT_LEAST_ONCE, retry.getQoS());
      assertTrue(retry.dup());
      new PubAckListener().handlePacket(new PubAck(4, identifier, ReasonCodes.SUCCESS), null, null, downgraded.protocol, downgraded.engine);
      verify(downgraded.subscription).ackReceived(41L);
      assertEquals(0, store.size());
    }
  }

  @Test
  void storageFailureClosesTransport() throws Exception {
    PacketIdStore store = PacketIdStore.memory(65535);
    Fixture events = fixture(store, QualityOfService.AT_LEAST_ONCE);
    TestProtocol protocol = new TestProtocol();
    Session session = mock(Session.class);
    when(session.isClosed()).thenReturn(true);
    protocol.setSession(session);
    protocol.getPacketIdManager().bindStore(store);
    store.close();
    protocol.sendMessage(events.event);
    verify(protocol.getEndPoint()).close();
    assertTrue(protocol.frames.isEmpty());
  }

  @Test
  void closeReleasesRegisterReservationsBeforeSessionOwnerClosesStore() throws Exception {
    try (PacketIdStore store = PacketIdStore.memory(65535)) {
      TestProtocol protocol = new TestProtocol();
      Session session = mock(Session.class);
      protocol.setSession(session);
      protocol.getPacketIdManager().bindStore(store);
      protocol.getPacketIdManager().nextControlPacketIdentifier();
      SessionManager manager = mock(SessionManager.class);
      try (var managerSingleton = mockStatic(SessionManager.class)) {
        managerSingleton.when(SessionManager::getInstance).thenReturn(manager);
        doAnswer(invocation -> {
          assertEquals(0, protocol.getPacketIdManager().size());
          store.close();
          return null;
        }).when(manager).close(session, false);
        protocol.close();
        verify(protocol.getEndPoint()).close();
      }
    }
  }

  @Test
  void failedSessionStoreCloseStillClosesTransport() throws Exception {
    TestProtocol protocol = new TestProtocol();
    Session session = mock(Session.class);
    protocol.setSession(session);
    SessionManager manager = mock(SessionManager.class);
    try (var managerSingleton = mockStatic(SessionManager.class)) {
      managerSingleton.when(SessionManager::getInstance).thenReturn(manager);
      doThrow(new java.io.IOException("journal flush failed")).when(manager).close(session, false);
      assertThrows(java.io.IOException.class, protocol::close);
      verify(protocol.getEndPoint()).close();
    }
  }

  @Test
  void closedProtocolDoesNotSendPubRelAfterPendingEngineAck() throws Exception {
    for (boolean restored : new boolean[]{false, true}) {
      try (PacketIdStore store = PacketIdStore.memory(65535)) {
        Fixture events = fixture(store, QualityOfService.EXACTLY_ONCE);
        TestProtocol protocol = new TestProtocol();
        Session session = mock(Session.class);
        when(session.getPacketIdStore()).thenReturn(store);
        java.util.concurrent.CompletableFuture<Void> engineAck = new java.util.concurrent.CompletableFuture<>();
        when(session.ackRecoveredDelivery(any())).thenReturn(engineAck);
        protocol.setSession(session);
        int identifier;
        if (restored) {
          identifier = store.allocate("mqtt-sn-topic", 41L).packetId();
          store.setQos(identifier, 2);
          store.setStage(identifier, 1);
          protocol.getPacketIdManager().bindStore(store);
          protocol.resumeSession(session);
        } else {
          protocol.getPacketIdManager().bindStore(store);
          identifier = protocol.getPacketIdManager().nextPacketIdentifier(events.subscription, 41L, 2);
          new PubRecListener().handlePacket(new PubRec(identifier), session, null, protocol, protocol.stateEngine);
        }
        assertTrue(protocol.frames.isEmpty());
        when(session.isClosed()).thenReturn(true);
        protocol.close();
        engineAck.complete(null);
        assertTrue(protocol.frames.isEmpty());
        assertEquals(1, store.getStage(identifier));
        verify(session, never()).resumeState();
      }
    }
  }

  private static class TestProtocol extends MQTT_SNProtocol {
    private final List<MQTT_SNPacket> frames = new ArrayList<>();

    TestProtocol() {
      super(mock(MQTTSNInterfaceManager.class), testEndPoint(), new InetSocketAddress("127.0.0.1", 1884), mock(SelectorTask.class),
          "MQTT-SN recovery test", new PacketFactory(), mock(RegisteredTopicConfiguration.class), mock(MqttSnConfig.class));
    }

    private static EndPoint testEndPoint() {
      EndPoint endPoint = mock(EndPoint.class);
      when(endPoint.getEndPointStatus()).thenReturn(mock(EndPointStatus.class));
      return endPoint;
    }

    @Override
    public void writeFrame(MQTT_SNPacket frame) {
      frames.add(frame);
    }

    @Override
    protected ParsedMessage parseOutboundMessage(MessageEvent event) {
      return new ParsedMessage(event.getDestinationName(), event.getMessage());
    }
  }

  private Publish roundTrip(Publish publish) throws Exception {
    Packet packet = new Packet(64, false);
    publish.packFrame(packet);
    packet.flip();
    return assertInstanceOf(Publish.class, new PacketFactory().parseFrame(packet));
  }

  private Fixture fixture(PacketIdStore store, QualityOfService qos) {
    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class);
    MqttSnConfig config = mock(MqttSnConfig.class);
    when(config.getMaxInFlightEvents()).thenReturn(1);
    when(config.getEventQueueTimeout()).thenReturn(1);
    when(protocol.getProtocolConfig()).thenReturn(config);
    PacketIdManager manager = new PacketIdManager();
    manager.bindStore(store);
    when(protocol.getPacketIdManager()).thenReturn(manager);
    StateEngine engine = mock(StateEngine.class);
    TopicAliasManager aliases = mock(TopicAliasManager.class);
    when(engine.getTopicAliasManager()).thenReturn(aliases);
    when(aliases.findTopicAlias("topic")).thenReturn((short) 4);
    when(protocol.buildPublish(anyShort(), anyInt(), any(), any(), anyShort())).thenAnswer(invocation -> {
      Publish publish = new Publish(invocation.getArgument(0), invocation.getArgument(1), new byte[]{1});
      publish.setQoS(invocation.getArgument(3));
      return publish;
    });
    List<MQTT_SNPacket> frames = new ArrayList<>();
    doAnswer(invocation -> { frames.add(invocation.getArgument(2)); return null; }).when(engine).sendPublish(any(), anyString(), any());
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    when(context.getKey()).thenReturn("topic");
    when(context.getDestinationName()).thenReturn("topic");
    when(context.getQualityOfService()).thenReturn(qos);
    when(subscription.getContext()).thenReturn(context);
    when(subscription.getDeliveryKey()).thenReturn("mqtt-sn-topic");
    when(subscription.getDepth()).thenReturn(1);
    Message message = mock(Message.class);
    when(message.getIdentifier()).thenReturn(41L);
    when(message.getCreation()).thenReturn(System.currentTimeMillis());
    MessageEvent event = new MessageEvent("topic", subscription, message, () -> {});
    return new Fixture(new MessagePipeline(protocol, engine), protocol, engine, manager, subscription, event, frames);
  }

  private record Fixture(MessagePipeline pipeline, MQTT_SNProtocol protocol, StateEngine engine,
      PacketIdManager manager, SubscribedEventManager subscription, MessageEvent event, List<MQTT_SNPacket> frames) {}
}
