/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PacketListenerFactory;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Disconnect;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PingReq;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PingResp;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.PubAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Subscribe;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Unsubscribe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.security.auth.Subject;
import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MQTT311ProtocolCoverageTest {

  private static final AtomicInteger FIXTURE_IDS = new AtomicInteger();

  @Test
  void constructorExposesConfiguredIdentityAndLimits() throws Exception {
    Fixture fixture = fixture();

    assertEquals("MQTT", fixture.protocol.getName());
    assertEquals("3.1.1", fixture.protocol.getVersion());
    assertEquals("waiting", fixture.protocol.getSessionId());
    assertEquals(8192L, fixture.protocol.getMaximumBufferSize());
    assertEquals(60_000L, fixture.protocol.getKeepAlive());
  }

  @Test
  void sessionIdTracksAssignedSession() throws Exception {
    Fixture fixture = fixture();
    Session session = mock(Session.class);
    when(session.getName()).thenReturn("mqtt311-client");

    fixture.protocol.setSession(session);

    assertEquals("mqtt311-client", fixture.protocol.getSessionId());
    verify(fixture.endPoint).completedConnection();
  }

  @Test
  void subjectIsEmptyBeforeSessionExists() throws Exception {
    Fixture fixture = fixture();

    Subject subject = fixture.protocol.getSubject();

    assertNotNull(subject);
    assertTrue(subject.getPrincipals().isEmpty());
  }

  @Test
  void subjectComesFromAssignedSession() throws Exception {
    Fixture fixture = fixture();
    Session session = mock(Session.class);
    SecurityContext securityContext = mock(SecurityContext.class);
    Subject subject = new Subject();
    when(session.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getSubject()).thenReturn(subject);
    setField(fixture.protocol, "session", session);

    assertSame(subject, fixture.protocol.getSubject());
  }

  @Test
  void setConnectedCancelsOutstandingConnectionTimeout() throws Exception {
    Fixture fixture = fixture();
    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    setField(fixture.protocol, "connectionTimeOut", future);

    fixture.protocol.setConnected(false);

    verify(future).cancel(true);
    assertNull(field(fixture.protocol, "connectionTimeOut"));
  }

  @Test
  void setConnectedWithoutTimeoutIsSafe() throws Exception {
    Fixture fixture = fixture();

    assertDoesNotThrow(() -> fixture.protocol.setConnected(false));
    assertNull(field(fixture.protocol, "connectionTimeOut"));
  }

  @ParameterizedTest
  @MethodSource("inactiveKeepAliveCases")
  void inactiveKeepAliveReturnsBeforeManager(
      boolean protocolClosed,
      boolean sessionPresent,
      boolean sessionClosed) throws Exception {
    Fixture fixture = fixture();
    if (protocolClosed) {
      setField(fixture.protocol, "closed", true);
    }
    if (sessionPresent) {
      Session session = mock(Session.class);
      when(session.isClosed()).thenReturn(sessionClosed);
      setField(fixture.protocol, "session", session);
    }

    fixture.protocol.sendKeepAlive();

    verifyNoInteractions(fixture.keepAliveManager);
    assertTrue(fixture.frames.isEmpty());
  }

  @ParameterizedTest
  @MethodSource("keepAliveActionCases")
  void keepAliveActionControlsPingEmission(
      boolean client,
      MqttKeepAliveManager.Action action,
      boolean expectedPing) throws Exception {
    Fixture fixture = fixture();
    Session session = mock(Session.class);
    when(session.isClosed()).thenReturn(false);
    when(session.getName()).thenReturn("keepalive-session");
    setField(fixture.protocol, "session", session);
    fixture.protocol.setKeepAlive(10_000L);
    when(fixture.endPoint.isClient()).thenReturn(client);
    when(fixture.endPoint.getLastWrite()).thenReturn(1_000L);
    when(fixture.endPoint.getLastRead()).thenReturn(2_000L);
    if (client) {
      when(fixture.keepAliveManager.checkClient(anyLong(), eq(1_000L), eq(10_000L)))
          .thenReturn(action);
    } else {
      when(fixture.keepAliveManager.checkServer(anyLong(), eq(2_000L), eq(10_000L)))
          .thenReturn(action);
    }

    fixture.protocol.sendKeepAlive();

    assertEquals(expectedPing, fixture.frames.size() == 1);
    if (expectedPing) {
      assertInstanceOf(PingReq.class, fixture.frames.get(0));
    }
  }

  @ParameterizedTest
  @MethodSource("keepAliveIntervalCases")
  void keepAliveTaskIntervalUsesManagerCalculation(
      boolean client,
      long keepAlive,
      long expected) throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, "keepAliveManager", new MqttKeepAliveManager());
    fixture.protocol.setKeepAlive(keepAlive);
    when(fixture.endPoint.isClient()).thenReturn(client);

    assertEquals(expected, fixture.protocol.getKeepAliveTaskInterval());
  }

  @Test
  void pingResponseDelegatesToKeepAliveManager() throws Exception {
    Fixture fixture = fixture();

    fixture.protocol.pingResponseReceived();

    verify(fixture.keepAliveManager).pingResponseReceived();
  }

  @Test
  void emptyPacketDoesNotAttemptReadRegistration() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(0);
    when(packet.hasRemaining()).thenReturn(false);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(fixture.selectorTask, never()).register(anyInt());
    verify(fixture.endPoint, never()).close();
  }

  @Test
  void successfulPacketLoopReregistersWhenListenerRequestsResume() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(4);
    when(packet.hasRemaining()).thenReturn(true, false);
    doReturn(true).when(fixture.protocol).handleMQTTEvent(packet);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(fixture.selectorTask).register(SelectionKey.OP_READ);
  }

  @Test
  void successfulPacketLoopDoesNotReregisterWhenListenerPausesRead() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(4);
    when(packet.hasRemaining()).thenReturn(true, false);
    doReturn(false).when(fixture.protocol).handleMQTTEvent(packet);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(fixture.selectorTask, never()).register(anyInt());
  }

  @Test
  void partialPacketRewindsAndReregistersRead() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(9);
    when(packet.hasRemaining()).thenReturn(true);
    doThrow(new EndOfBufferException("partial"))
        .when(fixture.protocol).handleMQTTEvent(packet);

    assertFalse(fixture.protocol.processPacket(packet));

    verify(packet).position(9);
    verify(fixture.selectorTask).register(SelectionKey.OP_READ);
    verify(fixture.endPoint, never()).close();
  }

  @Test
  void malformedPacketClosesEndpoint() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(0);
    when(packet.hasRemaining()).thenReturn(true);
    doThrow(new MalformedException("bad"))
        .when(fixture.protocol).handleMQTTEvent(packet);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(fixture.endPoint).close();
  }

  @ParameterizedTest
  @MethodSource("listenerDispatchCases")
  void decodedPacketUsesControlPacketListenerAndReturnsResumeState(
      MQTTPacket decoded,
      boolean resume,
      boolean responsePresent) throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    PacketFactory packetFactory = mock(PacketFactory.class);
    PacketListenerFactory listenerFactory = mock(PacketListenerFactory.class);
    PacketListener listener = mock(PacketListener.class);
    Session session = mock(Session.class);
    setField(fixture.protocol, "session", session);
    setField(fixture.protocol, "packetFactory", packetFactory);
    setField(fixture.protocol, "packetListenerFactory", listenerFactory);
    when(packetFactory.parseFrame(packet)).thenReturn(decoded);
    when(listenerFactory.getListener(decoded.getControlPacketId())).thenReturn(listener);
    when(listener.resumeRead()).thenReturn(resume);
    MQTTPacket response = responsePresent ? new PingResp() : null;
    when(listener.handlePacket(decoded, session, fixture.endPoint, fixture.protocol))
        .thenReturn(response);

    assertEquals(resume, fixture.protocol.handleMQTTEvent(packet));

    verify(listenerFactory).getListener(decoded.getControlPacketId());
    verify(listener).handlePacket(decoded, session, fixture.endPoint, fixture.protocol);
    if (responsePresent) {
      verify(fixture.selectorTask).push(response);
    } else {
      verify(fixture.selectorTask, never()).push(any());
    }
  }

  @Test
  void nullDecodedPacketSkipsListenerLookup() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    PacketFactory packetFactory = mock(PacketFactory.class);
    PacketListenerFactory listenerFactory = mock(PacketListenerFactory.class);
    setField(fixture.protocol, "packetFactory", packetFactory);
    setField(fixture.protocol, "packetListenerFactory", listenerFactory);
    when(packetFactory.parseFrame(packet)).thenReturn(null);

    assertTrue(fixture.protocol.handleMQTTEvent(packet));

    verifyNoInteractions(listenerFactory);
    verify(fixture.selectorTask, never()).push(any());
  }

  @ParameterizedTest
  @MethodSource("credentialCases")
  void connectBuildsExpectedCredentials(
      String username,
      String password,
      String expectedUsername,
      String expectedPassword) throws Exception {
    Fixture fixture = fixture();

    try {
      fixture.protocol.connect("client-1", username, password);

      io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect connect =
          assertInstanceOf(
              io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect.class,
              fixture.onlyFrame());
      assertEquals("client-1", connect.getSessionId());
      assertEquals(expectedUsername, connect.getUsername());
      if (expectedPassword == null) {
        assertNull(connect.getPassword());
      } else {
        assertArrayEquals(expectedPassword.toCharArray(), connect.getPassword());
      }
      verify(fixture.selectorTask).register(SelectionKey.OP_READ);
      verify(fixture.endPoint).completedConnection();
    } finally {
      fixture.protocol.setConnected(false);
    }
  }

  @ParameterizedTest
  @MethodSource("willCases")
  void connectCopiesWillFields(QualityOfService qos, boolean retain) throws Exception {
    Fixture fixture = fixture();
    byte[] payload = ("will-" + qos + "-" + retain).getBytes(StandardCharsets.UTF_8);
    Publish will = new Publish(retain, payload, qos, 7, "/will/topic");

    try {
      fixture.protocol.connect("client-will", null, null, will);

      io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect connect =
          assertInstanceOf(
              io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect.class,
              fixture.onlyFrame());
      assertTrue(connect.isWillFlag());
      assertEquals("/will/topic", connect.getWillTopic());
      assertArrayEquals(payload, connect.getWillMsg());
      assertEquals(qos, connect.getWillQOS());
      assertEquals(retain, connect.isWillRetain());
    } finally {
      fixture.protocol.setConnected(false);
    }
  }

  @ParameterizedTest
  @MethodSource("qosCases")
  void subscribeRemoteWritesSubscribeAndRecordsMapping(QualityOfService qos) throws Exception {
    Fixture fixture = fixture();

    fixture.protocol.subscribeRemote(
        "/remote/source",
        "/local/mapped",
        qos,
        null,
        null,
        null,
        Map.of());

    assertEquals("/local/mapped", fixture.protocol.getTopicNameMapping().get("/remote/source"));
    Subscribe subscribe = assertInstanceOf(Subscribe.class, fixture.onlyFrame());
    assertTrue(subscribe.getMessageId() > 0);
    assertEquals(1, subscribe.getSubscriptionList().size());
    assertEquals("/remote/source", subscribe.getSubscriptionList().get(0).getTopicName());
    assertEquals(qos, subscribe.getSubscriptionList().get(0).getQualityOfService());
    verify(fixture.endPoint).completedConnection();
  }

  @ParameterizedTest
  @MethodSource("topicNames")
  void unsubscribeRemoteWritesRequestedTopic(String topic) throws Exception {
    Fixture fixture = fixture();

    fixture.protocol.unsubscribeRemote(topic);

    Unsubscribe unsubscribe = assertInstanceOf(Unsubscribe.class, fixture.onlyFrame());
    assertTrue(unsubscribe.getPacketId() > 0);
    assertEquals(List.of(topic), unsubscribe.getUnsubscribeList());
  }

  @ParameterizedTest
  @MethodSource("localSubscriptionCases")
  void subscribeLocalBuildsExpectedContext(
      QualityOfService qos,
      String selector,
      boolean expectedSelector) throws Exception {
    Fixture fixture = fixture();
    Session session = mock(Session.class);
    fixture.protocol.setSession(session);

    fixture.protocol.subscribeLocal(
        "/local/source",
        "/remote/mapped",
        qos,
        selector,
        null,
        null,
        null,
        Map.of());

    var captor = org.mockito.ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(session).addSubscription(captor.capture());
    SubscriptionContext context = captor.getValue();
    assertEquals("/local/source", context.getDestinationName());
    assertEquals(qos, context.getQualityOfService());
    assertEquals(1024, context.getReceiveMaximum());
    assertTrue(context.allowOverlap());
    assertEquals(expectedSelector ? selector : null, context.getSelector());
    assertEquals("/remote/mapped", fixture.protocol.getTopicNameMapping().get("/local/source"));
  }

  @ParameterizedTest
  @MethodSource("topicNames")
  void unsubscribeLocalDelegatesToSession(String topic) throws Exception {
    Fixture fixture = fixture();
    Session session = mock(Session.class);
    fixture.protocol.setSession(session);

    fixture.protocol.unsubscribeLocal(topic);

    verify(session).removeSubscription(topic);
  }

  @Test
  void writeFrameQueuesPacketAndUpdatesSentCount() throws Exception {
    Fixture fixture = fixture();
    doCallRealMethod().when(fixture.protocol).writeFrame(any(ServerPacket.class));
    PingReq frame = new PingReq();

    fixture.protocol.writeFrame(frame);

    verify(fixture.endPointStatus).incrementSentMessages();
    verify(fixture.selectorTask).push(frame);
  }

  private static Fixture fixture() throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    EndPointStatus endPointStatus = mock(EndPointStatus.class);
    MqttConfig mqttConfig = new MqttConfig();
    mqttConfig.setMaxServerKeepAlive(60);
    mqttConfig.setMaximumBufferSize(8192);

    EndPointConfigDTO endPointConfig = new EndPointConfigDTO("tcp");
    EndPointServerConfigDTO serverConfig = new EndPointServerConfigDTO();
    serverConfig.setName("mqtt311-coverage");
    serverConfig.setEndPointConfig(endPointConfig);
    serverConfig.setProtocolConfigs(List.of(mqttConfig));

    int id = FIXTURE_IDS.incrementAndGet();
    when(endPoint.getConfig()).thenReturn(serverConfig);
    when(endPoint.getName()).thenReturn("mqtt311-coverage-" + id);
    when(endPoint.getJMXTypePath()).thenReturn(List.of("Coverage=" + id));
    when(endPoint.getEndPointStatus()).thenReturn(endPointStatus);

    MQTTProtocol protocol = spy(new MQTTProtocol(endPoint));
    SelectorTask selectorTask = mock(SelectorTask.class);
    MqttKeepAliveManager keepAliveManager = mock(MqttKeepAliveManager.class);
    setField(protocol, "selectorTask", selectorTask);
    setField(protocol, "keepAliveManager", keepAliveManager);

    List<ServerPacket> frames = new ArrayList<>();
    doAnswer(invocation -> {
      frames.add(invocation.getArgument(0));
      return null;
    }).when(protocol).writeFrame(any(ServerPacket.class));

    return new Fixture(
        protocol, endPoint, endPointStatus, selectorTask, keepAliveManager, frames);
  }

  private static Object field(Object target, String name) throws Exception {
    Field field = MQTTProtocol.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = MQTTProtocol.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Stream<Arguments> inactiveKeepAliveCases() {
    return Stream.of(
        Arguments.of(true, true, false),
        Arguments.of(false, false, false),
        Arguments.of(false, true, true));
  }

  private static Stream<Arguments> keepAliveActionCases() {
    return Stream.of(
        Arguments.of(true, MqttKeepAliveManager.Action.NONE, false),
        Arguments.of(true, MqttKeepAliveManager.Action.SEND_PING, true),
        Arguments.of(false, MqttKeepAliveManager.Action.NONE, false),
        Arguments.of(false, MqttKeepAliveManager.Action.SEND_PING, true));
  }

  private static Stream<Arguments> keepAliveIntervalCases() {
    return Stream.of(
        Arguments.of(true, 0L, 1L),
        Arguments.of(false, 0L, 1L),
        Arguments.of(true, 5L, 1L),
        Arguments.of(false, 5L, 1L),
        Arguments.of(true, 100L, 20L),
        Arguments.of(false, 100L, 20L));
  }

  private static Stream<Arguments> listenerDispatchCases() {
    return Stream.of(
        Arguments.of(new PingReq(), true, false),
        Arguments.of(new PingReq(), false, true),
        Arguments.of(new Disconnect(), true, false),
        Arguments.of(new PubAck(7), false, false),
        Arguments.of(
            new Publish(false, new byte[]{1}, QualityOfService.AT_MOST_ONCE, 0, "/a"),
            true,
            true),
        Arguments.of(
            new Publish(true, new byte[]{1, 2}, QualityOfService.AT_LEAST_ONCE, 4, "/b"),
            false,
            true),
        Arguments.of(new Subscribe(), true, false),
        Arguments.of(new Unsubscribe(List.of("/x")), false, false));
  }

  private static Stream<Arguments> credentialCases() {
    return Stream.of(
        Arguments.of(null, null, null, null),
        Arguments.of("", " pass ", "", "pass"),
        Arguments.of("user", " pass ", "user", "pass"),
        Arguments.of(" user ", "x", " user ", "x"));
  }

  private static Stream<Arguments> willCases() {
    List<Arguments> result = new ArrayList<>();
    for (QualityOfService qos : List.of(
        QualityOfService.AT_MOST_ONCE,
        QualityOfService.AT_LEAST_ONCE,
        QualityOfService.EXACTLY_ONCE)) {
      for (boolean retain : List.of(false, true)) {
        result.add(Arguments.of(qos, retain));
      }
    }
    return result.stream();
  }

  private static Stream<Arguments> qosCases() {
    return Stream.of(
        Arguments.of(QualityOfService.AT_MOST_ONCE),
        Arguments.of(QualityOfService.AT_LEAST_ONCE),
        Arguments.of(QualityOfService.EXACTLY_ONCE));
  }

  private static Stream<Arguments> topicNames() {
    return Stream.of(
        Arguments.of("/a"),
        Arguments.of("/sensor/temp"),
        Arguments.of("root/topic"),
        Arguments.of("a/b/c"),
        Arguments.of("+"),
        Arguments.of("#"),
        Arguments.of("$schema/test"),
        Arguments.of("topic with spaces"));
  }

  private static Stream<Arguments> localSubscriptionCases() {
    return Stream.of(
        Arguments.of(QualityOfService.AT_MOST_ONCE, null, false),
        Arguments.of(QualityOfService.AT_LEAST_ONCE, "", false),
        Arguments.of(QualityOfService.EXACTLY_ONCE, "priority > 4", true),
        Arguments.of(QualityOfService.AT_MOST_ONCE, "x = 1", true),
        Arguments.of(QualityOfService.AT_LEAST_ONCE, null, false),
        Arguments.of(QualityOfService.EXACTLY_ONCE, "", false));
  }

  private static final class Fixture {
    private final MQTTProtocol protocol;
    private final EndPoint endPoint;
    private final EndPointStatus endPointStatus;
    private final SelectorTask selectorTask;
    private final MqttKeepAliveManager keepAliveManager;
    private final List<ServerPacket> frames;

    private Fixture(
        MQTTProtocol protocol,
        EndPoint endPoint,
        EndPointStatus endPointStatus,
        SelectorTask selectorTask,
        MqttKeepAliveManager keepAliveManager,
        List<ServerPacket> frames) {
      this.protocol = protocol;
      this.endPoint = endPoint;
      this.endPointStatus = endPointStatus;
      this.selectorTask = selectorTask;
      this.keepAliveManager = keepAliveManager;
      this.frames = frames;
    }

    private ServerPacket onlyFrame() {
      assertEquals(1, frames.size());
      return frames.get(0);
    }
  }
}
