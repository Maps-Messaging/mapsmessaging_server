/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mavlink;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkAcceptedSourceDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.mavlink.message.Frame;
import io.mapsmessaging.mavlink.message.Version;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MavlinkProtocolHeaderCoverageTest {

  @ParameterizedTest
  @MethodSource("allowMessageIdCases")
  void allowMessageIdCoversSourceAndGlobalFilters(
      Map<Integer, MavlinkAcceptedSourceDTO> acceptedComponents,
      List<Integer> globalAccepted,
      int componentId,
      int messageId,
      boolean expected) throws Exception {
    Fixture fixture = fixture();
    fixture.config.setAcceptedMessageIds(globalAccepted);
    setField(fixture.protocol, MavlinkProtocol.class, "acceptedComponents", acceptedComponents);

    assertEquals(expected, fixture.protocol.allowMessageId(componentId, messageId));
  }

  @ParameterizedTest
  @MethodSource("headerValidationCases")
  void validateOutboundHeaderCoversIdentityBoundaries(String json, boolean valid)
      throws Exception {
    Fixture fixture = fixture();
    JsonObject input = JsonParser.parseString(json).getAsJsonObject();

    if (valid) {
      assertDoesNotThrow(() -> invoke(
          fixture.protocol, "validateOutboundHeader", new Class<?>[] {JsonObject.class}, input));
    } else {
      Throwable thrown = assertThrows(Throwable.class, () -> invoke(
          fixture.protocol, "validateOutboundHeader", new Class<?>[] {JsonObject.class}, input));
      assertInstanceOf(IllegalArgumentException.class, thrown);
    }
  }

  @ParameterizedTest
  @MethodSource("unsignedByteCases")
  void requiredUnsignedByteCoversMissingAndBounds(
      String json, boolean valid, Integer expected) throws Exception {
    Fixture fixture = fixture();
    JsonObject input = JsonParser.parseString(json).getAsJsonObject();

    if (valid) {
      assertEquals(expected, invoke(
          fixture.protocol,
          "getRequiredUnsignedByte",
          new Class<?>[] {JsonObject.class, String.class},
          input,
          "value"));
    } else {
      Throwable thrown = assertThrows(Throwable.class, () -> invoke(
          fixture.protocol,
          "getRequiredUnsignedByte",
          new Class<?>[] {JsonObject.class, String.class},
          input,
          "value"));
      assertInstanceOf(IllegalArgumentException.class, thrown);
    }
  }

  @ParameterizedTest
  @MethodSource("topicCases")
  void topicTemplateExpansionCoversAllPlaceholders(
      String template, String messageName, String expected) throws Exception {
    Fixture fixture = fixture();

    assertEquals(
        expected,
        fixture.protocol.computeTopicName(template, frame(33, 9), messageName));
  }

  @Test
  void remoteSocketUsesDeviceKeyHostAndPort() throws Exception {
    assertEquals("vehicle.local_14551", fixture().protocol.getRemoteSocket());
  }

  @Test
  void nextSequenceWrapsAtUnsignedByteBoundary() throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, MavlinkProtocol.class, "sequenceCounter", new AtomicInteger(254));

    assertEquals(254, fixture.protocol.nextSequence());
    assertEquals(255, fixture.protocol.nextSequence());
    assertEquals(0, fixture.protocol.nextSequence());
    assertEquals(1, fixture.protocol.nextSequence());
  }

  @Test
  void overrideSequenceCreatesHeaderWithoutLocalIdentity() throws Exception {
    Fixture fixture = fixture();
    JsonObject input = new JsonObject();

    invoke(fixture.protocol, "overrideSequence", new Class<?>[] {JsonObject.class}, input);

    JsonObject header = input.getAsJsonObject("header");
    assertNotNull(header);
    assertEquals(0, header.get("sequence").getAsInt());
    assertFalse(header.has("systemId"));
    assertFalse(header.has("componentId"));
  }

  @Test
  void overrideSequenceAppliesConfiguredLocalIdentity() throws Exception {
    Fixture fixture = fixture();
    fixture.config.setSystemId(255);
    fixture.config.setComponentId(190);
    setField(fixture.protocol, MavlinkProtocol.class, "sequenceCounter", new AtomicInteger(17));
    JsonObject input = JsonParser.parseString(
        "{\"header\":{\"sequence\":99,\"systemId\":1,\"componentId\":2}}")
        .getAsJsonObject();

    invoke(fixture.protocol, "overrideSequence", new Class<?>[] {JsonObject.class}, input);

    JsonObject header = input.getAsJsonObject("header");
    assertEquals(17, header.get("sequence").getAsInt());
    assertEquals(255, header.get("systemId").getAsInt());
    assertEquals(190, header.get("componentId").getAsInt());
  }

  @Test
  void packetEntryPointIsHandledByDedicatedReader() throws Exception {
    assertTrue(fixture().protocol.processPacket(new Packet(0, false)));
  }

  private static Fixture fixture() throws Exception {
    MavlinkProtocol protocol = mock(MavlinkProtocol.class, Answers.CALLS_REAL_METHODS);
    MavlinkConfigDTO config = new MavlinkConfigDTO();
    MavlinkDeviceKey key = new MavlinkDeviceKey(
        14550,
        InetSocketAddress.createUnresolved("vehicle.local", 14551),
        3);
    EndPoint endPoint = mock(EndPoint.class);

    setField(protocol, Protocol.class, "endPoint", endPoint);
    setField(protocol, MavlinkProtocol.class, "key", key);
    setField(protocol, MavlinkProtocol.class, "mavlinkConfig", config);
    setField(protocol, MavlinkProtocol.class, "acceptedComponents",
        new LinkedHashMap<Integer, MavlinkAcceptedSourceDTO>());
    setField(protocol, MavlinkProtocol.class, "sequenceCounter", new AtomicInteger());

    return new Fixture(protocol, config);
  }

  private static Frame frame(int messageId, int sequence) {
    Frame frame = new Frame();
    frame.setVersion(Version.V2);
    frame.setSystemId(3);
    frame.setComponentId(7);
    frame.setMessageId(messageId);
    frame.setSequence(sequence);
    frame.setPayload(new byte[0]);
    return frame;
  }

  private static MavlinkAcceptedSourceDTO source(List<Integer> acceptedIds) {
    MavlinkAcceptedSourceDTO source = new MavlinkAcceptedSourceDTO();
    source.setSystemId(3);
    source.setComponentId(7);
    source.setAcceptedMessageIds(acceptedIds);
    return source;
  }

  private static Stream<Arguments> allowMessageIdCases() {
    MavlinkAcceptedSourceDTO unrestricted = source(List.of());
    MavlinkAcceptedSourceDTO restricted = source(List.of(0, 33));
    return Stream.of(
        Arguments.of(Map.of(), List.of(), 7, 0, true),
        Arguments.of(Map.of(), List.of(0), 7, 33, true),
        Arguments.of(Map.of(7, unrestricted), List.of(), 8, 0, false),
        Arguments.of(Map.of(7, unrestricted), List.of(), 7, 999, true),
        Arguments.of(Map.of(7, unrestricted), List.of(0, 33), 7, 0, true),
        Arguments.of(Map.of(7, unrestricted), List.of(0, 33), 7, 1, false),
        Arguments.of(Map.of(7, restricted), List.of(), 7, 33, true),
        Arguments.of(Map.of(7, restricted), List.of(33), 7, 0, true),
        Arguments.of(Map.of(7, restricted), List.of(0), 7, 1, false)
    );
  }

  private static Stream<Arguments> headerValidationCases() {
    return Stream.of(
        Arguments.of("{}", false),
        Arguments.of("{\"header\":{}}", false),
        Arguments.of("{\"header\":{\"systemId\":null,\"componentId\":1}}", false),
        Arguments.of("{\"header\":{\"systemId\":1}}", false),
        Arguments.of("{\"header\":{\"systemId\":1,\"componentId\":null}}", false),
        Arguments.of("{\"header\":{\"systemId\":-1,\"componentId\":1}}", false),
        Arguments.of("{\"header\":{\"systemId\":256,\"componentId\":1}}", false),
        Arguments.of("{\"header\":{\"systemId\":1,\"componentId\":256}}", false),
        Arguments.of("{\"header\":{\"systemId\":0,\"componentId\":1}}", false),
        Arguments.of("{\"header\":{\"systemId\":1,\"componentId\":0}}", false),
        Arguments.of("{\"header\":{\"systemId\":1,\"componentId\":1}}", true),
        Arguments.of("{\"header\":{\"systemId\":255,\"componentId\":255}}", true)
    );
  }

  private static Stream<Arguments> unsignedByteCases() {
    return Stream.of(
        Arguments.of("{}", false, null),
        Arguments.of("{\"value\":null}", false, null),
        Arguments.of("{\"value\":-1}", false, null),
        Arguments.of("{\"value\":256}", false, null),
        Arguments.of("{\"value\":0}", true, 0),
        Arguments.of("{\"value\":1}", true, 1),
        Arguments.of("{\"value\":255}", true, 255)
    );
  }

  private static Stream<Arguments> topicCases() {
    return Stream.of(
        Arguments.of(
            "/{remoteSocket}/{systemId}/{componentId}/{messageId}/{messageName}",
            "GLOBAL_POSITION_INT",
            "/vehicle.local_14551/3/7/33/GLOBAL_POSITION_INT"),
        Arguments.of(
            "/{systemName}/{systemId}/{messageName}",
            "HEARTBEAT",
            "/3/3/HEARTBEAT"),
        Arguments.of("/static", "ANY", "/static")
    );
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

  private static void setField(Object target, Class<?> owner, String name, Object value)
      throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Fixture(MavlinkProtocol protocol, MavlinkConfigDTO config) {
  }
}
