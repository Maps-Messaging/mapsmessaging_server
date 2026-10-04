/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.n2k;

import com.google.gson.JsonObject;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.canbus.j1939.CanId;
import io.mapsmessaging.canbus.j1939.CanIdBuilder;
import io.mapsmessaging.canbus.j1939.n2k.framing.FramePacker;
import io.mapsmessaging.dto.rest.protocol.impl.N2kProtocolInformation;
import io.mapsmessaging.engine.schema.Schema;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.canbus.CanbusEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class N2kProtocolBranchCoverageTest {

  @Test
  void identifiesProtocolNameVersionAndSession() throws Exception {
    Fixture fixture = fixture();

    assertEquals("n2k", fixture.protocol.getName());
    assertEquals("1.0", fixture.protocol.getVersion());
    assertEquals("session-1", fixture.protocol.getSessionId());
  }

  @Test
  void returnsSessionSubject() throws Exception {
    Fixture fixture = fixture();
    SecurityContext context = mock(SecurityContext.class);
    Subject subject = new Subject();
    when(fixture.session.getSecurityContext()).thenReturn(context);
    when(context.getSubject()).thenReturn(subject);

    assertSame(subject, fixture.protocol.getSubject());
  }

  @Test
  void informationContainsSessionInformation() throws Exception {
    Fixture fixture = fixture();

    N2kProtocolInformation information = (N2kProtocolInformation) fixture.protocol.getInformation();

    assertNotNull(information);
    assertSame(fixture.sessionInformation, information.getSessionInfo());
    assertEquals("session-1", information.getSessionId());
  }

  @Test
  void nullMessageNameUsesRawTopic() throws Exception {
    Fixture fixture = fixture();

    assertEquals("/raw/can0", computeTopicName(fixture.protocol, 127250, null));
  }

  @ParameterizedTest
  @MethodSource("topicCases")
  void computesTemplatedTopic(String template, int pgn, String name, String expected) throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, N2kProtocol.class, "topicTemplate", template);

    assertEquals(expected, computeTopicName(fixture.protocol, pgn, name));
  }

  @ParameterizedTest
  @MethodSource("packetStringCases")
  void rendersCanFrameForDiagnostics(int canId, byte[] data) throws Exception {
    Fixture fixture = fixture();
    CanFrame frame = new CanFrame(canId, true, data.length, data);

    String rendered = invoke(fixture.protocol, "packetToString",
        new Class<?>[]{CanFrame.class}, frame);

    assertEquals("CanId: " + canId + " Data: " + Base64.getEncoder().encodeToString(data), rendered);
  }

  @Test
  void sendMessageIgnoresNullPayload() throws Exception {
    Fixture fixture = fixture();
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn(null);

    fixture.protocol.sendMessage(new MessageEvent("/out", null, message, null));

    verifyNoInteractions(fixture.formatter);
    verify(fixture.endPoint, never()).writeFrame(any());
  }

  @Test
  void sendMessageWritesRawCanFrameWithoutFormatter() throws Exception {
    Fixture fixture = fixture();
    CanFrame source = frame(127250, 23, 255, new byte[]{1, 2, 3, 4});
    byte[] raw = source.getRawData();
    assertEquals(13, raw.length);
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn(raw);

    fixture.protocol.sendMessage(new MessageEvent("/out", null, message, null));

    ArgumentCaptor<CanFrame> captor = ArgumentCaptor.forClass(CanFrame.class);
    verify(fixture.endPoint).writeFrame(captor.capture());
    assertEquals(source.canIdentifier(), captor.getValue().canIdentifier());
    assertArrayEquals(source.data(), captor.getValue().data());
    verifyNoInteractions(fixture.formatter);
  }

  @Test
  void sendMessageParsesJsonAndWritesCanFrame() throws Exception {
    Fixture fixture = fixture();
    CanFrame source = frame(127250, 24, 255, new byte[]{5, 6, 7});
    when(fixture.formatter.parseFromJson(any(JsonObject.class))).thenReturn(source.getRawData());
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn("{\"canId\":1}".getBytes(StandardCharsets.UTF_8));

    fixture.protocol.sendMessage(new MessageEvent("/out", null, message, null));

    verify(fixture.formatter).parseFromJson(any(JsonObject.class));
    ArgumentCaptor<CanFrame> captor = ArgumentCaptor.forClass(CanFrame.class);
    verify(fixture.endPoint).writeFrame(captor.capture());
    assertEquals(source.canIdentifier(), captor.getValue().canIdentifier());
    assertArrayEquals(source.data(), captor.getValue().data());
  }

  @Test
  void sendMessageDropsJsonWhenFormatterCannotBuildFrame() throws Exception {
    Fixture fixture = fixture();
    when(fixture.formatter.parseFromJson(any(JsonObject.class))).thenThrow(new IOException("bad frame"));
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn("{\"canId\":1}".getBytes(StandardCharsets.UTF_8));

    assertDoesNotThrow(() -> fixture.protocol.sendMessage(new MessageEvent("/out", null, message, null)));

    verify(fixture.endPoint, never()).writeFrame(any());
  }

  @Test
  void sendMessageSwallowsEndpointWriteFailureAfterSuccessfulDecode() throws Exception {
    Fixture fixture = fixture();
    CanFrame source = frame(127250, 24, 255, new byte[]{8, 9});
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn(source.getRawData());
    doThrow(new IOException("closed")).when(fixture.endPoint).writeFrame(any());

    assertDoesNotThrow(() -> fixture.protocol.sendMessage(new MessageEvent("/out", null, message, null)));

    verify(fixture.endPoint).writeFrame(any());
  }

  @Test
  void writePgnUsesSingleFrameForAddressClaim() throws Exception {
    Fixture fixture = fixture();
    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};

    fixture.protocol.writePgn(60928, 77, data);

    ArgumentCaptor<CanFrame> captor = ArgumentCaptor.forClass(CanFrame.class);
    verify(fixture.endPoint).writeFrame(captor.capture());
    verify(fixture.endPoint, never()).writeFrames(anyList());
    CanFrame frame = captor.getValue();
    assertArrayEquals(data, frame.data());
    CanId canId = CanId.parse(frame.canIdentifier());
    assertEquals(60928, canId.getPgn());
    assertEquals(77, canId.getDestinationAddress());
    assertEquals(42, canId.getSourceAddress());
  }

  @ParameterizedTest
  @CsvSource({
      "126996,10",
      "126998,11",
      "127250,12",
      "129025,255"
  })
  void writePgnUsesFastPacketPackerForOtherPgns(int pgn, int destination) throws Exception {
    Fixture fixture = fixture();
    byte[] data = {10, 11, 12};
    List<CanFrame> packed = List.of(frame(pgn, 42, destination, new byte[]{1}));
    when(fixture.framePacker.packFastPacket(eq(pgn), anyInt(), eq(42), eq(destination), same(data)))
        .thenReturn(packed);

    fixture.protocol.writePgn(pgn, destination, data);

    verify(fixture.framePacker).packFastPacket(eq(pgn), anyInt(), eq(42), eq(destination), same(data));
    verify(fixture.endPoint).writeFrames(same(packed));
    verify(fixture.endPoint, never()).writeFrame(any());
  }

  @Test
  void frameJsonParsingCanBeDisabled() throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, N2kProtocol.class, "parseToJson", false);

    boolean parsed = processFrameAsJson(fixture.protocol, frame(127250, 20, 255, new byte[]{1}));

    assertFalse(parsed);
    verifyNoInteractions(fixture.formatter);
  }

  @Test
  void knownUnknownCanIdentifierSkipsFormatter() throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, N2kProtocol.class, "parseToJson", true);
    CanFrame frame = frame(130000, 20, 255, new byte[]{1, 2});
    fixture.unknownCanIdentifiers.add(frame.canIdentifier());

    boolean parsed = processFrameAsJson(fixture.protocol, frame);

    assertFalse(parsed);
    verifyNoInteractions(fixture.formatter);
  }

  @Test
  void parseFailureCachesCanIdentifierAndAvoidsRepeatedFormatterWork() throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, N2kProtocol.class, "parseToJson", true);
    CanFrame frame = frame(130001, 21, 255, new byte[]{3, 4});
    when(fixture.formatter.parseToJson(any(byte[].class), any()))
        .thenThrow(mock(ParseException.class));

    assertFalse(processFrameAsJson(fixture.protocol, frame));
    assertTrue(fixture.unknownCanIdentifiers.contains(frame.canIdentifier()));
    assertFalse(processFrameAsJson(fixture.protocol, frame));

    verify(fixture.formatter, times(1)).parseToJson(any(byte[].class), any());
  }

  @Test
  void successfulFrameJsonParsingPublishesNamedTopic() throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, N2kProtocol.class, "parseToJson", true);
    CanFrame frame = frame(127250, 21, 255, new byte[]{3, 4});
    JsonObject json = jsonPacket(127250, "Heading");
    when(fixture.formatter.parseToJson(any(byte[].class), any())).thenReturn(json);

    boolean parsed = processFrameAsJson(fixture.protocol, frame);

    assertTrue(parsed);
    verify(fixture.session).findDestination("/can0/127250/Heading", DestinationType.TOPIC);
    verify(fixture.destination).storeMessage(any(Message.class));
  }

  @Test
  void processPacketReturnsWhenEndpointHasNoFrame() throws Exception {
    Fixture fixture = fixture();
    when(fixture.endPoint.readFrame()).thenReturn(null);

    assertTrue(fixture.protocol.processPacket(mock(Packet.class)));

    verify(fixture.session, never()).findDestination(anyString(), any());
  }

  @Test
  void processPacketPropagatesEndpointReadFailure() throws Exception {
    Fixture fixture = fixture();
    when(fixture.endPoint.readFrame()).thenThrow(new IOException("read failed"));

    assertThrows(IOException.class, () -> fixture.protocol.processPacket(mock(Packet.class)));
  }

  @Test
  void processPacketPublishesRawFrameWhenJsonParsingDisabled() throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, N2kProtocol.class, "parseToJson", false);
    CanFrame frame = frame(127250, 31, 255, new byte[]{1, 2, 3});
    when(fixture.endPoint.readFrame()).thenReturn(frame);

    assertTrue(fixture.protocol.processPacket(mock(Packet.class)));

    verify(fixture.session).findDestination("/raw/can0", DestinationType.TOPIC);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(fixture.destination).storeMessage(captor.capture());
    assertArrayEquals(frame.getRawData(), captor.getValue().getOpaqueData());
    assertEquals("canbus", captor.getValue().getContentType());
  }

  @Test
  void processPacketFallsBackToRawAfterJsonParseFailure() throws Exception {
    Fixture fixture = fixture();
    setField(fixture.protocol, N2kProtocol.class, "parseToJson", true);
    CanFrame frame = frame(130002, 32, 255, new byte[]{4, 5, 6});
    when(fixture.endPoint.readFrame()).thenReturn(frame);
    when(fixture.formatter.parseToJson(any(byte[].class), any()))
        .thenThrow(mock(ParseException.class));

    assertTrue(fixture.protocol.processPacket(mock(Packet.class)));

    verify(fixture.session).findDestination("/raw/can0", DestinationType.TOPIC);
    assertTrue(fixture.unknownCanIdentifiers.contains(frame.canIdentifier()));
  }

  @Test
  void jsonPacketWithoutJ1939UsesRawTopicAndEmptyDataMap() throws Exception {
    Fixture fixture = fixture();

    boolean processed = processJsonPacket(fixture.protocol, new JsonObject());

    assertTrue(processed);
    verify(fixture.session).findDestination("/raw/can0", DestinationType.TOPIC);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(fixture.destination).storeMessage(captor.capture());
    assertTrue(captor.getValue().getDataMap().isEmpty());
  }

  @Test
  void jsonPacketWithPgnButNoN2kNameUsesRawTopic() throws Exception {
    Fixture fixture = fixture();
    JsonObject json = new JsonObject();
    JsonObject j1939 = new JsonObject();
    j1939.addProperty("pgn", 127251);
    json.add("j1939", j1939);

    assertTrue(processJsonPacket(fixture.protocol, json));

    verify(fixture.session).findDestination("/raw/can0", DestinationType.TOPIC);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(fixture.destination).storeMessage(captor.capture());
    assertEquals(127251, captor.getValue().getDataMap().get("pgn").getData());
  }

  @Test
  void jsonPacketWithN2kNamePublishesTemplatedTopicAndDataMap() throws Exception {
    Fixture fixture = fixture();
    JsonObject json = jsonPacket(127250, "Vessel Heading");

    assertTrue(processJsonPacket(fixture.protocol, json));

    verify(fixture.session).findDestination("/can0/127250/Vessel Heading", DestinationType.TOPIC);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(fixture.destination).storeMessage(captor.capture());
    Message message = captor.getValue();
    assertEquals("canbus", message.getContentType());
    assertEquals(QualityOfService.AT_MOST_ONCE, message.getQualityOfService());
    assertTrue(message.isStoreOffline());
    assertFalse(message.isRetain());
    assertEquals("n2k", message.getMeta().get("protocol"));
    assertEquals("1.0", message.getMeta().get("version"));
    assertEquals("session-1", message.getMeta().get("sessionId"));
    assertEquals(127250, message.getDataMap().get("pgn").getData());
    assertEquals("Vessel Heading", message.getDataMap().get("name").getData());
    assertEquals("schema-1", message.getSchemaId());
  }

  @Test
  void rawFramePublishCarriesProtocolMetadata() throws Exception {
    Fixture fixture = fixture();
    CanFrame frame = frame(129025, 35, 255, new byte[]{9, 8, 7});

    invoke(fixture.protocol, "publishRawFrame", new Class<?>[]{CanFrame.class}, frame);

    verify(fixture.session).findDestination("/raw/can0", DestinationType.TOPIC);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(fixture.destination).storeMessage(captor.capture());
    Message message = captor.getValue();
    assertArrayEquals(frame.getRawData(), message.getOpaqueData());
    assertEquals("n2k", message.getMeta().get("protocol"));
    assertEquals("1.0", message.getMeta().get("version"));
    assertEquals("session-1", message.getMeta().get("sessionId"));
  }

  @Test
  void publishMessageDoesNothingWhenDestinationFutureIsNull() throws Exception {
    Fixture fixture = fixture();
    when(fixture.session.findDestination("/missing", DestinationType.TOPIC)).thenReturn(null);

    invoke(fixture.protocol, "publishMessage",
        new Class<?>[]{String.class, Message.class}, "/missing", mock(Message.class));

    verifyNoInteractions(fixture.destination);
  }

  @Test
  void publishMessageHandlesNullDestination() throws Exception {
    Fixture fixture = fixture();
    when(fixture.session.findDestination("/null", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(null));

    invoke(fixture.protocol, "publishMessage",
        new Class<?>[]{String.class, Message.class}, "/null", mock(Message.class));

    verifyNoInteractions(fixture.destination);
  }

  @Test
  void publishMessageSetsDefaultSchemaWhenDestinationHasNoSchema() throws Exception {
    Fixture fixture = fixture();
    Message message = mock(Message.class);
    when(fixture.destination.getSchema()).thenReturn(null);

    invoke(fixture.protocol, "publishMessage",
        new Class<?>[]{String.class, Message.class}, "/topic", message);

    verify(fixture.destination).updateSchema(fixture.defaultSchema, null);
    verify(fixture.destination).storeMessage(message);
  }

  @Test
  void publishMessageReplacesRawDestinationSchema() throws Exception {
    Fixture fixture = fixture();
    Schema schema = mock(Schema.class);
    when(schema.getUniqueId()).thenReturn(SchemaManager.DEFAULT_RAW_UUID.toString());
    when(fixture.destination.getSchema()).thenReturn(schema);
    Message message = mock(Message.class);

    invoke(fixture.protocol, "publishMessage",
        new Class<?>[]{String.class, Message.class}, "/topic", message);

    verify(fixture.destination).updateSchema(fixture.defaultSchema, null);
    verify(fixture.destination).storeMessage(message);
  }

  @Test
  void publishMessageKeepsExistingNonRawSchema() throws Exception {
    Fixture fixture = fixture();
    Schema schema = mock(Schema.class);
    when(schema.getUniqueId()).thenReturn("custom-schema");
    when(fixture.destination.getSchema()).thenReturn(schema);
    Message message = mock(Message.class);

    invoke(fixture.protocol, "publishMessage",
        new Class<?>[]{String.class, Message.class}, "/topic", message);

    verify(fixture.destination, never()).updateSchema(any(), any());
    verify(fixture.destination).storeMessage(message);
  }

  @ParameterizedTest
  @MethodSource("ignoredInboundRequests")
  void inboundRequestsThatDoNotNeedAResponseAreIgnored(int pgn, int source, int destination, byte[] payload)
      throws Exception {
    Fixture fixture = fixture();
    CanFrame frame = frame(pgn, source, destination, payload);

    invoke(fixture.protocol, "handleInboundRequest", new Class<?>[]{CanFrame.class}, frame);

    verify(fixture.endPoint, never()).writeFrame(any());
    verify(fixture.endPoint, never()).writeFrames(anyList());
  }

  private static Stream<Arguments> topicCases() {
    return Stream.of(
        Arguments.of("/{candevice}/{pgn}/{messageName}", 127250, "Heading", "/can0/127250/Heading"),
        Arguments.of("{candevice}:{pgn}:{messageName}", 129025, "Position", "can0:129025:Position"),
        Arguments.of("/{messageName}/{messageName}/{pgn}", 1, "X", "/X/X/1"),
        Arguments.of("/fixed", 127250, "Heading", "/fixed")
    );
  }

  private static Stream<Arguments> packetStringCases() {
    return Stream.of(
        Arguments.of(0x123, new byte[0]),
        Arguments.of(0x18F11223, new byte[]{1}),
        Arguments.of(0x18F11224, new byte[]{0, 1, 2, 3, 4, 5, 6, 7}),
        Arguments.of(0x18F11225, new byte[]{(byte) 0xFF, 0, 0x55})
    );
  }

  private static Stream<Arguments> ignoredInboundRequests() {
    return Stream.of(
        Arguments.of(127250, 30, 255, new byte[]{0, 0, 0}),
        Arguments.of(59904, 30, 12, new byte[]{0, 0, 0}),
        Arguments.of(59904, 30, 255, requestedPgn(130000)),
        Arguments.of(59904, 30, 42, requestedPgn(130001))
    );
  }

  private static byte[] requestedPgn(int pgn) {
    return new byte[]{
        (byte) (pgn & 0xFF),
        (byte) ((pgn >> 8) & 0xFF),
        (byte) ((pgn >> 16) & 0xFF)
    };
  }

  private static JsonObject jsonPacket(int pgn, String name) {
    JsonObject json = new JsonObject();
    JsonObject j1939 = new JsonObject();
    j1939.addProperty("pgn", pgn);
    JsonObject n2k = new JsonObject();
    n2k.addProperty("name", name);
    j1939.add("n2k", n2k);
    json.add("j1939", j1939);
    return json;
  }

  private static CanFrame frame(int pgn, int source, int destination, byte[] data) {
    int canId = CanIdBuilder.build(pgn, 6, source, destination);
    return new CanFrame(canId, true, data.length, data);
  }

  private static String computeTopicName(N2kProtocol protocol, int pgn, String name) throws Exception {
    return invoke(protocol, "computeTopicName",
        new Class<?>[]{int.class, String.class}, pgn, name);
  }

  private static boolean processFrameAsJson(N2kProtocol protocol, CanFrame frame) throws Exception {
    return invoke(protocol, "processFrameAsJson", new Class<?>[]{CanFrame.class}, frame);
  }

  private static boolean processJsonPacket(N2kProtocol protocol, JsonObject json) throws Exception {
    return invoke(protocol, "processPacket", new Class<?>[]{JsonObject.class}, json);
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(Object target, String name, Class<?>[] parameterTypes, Object... arguments)
      throws Exception {
    Method method = N2kProtocol.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(target, arguments);
  }

  private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Fixture fixture() throws Exception {
    N2kProtocol protocol = mock(N2kProtocol.class, CALLS_REAL_METHODS);
    CanbusEndPoint endPoint = mock(CanbusEndPoint.class);
    Session session = mock(Session.class);
    MessageFormatter formatter = mock(MessageFormatter.class);
    FramePacker framePacker = mock(FramePacker.class);
    SchemaConfig defaultSchema = mock(SchemaConfig.class);
    Destination destination = mock(Destination.class);
    Logger logger = mock(Logger.class);
    Set<Integer> unknownCanIdentifiers = ConcurrentHashMap.newKeySet();

    when(endPoint.getName()).thenReturn("can0");
    when(session.getName()).thenReturn("session-1");
    var sessionInformation = mock(io.mapsmessaging.dto.rest.session.SessionInformationDTO.class);
    when(session.getSessionInformation()).thenReturn(sessionInformation);
    when(defaultSchema.getUniqueId()).thenReturn("schema-1");
    when(session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenReturn(CompletableFuture.completedFuture(destination));
    when(destination.getSchema()).thenReturn(null);

    setField(protocol, N2kProtocol.class, "logger", logger);
    setField(protocol, N2kProtocol.class, "unknownCanIdentifiers", unknownCanIdentifiers);
    setField(protocol, N2kProtocol.class, "formatter", formatter);
    setField(protocol, N2kProtocol.class, "framePacker", framePacker);
    setField(protocol, N2kProtocol.class, "session", session);
    setField(protocol, N2kProtocol.class, "topicTemplate", "/{candevice}/{pgn}/{messageName}");
    setField(protocol, N2kProtocol.class, "rawTopicTemplate", "/raw/can0");
    setField(protocol, N2kProtocol.class, "parseToJson", true);
    setField(protocol, N2kProtocol.class, "defaultSchemaConfig", defaultSchema);
    setField(protocol, N2kProtocol.class, "canbusAddress", 42);
    setField(protocol, N2kProtocol.class, "qos", QualityOfService.AT_MOST_ONCE);
    setField(protocol, N2kProtocol.class, "storeOffline", true);

    setField(protocol, Protocol.class, "endPoint", endPoint);
    setField(protocol, Protocol.class, "destinationTransformerMap", new ConcurrentHashMap<>());
    setField(protocol, Protocol.class, "topicNameMapping", new ConcurrentHashMap<>());
    setField(protocol, Protocol.class, "topicNameAnalyserMap", new ConcurrentHashMap<>());
    setField(protocol, Protocol.class, "resourceNameAnalyserMap", new ConcurrentHashMap<>());
    setField(protocol, Protocol.class, "parserLookup", new ConcurrentHashMap<>());

    return new Fixture(
        protocol,
        endPoint,
        session,
        formatter,
        framePacker,
        defaultSchema,
        destination,
        unknownCanIdentifiers,
        sessionInformation);
  }

  private static final class Fixture {
    private final N2kProtocol protocol;
    private final CanbusEndPoint endPoint;
    private final Session session;
    private final MessageFormatter formatter;
    private final FramePacker framePacker;
    private final SchemaConfig defaultSchema;
    private final Destination destination;
    private final Set<Integer> unknownCanIdentifiers;
    private final io.mapsmessaging.dto.rest.session.SessionInformationDTO sessionInformation;

    private Fixture(
        N2kProtocol protocol,
        CanbusEndPoint endPoint,
        Session session,
        MessageFormatter formatter,
        FramePacker framePacker,
        SchemaConfig defaultSchema,
        Destination destination,
        Set<Integer> unknownCanIdentifiers,
        io.mapsmessaging.dto.rest.session.SessionInformationDTO sessionInformation) {
      this.protocol = protocol;
      this.endPoint = endPoint;
      this.session = session;
      this.formatter = formatter;
      this.framePacker = framePacker;
      this.defaultSchema = defaultSchema;
      this.destination = destination;
      this.unknownCanIdentifiers = unknownCanIdentifiers;
      this.sessionInformation = sessionInformation;
    }
  }
}
