/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkAcceptedSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.mavlink.MavlinkEventFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.mavlink.message.Frame;
import io.mapsmessaging.mavlink.message.Version;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MavlinkProtocolFlowCoverageTest {

  @ParameterizedTest
  @MethodSource("ignoredOutboundCases")
  void outboundMessageAlwaysCompletesAndRejectsBadCorrelationOrPayload(
      String correlation, String payload) throws Exception {
    Fixture fixture = fixture();
    Message message = mock(Message.class);
    when(message.getCorrelationData()).thenReturn(
        correlation == null ? null : correlation.getBytes(StandardCharsets.UTF_8));
    when(message.getOpaqueData()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));
    Runnable completion = mock(Runnable.class);

    fixture.protocol.sendMessage(
        new MessageEvent("/outbound", mock(SubscribedEventManager.class), message, completion));

    verify(completion).run();
    verify(fixture.endPoint, never()).sendPacket(any(Packet.class));
    verify(fixture.factory, never()).writeTlog(any());
  }

  @Test
  void validOutboundMessageOverridesSequenceAndWritesFrame() throws Exception {
    Fixture fixture = fixture();
    byte[] frame = new byte[]{1, 2, 3, 4};
    when(fixture.formatter.parseFromJson(any(JsonObject.class))).thenReturn(frame);
    when(fixture.endPoint.sendPacket(any(Packet.class))).thenReturn(frame.length);
    Runnable completion = mock(Runnable.class);

    fixture.protocol.sendMessage(event(
        "ID#42#127.0.0.1:14550",
        "{\"header\":{\"systemId\":7,\"componentId\":8},\"message\":{}}",
        completion));

    ArgumentCaptor<JsonObject> json = ArgumentCaptor.forClass(JsonObject.class);
    verify(fixture.formatter).parseFromJson(json.capture());
    assertEquals(0, json.getValue().getAsJsonObject("header").get("sequence").getAsInt());
    assertEquals(7, json.getValue().getAsJsonObject("header").get("systemId").getAsInt());
    assertEquals(8, json.getValue().getAsJsonObject("header").get("componentId").getAsInt());
    verify(fixture.endPoint).sendPacket(any(Packet.class));
    verify(fixture.factory).writeTlog(frame);
    verify(completion).run();
  }

  @Test
  void localMavlinkIdentityOverridesOutboundHeader() throws Exception {
    Fixture fixture = fixture();
    fixture.config.setSystemId(255);
    fixture.config.setComponentId(190);
    byte[] frame = new byte[]{9, 8, 7};
    when(fixture.formatter.parseFromJson(any(JsonObject.class))).thenReturn(frame);
    when(fixture.endPoint.sendPacket(any(Packet.class))).thenReturn(frame.length);

    fixture.protocol.sendMessage(event(
        "ID#42#127.0.0.1:14550",
        "{\"header\":{\"systemId\":1,\"componentId\":2}}",
        mock(Runnable.class)));

    ArgumentCaptor<JsonObject> json = ArgumentCaptor.forClass(JsonObject.class);
    verify(fixture.formatter).parseFromJson(json.capture());
    JsonObject header = json.getValue().getAsJsonObject("header");
    assertEquals(255, header.get("systemId").getAsInt());
    assertEquals(190, header.get("componentId").getAsInt());
  }

  @Test
  void udpOutboundUsesCorrelationSocketAddress() throws Exception {
    Fixture fixture = fixture();
    when(fixture.endPoint.isUDP()).thenReturn(true);
    byte[] frame = new byte[]{4, 5, 6};
    when(fixture.formatter.parseFromJson(any(JsonObject.class))).thenReturn(frame);
    when(fixture.endPoint.sendPacket(any(Packet.class))).thenReturn(frame.length);

    fixture.protocol.sendMessage(event(
        "ID#42#vehicle.local:14555",
        "{\"header\":{\"systemId\":7,\"componentId\":8}}",
        mock(Runnable.class)));

    ArgumentCaptor<Packet> packet = ArgumentCaptor.forClass(Packet.class);
    verify(fixture.endPoint).sendPacket(packet.capture());
    InetSocketAddress address = assertInstanceOf(InetSocketAddress.class, packet.getValue().getFromAddress());
    assertEquals("vehicle.local", address.getHostString());
    assertEquals(14555, address.getPort());
    verify(fixture.factory).writeTlog(frame);
  }

  @Test
  void formatterFailureIsContainedAndStillCompletesMessage() throws Exception {
    Fixture fixture = fixture();
    when(fixture.formatter.parseFromJson(any(JsonObject.class)))
        .thenThrow(new IOException("expected"));
    Runnable completion = mock(Runnable.class);

    assertDoesNotThrow(() -> fixture.protocol.sendMessage(event(
        "ID#42#127.0.0.1:14550",
        "{\"header\":{\"systemId\":7,\"componentId\":8}}",
        completion)));

    verify(completion).run();
    verify(fixture.endPoint, never()).sendPacket(any(Packet.class));
    verify(fixture.factory, never()).writeTlog(any());
  }

  @Test
  void endpointSendFailureDoesNotWriteTlogAndStillCompletesMessage() throws Exception {
    Fixture fixture = fixture();
    byte[] frame = new byte[]{1, 1, 2, 3};
    when(fixture.formatter.parseFromJson(any(JsonObject.class))).thenReturn(frame);
    when(fixture.endPoint.sendPacket(any(Packet.class))).thenThrow(new IOException("expected"));
    Runnable completion = mock(Runnable.class);

    assertDoesNotThrow(() -> fixture.protocol.sendMessage(event(
        "ID#42#127.0.0.1:14550",
        "{\"header\":{\"systemId\":7,\"componentId\":8}}",
        completion)));

    verify(completion).run();
    verify(fixture.factory, never()).writeTlog(any());
  }

  @Test
  void missingDestinationFutureIsContained() throws Exception {
    Fixture fixture = fixture();
    when(fixture.session.findDestination("/topic", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(null);

    assertDoesNotThrow(() -> invoke(
        fixture.protocol,
        "sendMessage",
        new Class<?>[]{String.class, Message.class},
        "/topic",
        mock(Message.class)));
  }

  @Test
  void failedDestinationLookupIsContained() throws Exception {
    Fixture fixture = fixture();
    CompletableFuture<Destination> future = new CompletableFuture<>();
    future.completeExceptionally(new IOException("expected"));
    when(fixture.session.findDestination("/topic", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(future);

    assertDoesNotThrow(() -> invoke(
        fixture.protocol,
        "sendMessage",
        new Class<?>[]{String.class, Message.class},
        "/topic",
        mock(Message.class)));
  }

  @Test
  void resolvedDestinationStoresMessage() throws Exception {
    Fixture fixture = fixture();
    Destination destination = mock(Destination.class);
    Message message = mock(Message.class);
    when(fixture.session.findDestination("/topic", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));

    invoke(
        fixture.protocol,
        "sendMessage",
        new Class<?>[]{String.class, Message.class},
        "/topic",
        message);

    verify(destination).storeMessage(message);
  }

  @Test
  void destinationStoreFailureIsContained() throws Exception {
    Fixture fixture = fixture();
    Destination destination = mock(Destination.class);
    Message message = mock(Message.class);
    when(fixture.session.findDestination("/topic", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    when(destination.storeMessage(message)).thenThrow(new IOException("expected"));

    assertDoesNotThrow(() -> invoke(
        fixture.protocol,
        "sendMessage",
        new Class<?>[]{String.class, Message.class},
        "/topic",
        message));
  }

  @Test
  void processedFramePublishesExpectedEnvelopeMetadataAndCorrelation() throws Exception {
    Fixture fixture = fixture();
    Destination destination = mock(Destination.class);
    when(fixture.session.findDestination(
        "/vehicle.local_14551/3/7/HEARTBEAT",
        io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    Frame frame = frame(0, 12, true, new byte[]{10, 20, 30});
    byte[] raw = new byte[]{1, 2, 3};

    assertTrue(fixture.protocol.processPacket(frame, "HEARTBEAT", raw, "127.0.0.1:14550"));

    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
    verify(destination).storeMessage(messageCaptor.capture());
    Message message = messageCaptor.getValue();
    assertArrayEquals(raw, message.getOpaqueData());
    assertEquals("mavlink", message.getContentType());
    assertEquals("/outbound", message.getResponseTopic());
    assertEquals("ID#42#127.0.0.1:14550",
        new String(message.getCorrelationData(), StandardCharsets.UTF_8));
    assertEquals("MavLink", message.getMeta().get("protocol"));
    assertEquals("V2", message.getMeta().get("version"));
    assertEquals("mavlink-session", message.getMeta().get("sessionId"));
    assertEquals(3L, number(message.getDataMap().get("systemId")));
    assertEquals(7L, number(message.getDataMap().get("componentId")));
    assertEquals(12L, number(message.getDataMap().get("sequence")));
    assertArrayEquals(new byte[]{10, 20, 30},
        (byte[]) message.getDataMap().get("payload").getData());
    assertEquals(Boolean.TRUE, message.getDataMap().get("signed").getData());
  }

  @ParameterizedTest
  @MethodSource("rejectedFrameCases")
  void rejectedFramePublicationPreservesRawOrBuildsMetadata(
      boolean includeMetadata, boolean expectJson) throws Exception {
    Fixture fixture = fixture();
    fixture.config.setRejectedFrameNamespace("/rejected/{messageId}");
    fixture.config.setIncludeRejectedFrameMetadata(includeMetadata);
    Destination destination = mock(Destination.class);
    when(fixture.session.findDestination(
        "/rejected/33",
        io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    Frame frame = frame(33, 17, true, new byte[]{4, 5, 6});
    ProcessedFrame processed = mock(ProcessedFrame.class);
    when(processed.getFrame()).thenReturn(frame);
    when(processed.getMessageName()).thenReturn("GLOBAL_POSITION_INT");
    byte[] raw = new byte[]{7, 8, 9};

    invoke(
        fixture.protocol,
        "handleRejectedEvents",
        new Class<?>[]{ProcessedFrame.class, byte[].class},
        processed,
        raw);

    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
    verify(destination).storeMessage(messageCaptor.capture());
    String payload = new String(messageCaptor.getValue().getOpaqueData(), StandardCharsets.UTF_8);
    if (expectJson) {
      assertTrue(payload.contains("\"messageName\":\"GLOBAL_POSITION_INT\""));
      assertTrue(payload.contains("\"messageId\":33"));
      assertTrue(payload.contains("\"systemId\":3"));
      assertTrue(payload.contains("\"componentId\":7"));
      assertTrue(payload.contains("\"sequence\":17"));
      assertTrue(payload.contains("\"signed\":true"));
      assertTrue(payload.contains("\"payload\":\"BAUG\""));
    } else {
      assertArrayEquals(raw, messageCaptor.getValue().getOpaqueData());
    }
  }

  @Test
  void rejectedFrameWithBlankNamespaceDoesNothing() throws Exception {
    Fixture fixture = fixture();
    fixture.config.setRejectedFrameNamespace("");
    ProcessedFrame processed = mock(ProcessedFrame.class);
    when(processed.getFrame()).thenReturn(frame(33, 17, false, new byte[0]));

    invoke(
        fixture.protocol,
        "handleRejectedEvents",
        new Class<?>[]{ProcessedFrame.class, byte[].class},
        processed,
        new byte[]{1});

    verify(fixture.session, never()).findDestination(anyString(), any());
  }

  @Test
  void rawFrameWithoutDecodedEnvelopeUpdatesInputCountersAndStops() throws Exception {
    Fixture fixture = fixture();
    byte[] raw = new byte[]{1, 2, 3, 4};
    when(fixture.eventFactory.unpack(eq("mavlink-test"), any(ByteBuffer.class)))
        .thenReturn(Optional.empty());

    fixture.protocol.processRawFrame(raw, "127.0.0.1:14550");

    verify(fixture.status).incrementReceivedMessages();
    verify(fixture.endPoint).updateReadBytes(raw.length);
    verify(fixture.session, never()).findDestination(anyString(), any());
  }

  @ParameterizedTest
  @MethodSource("dialectDetectionCases")
  void firstHeartbeatMarksDialectDetectedWithoutPublishingRejectedFrame(
      Integer autopilot, boolean includeAutopilot) throws Exception {
    Fixture fixture = fixture();
    fixture.config.setRejectedFrameNamespace("");
    fixture.config.setStatusTopicNameTemplate("");
    setField(
        fixture.protocol,
        MavlinkProtocol.class,
        "acceptedComponents",
        Map.of(99, acceptedSource(99)));
    Frame frame = frame(0, 1, false, new byte[0]);
    ProcessedFrame processed = mock(ProcessedFrame.class);
    when(processed.getFrame()).thenReturn(frame);
    when(processed.getMessageName()).thenReturn("HEARTBEAT");
    when(processed.getFields()).thenReturn(
        includeAutopilot ? Map.of("autopilot", autopilot) : Map.of());
    when(fixture.eventFactory.unpack(eq("mavlink-test"), any(ByteBuffer.class)))
        .thenReturn(Optional.of(processed));

    fixture.protocol.processRawFrame(new byte[]{1, 2}, "127.0.0.1:14550");

    assertTrue((Boolean) field(fixture.protocol, MavlinkProtocol.class, "detectedDialect"));
    verify(fixture.session, never()).findDestination(anyString(), any());
  }

  @Test
  void nonHeartbeatFrameDoesNotMarkDialectDetected() throws Exception {
    Fixture fixture = fixture();
    fixture.config.setRejectedFrameNamespace("");
    fixture.config.setStatusTopicNameTemplate("");
    setField(
        fixture.protocol,
        MavlinkProtocol.class,
        "acceptedComponents",
        Map.of(99, acceptedSource(99)));
    Frame frame = frame(33, 1, false, new byte[0]);
    ProcessedFrame processed = mock(ProcessedFrame.class);
    when(processed.getFrame()).thenReturn(frame);
    when(processed.getMessageName()).thenReturn("GLOBAL_POSITION_INT");
    when(processed.getFields()).thenReturn(Map.of());
    when(fixture.eventFactory.unpack(eq("mavlink-test"), any(ByteBuffer.class)))
        .thenReturn(Optional.of(processed));

    fixture.protocol.processRawFrame(new byte[]{1, 2}, "127.0.0.1:14550");

    assertFalse((Boolean) field(fixture.protocol, MavlinkProtocol.class, "detectedDialect"));
  }

  @ParameterizedTest
  @MethodSource("conversionCases")
  void frameConversionPreservesHeaderFields(
      int sequence, boolean signed, byte[] payload) throws Exception {
    Fixture fixture = fixture();
    Frame frame = frame(33, sequence, signed, payload);

    @SuppressWarnings("unchecked")
    Map<String, TypedData> converted = (Map<String, TypedData>) invoke(
        fixture.protocol,
        "convertToMap",
        new Class<?>[]{Frame.class},
        frame);

    assertEquals("V2", converted.get("version").getData());
    assertEquals(3L, number(converted.get("systemId")));
    assertEquals(7L, number(converted.get("componentId")));
    assertEquals((long) sequence, number(converted.get("sequence")));
    assertArrayEquals(payload, (byte[]) converted.get("payload").getData());
    assertEquals(signed, converted.get("signed").getData());
  }

  private static MessageEvent event(String correlation, String payload, Runnable completion) {
    Message message = mock(Message.class);
    when(message.getCorrelationData()).thenReturn(correlation.getBytes(StandardCharsets.UTF_8));
    when(message.getOpaqueData()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));
    return new MessageEvent(
        "/outbound",
        mock(SubscribedEventManager.class),
        message,
        completion);
  }

  private static long number(TypedData data) {
    return ((Number) data.getData()).longValue();
  }

  private static MavlinkAcceptedSourceDTO acceptedSource(int componentId) {
    MavlinkAcceptedSourceDTO source = new MavlinkAcceptedSourceDTO();
    source.setSystemId(3);
    source.setComponentId(componentId);
    source.setAcceptedMessageIds(List.of());
    return source;
  }

  private static Frame frame(int messageId, int sequence, boolean signed, byte[] payload) {
    Frame frame = new Frame();
    frame.setVersion(Version.V2);
    frame.setSystemId(3);
    frame.setComponentId(7);
    frame.setMessageId(messageId);
    frame.setSequence(sequence);
    frame.setSigned(signed);
    frame.setPayload(payload);
    return frame;
  }

  private static Stream<Arguments> ignoredOutboundCases() {
    return Stream.of(
        Arguments.of(null, "{}"),
        Arguments.of("", "{}"),
        Arguments.of("not-an-id", "{}"),
        Arguments.of("ID#", "{}"),
        Arguments.of("ID#42", "{}"),
        Arguments.of("ID#41#127.0.0.1:14550", "{}"),
        Arguments.of("XX#42#127.0.0.1:14550", "{}"),
        Arguments.of("ID#42#127.0.0.1:14550", "{"));
  }

  private static Stream<Arguments> rejectedFrameCases() {
    return Stream.of(
        Arguments.of(false, false),
        Arguments.of(true, true));
  }

  private static Stream<Arguments> dialectDetectionCases() {
    return Stream.of(
        Arguments.of(99, true),
        Arguments.of(0, true),
        Arguments.of(0, false));
  }

  private static Stream<Arguments> conversionCases() {
    return Stream.of(
        Arguments.of(0, false, new byte[0]),
        Arguments.of(1, true, new byte[]{1}),
        Arguments.of(127, false, new byte[]{1, 2, 3}),
        Arguments.of(255, true, new byte[]{(byte) 0xff, 0, 1}));
  }

  private static Fixture fixture() throws Exception {
    MavlinkProtocol protocol = mock(MavlinkProtocol.class, Answers.CALLS_REAL_METHODS);
    MavlinkConfigDTO config = new MavlinkConfigDTO();
    config.setTopicNameTemplate(
        "/{remoteSocket}/{systemId}/{componentId}/{messageName}");
    config.setStatusTopicNameTemplate("");
    config.setRejectedFrameNamespace("");
    EndPoint endPoint = mock(EndPoint.class);
    EndPointStatus status = mock(EndPointStatus.class);
    Session session = mock(Session.class);
    MavlinkConnectionManager factory = mock(MavlinkConnectionManager.class);
    MessageFormatter formatter = mock(MessageFormatter.class);
    MavlinkEventFactory eventFactory = mock(MavlinkEventFactory.class);
    MavlinkDeviceKey key = new MavlinkDeviceKey(
        14550,
        InetSocketAddress.createUnresolved("vehicle.local", 14551),
        3);

    when(endPoint.getId()).thenReturn(42L);
    when(endPoint.getName()).thenReturn("mavlink-test");
    when(endPoint.getEndPointStatus()).thenReturn(status);
    when(session.getName()).thenReturn("mavlink-session");

    setField(protocol, Protocol.class, "endPoint", endPoint);
    setField(protocol, MavlinkProtocol.class, "factory", factory);
    setField(protocol, MavlinkProtocol.class, "key", key);
    setField(protocol, MavlinkProtocol.class, "mavlinkConfig", config);
    setField(protocol, MavlinkProtocol.class, "session", session);
    setField(protocol, MavlinkProtocol.class, "acceptedComponents",
        new LinkedHashMap<Integer, MavlinkAcceptedSourceDTO>());
    setField(protocol, MavlinkProtocol.class, "sequenceTrackers", new ConcurrentHashMap<>());
    setField(protocol, MavlinkProtocol.class, "outboundTopicName", "/outbound");
    setField(protocol, MavlinkProtocol.class, "mavlinkEventFactory", eventFactory);
    setField(protocol, MavlinkProtocol.class, "formatter", formatter);
    setField(protocol, MavlinkProtocol.class, "qos", QualityOfService.AT_MOST_ONCE);
    setField(protocol, MavlinkProtocol.class, "storeOffline", false);
    setField(protocol, MavlinkProtocol.class, "sequenceCounter", new AtomicInteger());
    setField(protocol, MavlinkProtocol.class, "gson", GsonFactory.createStrictJsonWithSafeFloats());
    setField(protocol, MavlinkProtocol.class, "detectedDialect", false);

    return new Fixture(protocol, config, endPoint, status, session, factory, formatter, eventFactory);
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(
      Object target, String name, Class<?>[] parameterTypes, Object... arguments)
      throws Exception {
    Method method = MavlinkProtocol.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    try {
      return (T) method.invoke(target, arguments);
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

  private static Object field(Object target, Class<?> owner, String name) throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void setField(Object target, Class<?> owner, String name, Object value)
      throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Fixture(
      MavlinkProtocol protocol,
      MavlinkConfigDTO config,
      EndPoint endPoint,
      EndPointStatus status,
      Session session,
      MavlinkConnectionManager factory,
      MessageFormatter formatter,
      MavlinkEventFactory eventFactory) {
  }
}
