/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.protocol.impl.canaerospace;

import com.google.gson.JsonObject;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.network.io.impl.canbus.CanbusEndPoint;
import io.mapsmessaging.engine.schema.Schema;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CanaerospaceProtocolCoverageTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "Engine RPM",
      "fuel/flow",
      "hydraulic pressure",
      "A+B",
      "temp°C",
      "name:with:colon",
      "tabs\there",
      "a#b",
      "a+b",
      "a b c"
  })
  void incomingMessageNamesAreSanitisedBeforeTopicUse(String incomingName) throws Exception {
    Harness harness = harness();
    JsonObject json = payload(incomingName);

    assertTrue(harness.protocol.processPacket(frame(0x101), json));

    String topic = capturedTopic(harness.session);
    assertFalse(topic.contains(" "));
    assertFalse(topic.contains("/"));
    assertFalse(topic.contains("+"));
    assertFalse(topic.contains("#"));
    assertFalse(topic.contains(":"));
  }

  @Test
  void incomingKnownFramePublishesJsonMessageWithExpectedMetadata() throws Exception {
    Harness harness = harness();
    JsonObject json = payload("Engine_RPM");

    assertTrue(harness.protocol.processPacket(frame(0x102), json));

    Message message = capturedMessage(harness.destination);
    assertEquals(json.toString(), new String(message.getOpaqueData(), StandardCharsets.UTF_8));
    assertEquals("application/json", message.getContentType());
    assertEquals(QualityOfService.AT_LEAST_ONCE, message.getQualityOfService());
    assertTrue(message.isStoreOffline());
    assertFalse(message.isRetain());
    assertEquals("json-schema-id", message.getSchemaId());
    assertEquals("canaerospace", message.getMeta().get("protocol"));
    assertEquals("1.0", message.getMeta().get("version"));
    assertEquals("session-name", message.getMeta().get("sessionId"));
    assertNotNull(message.getMeta().get("time_ms"));
  }

  @Test
  void incomingNameIsCachedByCanIdentifier() throws Exception {
    Harness harness = harness();

    harness.protocol.processPacket(frame(0x103), payload("First Name"));
    harness.protocol.processPacket(frame(0x103), payload("Different Name"));

    verify(harness.session, times(2))
        .findDestination(eq("/can/vcan1/First_Name"), eq(DestinationType.TOPIC));
    verify(harness.session, never())
        .findDestination(eq("/can/vcan1/Different_Name"), eq(DestinationType.TOPIC));
  }

  @Test
  void missingNameFallsBackToRawTopic() throws Exception {
    Harness harness = harness();
    JsonObject json = new JsonObject();
    json.add("canaerospace", new JsonObject());

    harness.protocol.processPacket(frame(0x104), json);

    verify(harness.session)
        .findDestination("/can/vcan1/unknown", DestinationType.TOPIC);
  }

  @Test
  void absentCanaerospaceObjectFallsBackToRawTopic() throws Exception {
    Harness harness = harness();
    JsonObject json = new JsonObject();
    json.addProperty("other", "value");

    harness.protocol.processPacket(frame(0x105), json);

    verify(harness.session)
        .findDestination("/can/vcan1/unknown", DestinationType.TOPIC);
  }

  @Test
  void nullJsonFallsBackToRawTopic() throws Exception {
    Harness harness = harness();

    harness.protocol.processPacket(frame(0x106), null);

    verify(harness.session)
        .findDestination("/can/vcan1/unknown", DestinationType.TOPIC);
  }

  @Test
  void destinationWithoutSchemaIsUpgradedBeforeStore() throws Exception {
    Harness harness = harness();
    when(harness.destination.getSchema()).thenReturn(null);

    harness.protocol.processPacket(frame(0x107), payload("Pressure"));

    verify(harness.destination).updateSchema(harness.defaultSchema, null);
    verify(harness.destination).storeMessage(any(Message.class));
  }

  @Test
  void destinationWithRawSchemaIsUpgradedBeforeStore() throws Exception {
    Harness harness = harness();
    Schema rawSchema = mock(Schema.class);
    when(rawSchema.getUniqueId()).thenReturn(
        io.mapsmessaging.engine.schema.SchemaManager.DEFAULT_RAW_UUID.toString());
    when(harness.destination.getSchema()).thenReturn(rawSchema);

    harness.protocol.processPacket(frame(0x108), payload("Pressure"));

    verify(harness.destination).updateSchema(harness.defaultSchema, null);
    verify(harness.destination).storeMessage(any(Message.class));
  }

  @Test
  void destinationWithSpecificSchemaIsNotRewritten() throws Exception {
    Harness harness = harness();
    Schema schema = mock(Schema.class);
    when(schema.getUniqueId()).thenReturn("specific-schema");
    when(harness.destination.getSchema()).thenReturn(schema);

    harness.protocol.processPacket(frame(0x109), payload("Pressure"));

    verify(harness.destination, never()).updateSchema(any(), any());
    verify(harness.destination).storeMessage(any(Message.class));
  }

  @Test
  void nullDestinationFutureIsIgnored() throws Exception {
    Harness harness = harness();
    when(harness.session.findDestination(anyString(), eq(DestinationType.TOPIC))).thenReturn(null);

    assertDoesNotThrow(() -> harness.protocol.processPacket(frame(0x10A), payload("Pressure")));
  }

  @Test
  void futureCompletingWithNullDestinationIsIgnored() throws Exception {
    Harness harness = harness();
    when(harness.session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenReturn(CompletableFuture.completedFuture(null));

    assertDoesNotThrow(() -> harness.protocol.processPacket(frame(0x10B), payload("Pressure")));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "EngineRPM",
      "engine_rpm",
      "engine-rpm",
      "engine.rpm",
      "ABC123",
      "a_b-c.d"
  })
  void legalMessageNameCharactersArePreserved(String name) throws Exception {
    Harness harness = harness();

    harness.protocol.processPacket(frame(0x10C), payload(name));

    verify(harness.session)
        .findDestination("/can/vcan1/" + name, DestinationType.TOPIC);
  }

  @Test
  void topicFallsBackWhenNameIsEmpty() throws Exception {
    Harness harness = harness();
    assertEquals("/can/vcan1/unknown", computeTopic(harness.protocol, ""));
  }

  @Test
  void topicFallsBackWhenNameIsNull() throws Exception {
    Harness harness = harness();
    assertEquals("/can/vcan1/unknown", computeTopic(harness.protocol, null));
  }

  @Test
  void packetDispatchRejectsMissingCanFrame() throws Exception {
    Harness harness = harness();
    when(harness.endPoint.readFrame()).thenReturn(null);

    java.io.IOException failure = assertThrows(
        java.io.IOException.class,
        () -> harness.protocol.processPacket(mock(io.mapsmessaging.network.io.Packet.class)));

    assertEquals("No frame received from CAN bus", failure.getMessage());
  }

  @Test
  void packetDispatchPublishesRawFrameWhenJsonParsingIsDisabled() throws Exception {
    Harness harness = harness();
    CanFrame frame = frame(0x120);
    set(harness.protocol, "parseToJson", false);
    when(harness.endPoint.readFrame()).thenReturn(frame);

    assertTrue(harness.protocol.processPacket(mock(io.mapsmessaging.network.io.Packet.class)));

    verify(harness.session)
        .findDestination("/can/vcan1/unknown", DestinationType.TOPIC);
    Message message = capturedMessage(harness.destination);
    assertArrayEquals(frame.getRawData(), message.getOpaqueData());
    assertEquals("application/octet-stream", message.getContentType());
    assertEquals(QualityOfService.AT_MOST_ONCE, message.getQualityOfService());
    verifyNoInteractions(harness.formatter);
  }

  @Test
  void packetDispatchParsesAndPublishesJsonWhenEnabled() throws Exception {
    Harness harness = harness();
    CanFrame frame = frame(0x121);
    JsonObject json = payload("Engine RPM");
    set(harness.protocol, "parseToJson", true);
    when(harness.endPoint.readFrame()).thenReturn(frame);
    when(harness.formatter.parseToJson(any(byte[].class), eq(ParseMode.IGNORE))).thenReturn(json);

    assertTrue(harness.protocol.processPacket(mock(io.mapsmessaging.network.io.Packet.class)));

    verify(harness.formatter).parseToJson(any(byte[].class), eq(ParseMode.IGNORE));
    verify(harness.session)
        .findDestination("/can/vcan1/Engine_RPM", DestinationType.TOPIC);
    Message message = capturedMessage(harness.destination);
    assertEquals("application/json", message.getContentType());
  }

  @Test
  void metadataMethodsReportProtocolIdentity() throws Exception {
    Harness harness = harness();

    assertEquals("canaerospace", harness.protocol.getName());
    assertEquals("1.0", harness.protocol.getVersion());
    assertEquals("session-name", harness.protocol.getSessionId());
  }

  private static Harness harness() throws Exception {
    CanaerospaceProtocol protocol = mock(CanaerospaceProtocol.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    CanbusEndPoint endPoint = mock(CanbusEndPoint.class);
    SchemaConfig defaultSchema = mock(SchemaConfig.class);
    MessageFormatter formatter = mock(MessageFormatter.class);

    when(session.getName()).thenReturn("session-name");
    when(endPoint.getName()).thenReturn("vcan1");
    when(defaultSchema.getUniqueId()).thenReturn("json-schema-id");
    when(session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenReturn(CompletableFuture.completedFuture(destination));

    set(protocol, "session", session);
    set(protocol, "topicTemplate", "/can/{candevice}/{messageName}");
    set(protocol, "rawTopicTemplate", "/can/vcan1/unknown");
    set(protocol, "mapCanIdToName", new java.util.concurrent.ConcurrentHashMap<Integer, String>());
    set(protocol, "qos", QualityOfService.AT_LEAST_ONCE);
    set(protocol, "storeOffline", true);
    set(protocol, "defaultSchemaConfig", defaultSchema);
    set(protocol, "formatter", formatter);
    set(protocol, "parseMode", ParseMode.IGNORE);
    setInherited(protocol, "endPoint", endPoint);

    return new Harness(protocol, session, destination, defaultSchema, endPoint, formatter);
  }

  private static JsonObject payload(String name) {
    JsonObject root = new JsonObject();
    JsonObject canaerospace = new JsonObject();
    canaerospace.addProperty("name", name);
    root.add("canaerospace", canaerospace);
    root.addProperty("value", 42);
    return root;
  }

  private static CanFrame frame(int id) {
    byte[] data = new byte[]{1,2,3,4,5,6,7,8};
    return new CanFrame(id, false, data.length, data);
  }

  private static String capturedTopic(Session session) {
    ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
    verify(session).findDestination(topic.capture(), eq(DestinationType.TOPIC));
    return topic.getValue();
  }

  private static Message capturedMessage(Destination destination) throws Exception {
    ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
    verify(destination).storeMessage(message.capture());
    return message.getValue();
  }

  private static String computeTopic(CanaerospaceProtocol protocol, String name) throws Exception {
    Method method = CanaerospaceProtocol.class.getDeclaredMethod("computeTopicName", String.class);
    method.setAccessible(true);
    return (String) method.invoke(protocol, name);
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = CanaerospaceProtocol.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void setInherited(Object target, String name, Object value) throws Exception {
    Class<?> type = target.getClass().getSuperclass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
        return;
      } catch (NoSuchFieldException ignored) {
        type = type.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }

  private record Harness(
      CanaerospaceProtocol protocol,
      Session session,
      Destination destination,
      SchemaConfig defaultSchema,
      CanbusEndPoint endPoint,
      MessageFormatter formatter) {
  }
}
