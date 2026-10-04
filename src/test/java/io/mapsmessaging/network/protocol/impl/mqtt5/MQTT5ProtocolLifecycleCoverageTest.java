/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.MqttKeepAliveManager;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.PacketListener5;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.PacketListenerFactory5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationMethod;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ReasonString;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.UserProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
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

class MQTT5ProtocolLifecycleCoverageTest {

  private static final AtomicInteger FIXTURE_IDS = new AtomicInteger();

  @Test
  void processPacketDoesNothingWhileClosing() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    fixture.protocol.setClosing(true);

    assertTrue(fixture.protocol.processPacket(packet));

    verifyNoInteractions(packet);
    verify(fixture.selectorTask, never()).register(anyInt());
  }

  @Test
  void emptyPacketReregistersReadInterest() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(0);
    when(packet.hasRemaining()).thenReturn(false);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(fixture.selectorTask).register(SelectionKey.OP_READ);
    verify(fixture.endPoint, never()).close();
  }

  @Test
  void successfulPacketLoopReregistersReadInterest() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(4);
    when(packet.hasRemaining()).thenReturn(true, false);
    doNothing().when(fixture.protocol).handleMQTTEvent(packet);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(fixture.protocol).handleMQTTEvent(packet);
    verify(fixture.selectorTask).register(SelectionKey.OP_READ);
  }

  @Test
  void bufferUnderflowRewindsCompactsAndPreservesConnection() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(7);
    when(packet.hasRemaining()).thenReturn(true);
    doThrow(new BufferUnderflowException())
        .when(fixture.protocol).handleMQTTEvent(packet);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(packet).position(7);
    verify(packet).compact();
    verify(packet).flip();
    verify(fixture.selectorTask, never()).register(anyInt());
    verify(fixture.endPoint, never()).close();
  }

  @Test
  void endOfBufferRewindsAndReregistersBeforeReturningFalse() throws Exception {
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

  @Test
  void closedChannelDuringReadRegistrationClosesEndpoint() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(0);
    when(packet.hasRemaining()).thenReturn(false);
    doThrow(new ClosedChannelException())
        .when(fixture.selectorTask).register(SelectionKey.OP_READ);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(fixture.endPoint).close();
  }

  @Test
  void ioFailureDuringReadRegistrationClosesEndpoint() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    when(packet.position()).thenReturn(0);
    when(packet.hasRemaining()).thenReturn(false);
    doThrow(new IOException("expected"))
        .when(fixture.selectorTask).register(SelectionKey.OP_READ);

    assertTrue(fixture.protocol.processPacket(packet));

    verify(fixture.endPoint).close();
  }

  @Test
  void nullDecodedPacketStopsBeforeListenerLookup() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    PacketFactory5 packetFactory = mock(PacketFactory5.class);
    PacketListenerFactory5 listenerFactory = mock(PacketListenerFactory5.class);
    when(packetFactory.parseFrame(packet)).thenReturn(null);
    setField(fixture.protocol, "packetFactory", packetFactory);
    setField(fixture.protocol, "packetListenerFactory", listenerFactory);

    fixture.protocol.handleMQTTEvent(packet);

    verifyNoInteractions(listenerFactory);
    verify(fixture.selectorTask, never()).push(any());
  }

  @Test
  void ordinaryPacketUsesItsControlPacketListenerAndCurrentSession() throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    PacketFactory5 packetFactory = mock(PacketFactory5.class);
    PacketListenerFactory5 listenerFactory = mock(PacketListenerFactory5.class);
    PacketListener5 listener = mock(PacketListener5.class);
    Session session = mock(Session.class);
    PingReq5 ping = new PingReq5();
    setField(fixture.protocol, "session", session);
    setField(fixture.protocol, "packetFactory", packetFactory);
    setField(fixture.protocol, "packetListenerFactory", listenerFactory);
    when(packetFactory.parseFrame(packet)).thenReturn(ping);
    when(listenerFactory.getListener(anyInt())).thenReturn(listener);

    fixture.protocol.handleMQTTEvent(packet);

    verify(listenerFactory).getListener(ping.getControlPacketId());
    verify(listener).handlePacket(
        same(ping), same(session), same(fixture.endPoint), same(fixture.protocol));
  }

  @ParameterizedTest
  @MethodSource("connectRoutingCases")
  void connectAuthenticationPropertiesChooseExpectedListener(
      boolean authenticationProperty,
      boolean authenticationContextPresent,
      boolean expectedAuthListener) throws Exception {
    Fixture fixture = fixture();
    Packet packet = mock(Packet.class);
    PacketFactory5 packetFactory = mock(PacketFactory5.class);
    PacketListenerFactory5 listenerFactory = mock(PacketListenerFactory5.class);
    PacketListener5 listener = mock(PacketListener5.class);
    Session session = mock(Session.class);
    Connect5 connect = new Connect5();
    if (authenticationProperty) {
      connect.add(new AuthenticationMethod("PLAIN"));
    }
    if (authenticationContextPresent) {
      fixture.protocol.setAuthenticationContext(mock(AuthenticationContext.class));
    }
    setField(fixture.protocol, "session", session);
    setField(fixture.protocol, "packetFactory", packetFactory);
    setField(fixture.protocol, "packetListenerFactory", listenerFactory);
    when(packetFactory.parseFrame(packet)).thenReturn(connect);
    when(listenerFactory.getListener(anyInt())).thenReturn(listener);

    fixture.protocol.handleMQTTEvent(packet);

    int expectedId = expectedAuthListener ? MQTTPacket5.AUTH : connect.getControlPacketId();
    verify(listenerFactory).getListener(expectedId);
    if (expectedAuthListener) {
      verify(listener).handlePacket(
          same(connect), isNull(), same(fixture.endPoint), same(fixture.protocol));
    } else {
      verify(listener).handlePacket(
          same(connect), same(session), same(fixture.endPoint), same(fixture.protocol));
    }
  }

  @Test
  void nullResponseDoesNotQueueWrite() throws Exception {
    Fixture fixture = fixture();

    fixture.handleResponse(null);

    verify(fixture.selectorTask, never()).push(any());
  }

  @Test
  void ordinaryResponseIsQueuedUnmodified() throws Exception {
    Fixture fixture = fixture();
    PingResp5 response = new PingResp5();

    fixture.handleResponse(response);

    verify(fixture.selectorTask).push(response);
  }

  @ParameterizedTest
  @MethodSource("problemStatusCodes")
  void problemInformationAddsReasonStringToStatusResponses(StatusCode statusCode)
      throws Exception {
    Fixture fixture = fixture();
    fixture.protocol.setSendProblemInformation(true);
    PubAck5 response = new PubAck5(10, statusCode);

    fixture.handleResponse(response);

    ReasonString reason = (ReasonString) response.getProperties().get(
        MessagePropertyFactory.REASON_STRING);
    assertNotNull(reason);
    assertEquals(statusCode.getDescription(), reason.getReasonString());
    verify(fixture.selectorTask).push(response);
  }

  @ParameterizedTest
  @MethodSource("problemInformationDisabledCases")
  void disabledProblemInformationLeavesStatusResponsePropertiesUntouched(StatusCode statusCode)
      throws Exception {
    Fixture fixture = fixture();
    fixture.protocol.setSendProblemInformation(false);
    PubAck5 response = new PubAck5(11, statusCode);

    fixture.handleResponse(response);

    assertNull(response.getProperties().get(MessagePropertyFactory.REASON_STRING));
    verify(fixture.selectorTask).push(response);
  }

  @ParameterizedTest
  @MethodSource("connAckStatusCodes")
  void connAckNeverReceivesProblemReasonString(StatusCode statusCode) throws Exception {
    Fixture fixture = fixture();
    fixture.protocol.setSendProblemInformation(true);
    ConnAck5 response = new ConnAck5();
    response.setStatusCode(statusCode);

    fixture.handleResponse(response);

    assertNull(response.getProperties().get(MessagePropertyFactory.REASON_STRING));
    verify(fixture.selectorTask).push(response);
  }

  @ParameterizedTest
  @MethodSource("inactiveKeepAliveCases")
  void keepAliveReturnsBeforeManagerForInactiveSession(
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
      assertInstanceOf(PingReq5.class, fixture.frames.get(0));
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
  void pingResponseResetsKeepAliveManager() throws Exception {
    Fixture fixture = fixture();

    fixture.protocol.pingResponseReceived();

    verify(fixture.keepAliveManager).pingResponseReceived();
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
  void setConnectedWithNoOutstandingTimeoutIsSafe() throws Exception {
    Fixture fixture = fixture();

    assertDoesNotThrow(() -> fixture.protocol.setConnected(false));

    assertNull(field(fixture.protocol, "connectionTimeOut"));
  }

  @Test
  void subjectComesFromCurrentSessionSecurityContext() throws Exception {
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
  void writeFrameQueuesPacketAndUpdatesSentCount() throws Exception {
    Fixture fixture = fixture();
    doCallRealMethod().when(fixture.protocol).writeFrame(any(ServerPacket.class));
    PingReq5 frame = new PingReq5();

    fixture.protocol.writeFrame(frame);

    verify(fixture.endPointStatus).incrementSentMessages();
    verify(fixture.selectorTask).push(frame);
  }

  @ParameterizedTest
  @MethodSource("credentialCases")
  void connectBuildsCredentialFlagsAndTrimsPassword(
      String username,
      String password,
      boolean expectedUsername,
      boolean expectedPassword,
      String expectedPasswordValue) throws Exception {
    Fixture fixture = fixture();

    try {
      fixture.protocol.connect("client-1", username, password);

      Connect5 connect = assertInstanceOf(Connect5.class, fixture.onlyFrame());
      assertEquals(expectedUsername, connect.hasUsername());
      assertEquals(expectedPassword, connect.hasPassword());
      if (expectedUsername) {
        assertEquals(username, connect.getUsername());
      }
      if (expectedPassword) {
        assertEquals(expectedPasswordValue, new String(connect.getPassword()));
      }
      assertEquals("client-1", connect.getSessionId());
      verify(fixture.selectorTask).register(SelectionKey.OP_READ);
      verify(fixture.endPoint).completedConnection();
    } finally {
      fixture.protocol.setConnected(false);
    }
  }

  @ParameterizedTest
  @MethodSource("willCases")
  void connectCopiesWillPayloadQosRetainAndProperties(
      QualityOfService qos,
      boolean retain) throws Exception {
    Fixture fixture = fixture();
    byte[] payload = ("will-" + qos + "-" + retain).getBytes(StandardCharsets.UTF_8);
    Publish5 will = new Publish5(payload, qos, 7, "/will/topic", retain);
    will.add(new UserProperty("source", "coverage"));

    try {
      fixture.protocol.connect("client-will", null, null, will);

      Connect5 connect = assertInstanceOf(Connect5.class, fixture.onlyFrame());
      assertTrue(connect.isWillFlag());
      assertEquals("/will/topic", connect.getWillTopic());
      assertArrayEquals(payload, connect.getWillMsg());
      assertEquals(qos, connect.getWillQOS());
      assertEquals(retain, connect.isWillRetain());
      assertSame(will.getProperties(), connect.getWillProperties());
    } finally {
      fixture.protocol.setConnected(false);
    }
  }

  private static Fixture fixture() throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    EndPointStatus endPointStatus = mock(EndPointStatus.class);
    MqttConfig mqttConfig = new MqttConfig();
    mqttConfig.setMaxServerKeepAlive(60);

    EndPointConfigDTO endPointConfig = new EndPointConfigDTO("tcp");
    EndPointServerConfigDTO serverConfig = new EndPointServerConfigDTO();
    serverConfig.setName("mqtt5-lifecycle");
    serverConfig.setEndPointConfig(endPointConfig);
    serverConfig.setProtocolConfigs(List.of(mqttConfig));

    int id = FIXTURE_IDS.incrementAndGet();
    when(endPoint.getConfig()).thenReturn(serverConfig);
    when(endPoint.getName()).thenReturn("mqtt5-lifecycle-" + id);
    when(endPoint.getJMXTypePath()).thenReturn(List.of("Lifecycle=" + id));
    when(endPoint.getEndPointStatus()).thenReturn(endPointStatus);

    MQTT5Protocol protocol = spy(new MQTT5Protocol(endPoint));
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
    Field field = MQTT5Protocol.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = MQTT5Protocol.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Object invoke(
      Object target, String name, Class<?>[] types, Object... args) throws Exception {
    Method method = MQTT5Protocol.class.getDeclaredMethod(name, types);
    method.setAccessible(true);
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof Exception checked) {
        throw checked;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw exception;
    }
  }

  private static Stream<Arguments> connectRoutingCases() {
    return Stream.of(
        Arguments.of(false, false, false),
        Arguments.of(false, true, false),
        Arguments.of(true, false, false),
        Arguments.of(true, true, true));
  }

  private static Stream<Arguments> problemStatusCodes() {
    return Stream.of(
        Arguments.of(StatusCode.SUCCESS),
        Arguments.of(StatusCode.NO_MATCHING_SUBSCRIBERS),
        Arguments.of(StatusCode.MALFORMED_PACKET),
        Arguments.of(StatusCode.BAD_USERNAME_PASSWORD),
        Arguments.of(StatusCode.SERVER_BUSY));
  }

  private static Stream<Arguments> problemInformationDisabledCases() {
    return Stream.of(
        Arguments.of(StatusCode.SUCCESS),
        Arguments.of(StatusCode.PROTOCOL_ERROR));
  }

  private static Stream<Arguments> connAckStatusCodes() {
    return Stream.of(
        Arguments.of(StatusCode.SUCCESS),
        Arguments.of(StatusCode.BAD_USERNAME_PASSWORD));
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
        Arguments.of(false, MqttKeepAliveManager.Action.NONE, false));
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

  private static Stream<Arguments> credentialCases() {
    return Stream.of(
        Arguments.of(null, null, false, false, null),
        Arguments.of(null, "secret", false, false, null),
        Arguments.of("", " pass ", false, true, "pass"),
        Arguments.of("user", " pass ", true, true, "pass"),
        Arguments.of(" user ", "x", true, true, "x"),
        Arguments.of("user", "   ", true, false, null));
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

  private static final class Fixture {
    private final MQTT5Protocol protocol;
    private final EndPoint endPoint;
    private final EndPointStatus endPointStatus;
    private final SelectorTask selectorTask;
    private final MqttKeepAliveManager keepAliveManager;
    private final List<ServerPacket> frames;

    private Fixture(
        MQTT5Protocol protocol,
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

    private void handleResponse(MQTTPacket5 response) throws Exception {
      invoke(
          protocol,
          "handleResponse",
          new Class<?>[]{MQTTPacket5.class},
          response);
    }

    private ServerPacket onlyFrame() {
      assertEquals(1, frames.size());
      return frames.get(0);
    }
  }
}
