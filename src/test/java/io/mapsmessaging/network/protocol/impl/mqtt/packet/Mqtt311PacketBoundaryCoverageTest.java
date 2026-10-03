/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class Mqtt311PacketBoundaryCoverageTest {

  @ParameterizedTest
  @MethodSource("variableIntegers")
  void variableIntegerRoundTrips(long value) throws Exception {
    Packet packet = packet(16);
    MQTTPacket.writeVariableInt(packet, value);
    packet.flip();

    assertEquals(value, MQTTPacket.readVariableInt(packet));
    assertEquals(0, packet.available());
  }

  @Test
  void truncatedVariableIntegerThrowsEndOfBuffer() {
    Packet packet = wrapped((byte) 0x80);

    assertThrows(EndOfBufferException.class, () -> MQTTPacket.readVariableInt(packet));
  }

  @ParameterizedTest
  @MethodSource("utf8Strings")
  void utf8StringRoundTrips(String value) throws Exception {
    Packet packet = packet(1024);
    MQTTPacket.writeUTF8(packet, value);
    packet.flip();

    assertEquals(value, MQTTPacket.readUTF8(packet));
    assertEquals(0, packet.available());
  }

  @ParameterizedTest
  @ValueSource(strings = {"\u0000", "a\u0000b", "\u0000tail"})
  void utf8StringRejectsNullCharacter(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    Packet packet = packet(bytes.length + 2);
    MQTTPacket.writeShort(packet, bytes.length);
    packet.put(bytes);
    packet.flip();

    assertThrows(MalformedException.class, () -> MQTTPacket.readUTF8(packet));
  }

  @ParameterizedTest
  @ValueSource(strings = {"\u0000", "prefix\u0000", "a\u0000b"})
  void remainingStringRejectsNullCharacter(String value) {
    Packet packet = wrapped(value.getBytes(StandardCharsets.UTF_8));

    assertThrows(MalformedException.class, () -> MQTTPacket.readRemainingString(packet));
  }

  @ParameterizedTest
  @MethodSource("shortValues")
  void unsignedShortRoundTrips(int value) {
    Packet packet = packet(2);
    MQTTPacket.writeShort(packet, value);
    packet.flip();

    assertEquals(value, MQTTPacket.readShort(packet));
  }

  @ParameterizedTest
  @MethodSource("intValues")
  void unsignedIntRoundTrips(long value) {
    Packet packet = packet(4);
    MQTTPacket.writeInt(packet, value);
    packet.flip();

    assertEquals(value, MQTTPacket.readInt(packet));
  }

  @ParameterizedTest
  @MethodSource("bufferPayloads")
  void lengthPrefixedBufferRoundTrips(byte[] payload) {
    Packet packet = packet(payload.length + 2);
    MQTTPacket.writeBuffer(payload, packet);
    packet.flip();

    assertArrayEquals(payload, MQTTPacket.readBuffer(packet));
    assertEquals(0, packet.available());
  }

  @Test
  void completionCallbackRunsAtMostOnce() {
    AtomicInteger calls = new AtomicInteger();
    PingReq packet = new PingReq();
    packet.setCallback(calls::incrementAndGet);

    packet.complete();
    packet.complete();

    assertEquals(1, calls.get());
    assertNull(packet.getCallback());
  }

  @ParameterizedTest
  @MethodSource("connAckCases")
  void connAckRoundTripsResponseAndSessionPresent(
      byte responseCode,
      boolean sessionPresent) throws Exception {
    ConnAck source = new ConnAck();
    source.setResponseCode(responseCode);
    source.setRestoredFlag(sessionPresent);

    Packet encoded = encode(source, 16);
    assertEquals((byte) (MQTTPacket.CONNACK << 4), encoded.get());
    assertEquals(2, MQTTPacket.readVariableInt(encoded));

    ConnAck decoded = new ConnAck(encoded);
    assertEquals(responseCode, decoded.getResponseCode());
    assertEquals(sessionPresent, decoded.isSessionPresent());
  }

  @ParameterizedTest
  @MethodSource("publishCases")
  void publishRoundTripsHeaderTopicPacketIdAndPayload(
      QualityOfService qos,
      boolean retain,
      int payloadSize) throws Exception {
    int packetId = qos.isSendPacketId() ? 0x1234 : 0;
    byte[] payload = sequence(payloadSize);
    Publish source = new Publish(retain, payload, qos, packetId, "/sensor/value");

    Packet encoded = encode(source, 4096);
    byte fixedHeader = encoded.get();
    long remaining = MQTTPacket.readVariableInt(encoded);
    Publish decoded = new Publish(fixedHeader, remaining, encoded, 0);

    assertEquals(retain, decoded.isRetain());
    assertFalse(decoded.isDuplicate());
    assertEquals(qos, decoded.getQos());
    assertEquals(packetId, decoded.getPacketId());
    assertEquals("/sensor/value", decoded.getDestinationName());
    assertArrayEquals(payload, decoded.getPayload());
  }

  @ParameterizedTest
  @MethodSource("publishMaximumBufferCases")
  void publishEnforcesConfiguredPayloadMaximum(
      int payloadSize,
      long maximum,
      boolean expectedAccepted) throws Exception {
    Publish source = new Publish(
        false,
        sequence(payloadSize),
        QualityOfService.AT_MOST_ONCE,
        0,
        "/bounded");

    Packet encoded = encode(source, 4096);
    byte fixedHeader = encoded.get();
    long remaining = MQTTPacket.readVariableInt(encoded);

    if (expectedAccepted) {
      Publish decoded = new Publish(fixedHeader, remaining, encoded, maximum);
      assertEquals(payloadSize, decoded.getPayload().length);
    } else {
      assertThrows(
          MalformedException.class,
          () -> new Publish(fixedHeader, remaining, encoded, maximum));
    }
  }

  @ParameterizedTest
  @MethodSource("invalidPublishTopics")
  void publishRejectsInvalidTopicNames(String topic) throws Exception {
    byte[] wire = publishWire(
        (byte) (MQTTPacket.PUBLISH << 4),
        topic,
        null,
        new byte[]{1});

    Packet packet = wrapped(wire);
    byte fixedHeader = packet.get();
    long remaining = MQTTPacket.readVariableInt(packet);

    assertThrows(
        MalformedException.class,
        () -> new Publish(fixedHeader, remaining, packet, 0));
  }

  @Test
  void qosZeroPublishRejectsDuplicateFlag() throws Exception {
    byte[] wire = publishWire(
        (byte) ((MQTTPacket.PUBLISH << 4) | 0b1000),
        "/topic",
        null,
        new byte[]{1});

    Packet packet = wrapped(wire);
    byte fixedHeader = packet.get();
    long remaining = MQTTPacket.readVariableInt(packet);

    assertThrows(
        MalformedException.class,
        () -> new Publish(fixedHeader, remaining, packet, 0));
  }

  @Test
  void publishRejectsReservedQosValue() throws Exception {
    byte[] wire = publishWire(
        (byte) ((MQTTPacket.PUBLISH << 4) | 0b0110),
        "/topic",
        7,
        new byte[]{1});

    Packet packet = wrapped(wire);
    byte fixedHeader = packet.get();
    long remaining = MQTTPacket.readVariableInt(packet);

    assertThrows(
        MalformedException.class,
        () -> new Publish(fixedHeader, remaining, packet, 0));
  }

  @ParameterizedTest
  @MethodSource("subscriptionCases")
  void subscribeRoundTripsMultipleTopicFilters(
      int packetId,
      List<SubscriptionInfo> subscriptions) throws Exception {
    Subscribe source = new Subscribe();
    source.setMessageId(packetId);
    source.getSubscriptionList().addAll(subscriptions);

    Packet encoded = encode(source, 4096);
    byte fixedHeader = encoded.get();
    long remaining = MQTTPacket.readVariableInt(encoded);
    Subscribe decoded = new Subscribe(fixedHeader, remaining, encoded);

    assertEquals(packetId, decoded.getMessageId());
    assertEquals(subscriptions.size(), decoded.getSubscriptionList().size());
    for (int i = 0; i < subscriptions.size(); i++) {
      assertEquals(
          subscriptions.get(i).getTopicName(),
          decoded.getSubscriptionList().get(i).getTopicName());
      assertEquals(
          subscriptions.get(i).getQualityOfService(),
          decoded.getSubscriptionList().get(i).getQualityOfService());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3, 4, 8, 15})
  void subscribeRejectsInvalidFixedHeaderNibble(int nibble) {
    Packet body = packet(16);
    MQTTPacket.writeShort(body, 1);
    MQTTPacket.writeUTF8(body, "/a");
    body.put((byte) 0);
    body.flip();

    assertThrows(
        MalformedException.class,
        () -> new Subscribe(
            (byte) ((MQTTPacket.SUBSCRIBE << 4) | nibble),
            6,
            body));
  }

  @Test
  void subscribeRejectsEmptyPayload() {
    Packet body = packet(2);
    MQTTPacket.writeShort(body, 1);
    body.flip();

    assertThrows(
        MalformedException.class,
        () -> new Subscribe((byte) 0x82, 2, body));
  }

  @Test
  void subscribeRejectsQosThree() {
    Packet body = packet(16);
    MQTTPacket.writeShort(body, 1);
    MQTTPacket.writeUTF8(body, "/a");
    body.put((byte) 3);
    body.flip();

    assertThrows(
        MalformedException.class,
        () -> new Subscribe((byte) 0x82, 6, body));
  }

  @ParameterizedTest
  @MethodSource("unsubscribeCases")
  void unsubscribeRoundTripsTopicFilters(
      int packetId,
      List<String> topics) throws Exception {
    Unsubscribe source = new Unsubscribe(topics);
    source.setMessageId(packetId);

    Packet encoded = encode(source, 4096);
    byte fixedHeader = encoded.get();
    long remaining = MQTTPacket.readVariableInt(encoded);
    Unsubscribe decoded = new Unsubscribe(fixedHeader, remaining, encoded);

    assertEquals(packetId, decoded.getPacketId());
    assertEquals(topics, decoded.getUnsubscribeList());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3, 4, 8, 15})
  void unsubscribeRejectsInvalidFixedHeaderNibble(int nibble) {
    Packet body = packet(16);
    MQTTPacket.writeShort(body, 1);
    MQTTPacket.writeUTF8(body, "/a");
    body.flip();

    assertThrows(
        MalformedException.class,
        () -> new Unsubscribe(
            (byte) ((MQTTPacket.UNSUBSCRIBE << 4) | nibble),
            5,
            body));
  }

  @Test
  void unsubscribeRejectsEmptyPayload() {
    Packet body = packet(2);
    MQTTPacket.writeShort(body, 1);
    body.flip();

    assertThrows(
        MalformedException.class,
        () -> new Unsubscribe((byte) 0xa2, 2, body));
  }

  @ParameterizedTest
  @MethodSource("publishMonitorCases")
  void publishMonitorPacketsRoundTrip(
      int packetType,
      int packetId) throws Exception {
    PublishMonitorPacket source = monitorPacket(packetType, packetId);
    Packet encoded = encode(source, 16);
    byte fixedHeader = encoded.get();
    long remaining = MQTTPacket.readVariableInt(encoded);

    PublishMonitorPacket decoded = parseMonitor(packetType, fixedHeader, remaining, encoded);
    assertEquals(packetId, decoded.getPacketIdentifier());
  }

  @ParameterizedTest
  @MethodSource("invalidMonitorHeaders")
  void publishMonitorPacketsRejectInvalidReservedBits(
      int packetType,
      int nibble) {
    Packet body = packet(2);
    MQTTPacket.writeShort(body, 1);
    body.flip();

    assertThrows(
        MalformedException.class,
        () -> parseMonitor(
            packetType,
            (byte) ((packetType << 4) | nibble),
            2,
            body));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3, 4, 127})
  void pubAckRejectsInvalidRemainingLength(int remaining) {
    Packet body = wrapped((byte) 0, (byte) 1);

    assertThrows(
        MalformedException.class,
        () -> new PubAck((byte) 0x40, remaining, body));
  }

  @ParameterizedTest
  @MethodSource("subAckCases")
  void subAckRoundTrips(int packetId, byte[] results) throws Exception {
    SubAck source = new SubAck(packetId, results);

    Packet encoded = encode(source, 64);
    byte fixedHeader = encoded.get();
    long remaining = MQTTPacket.readVariableInt(encoded);
    SubAck decoded = new SubAck(fixedHeader, remaining, encoded);

    assertEquals(packetId, decoded.getPacketId());
    assertArrayEquals(results, decoded.getResult());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 15})
  void subAckRejectsReservedFixedHeaderBits(int nibble) {
    Packet body = wrapped((byte) 0, (byte) 1, (byte) 0);

    assertThrows(
        MalformedException.class,
        () -> new SubAck(
            (byte) ((MQTTPacket.SUBACK << 4) | nibble),
            3,
            body));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2})
  void subAckRejectsTooShortRemainingLength(int remaining) {
    Packet body = wrapped((byte) 0, (byte) 1, (byte) 0);

    assertThrows(
        MalformedException.class,
        () -> new SubAck((byte) 0x90, remaining, body));
  }

  private static Packet packet(int capacity) {
    return new Packet(capacity, false);
  }

  private static Packet wrapped(byte... bytes) {
    return new Packet(ByteBuffer.wrap(bytes));
  }

  private static Packet encode(MQTTPacket source, int capacity) {
    Packet packet = packet(capacity);
    source.packFrame(packet);
    packet.flip();
    return packet;
  }

  private static byte[] publishWire(
      byte fixedHeader,
      String topic,
      Integer packetId,
      byte[] payload) {
    Packet body = packet(4096);
    MQTTPacket.writeUTF8(body, topic);
    if (packetId != null) {
      MQTTPacket.writeShort(body, packetId);
    }
    body.put(payload);
    int remaining = body.position();
    body.flip();

    Packet frame = packet(4096);
    frame.put(fixedHeader);
    MQTTPacket.writeVariableInt(frame, remaining);
    byte[] remainingBytes = new byte[remaining];
    body.get(remainingBytes);
    frame.put(remainingBytes);
    frame.flip();

    byte[] wire = new byte[frame.available()];
    frame.get(wire);
    return wire;
  }

  private static PublishMonitorPacket monitorPacket(int packetType, int packetId) {
    return switch (packetType) {
      case MQTTPacket.PUBACK -> new PubAck(packetId);
      case MQTTPacket.PUBREC -> new PubRec(packetId);
      case MQTTPacket.PUBREL -> new PubRel(packetId);
      case MQTTPacket.PUBCOMP -> new PubComp(packetId);
      default -> throw new IllegalArgumentException("Unsupported packet type " + packetType);
    };
  }

  private static PublishMonitorPacket parseMonitor(
      int packetType,
      byte fixedHeader,
      long remaining,
      Packet packet) throws MalformedException {
    return switch (packetType) {
      case MQTTPacket.PUBACK -> new PubAck(fixedHeader, remaining, packet);
      case MQTTPacket.PUBREC -> new PubRec(fixedHeader, remaining, packet);
      case MQTTPacket.PUBREL -> new PubRel(fixedHeader, remaining, packet);
      case MQTTPacket.PUBCOMP -> new PubComp(fixedHeader, remaining, packet);
      default -> throw new IllegalArgumentException("Unsupported packet type " + packetType);
    };
  }

  private static byte[] sequence(int size) {
    byte[] payload = new byte[size];
    for (int i = 0; i < size; i++) {
      payload[i] = (byte) (i & 0xff);
    }
    return payload;
  }

  private static Stream<Arguments> variableIntegers() {
    return Stream.of(
        Arguments.of(0L),
        Arguments.of(1L),
        Arguments.of(2L),
        Arguments.of(126L),
        Arguments.of(127L),
        Arguments.of(128L),
        Arguments.of(16_383L),
        Arguments.of(16_384L),
        Arguments.of(2_097_151L),
        Arguments.of(2_097_152L),
        Arguments.of(268_435_455L));
  }

  private static Stream<Arguments> utf8Strings() {
    return Stream.of(
        Arguments.of(""),
        Arguments.of("a"),
        Arguments.of("sensor/temp"),
        Arguments.of("/root/topic"),
        Arguments.of("topic with spaces"),
        Arguments.of("$schema/value"),
        Arguments.of("Español"),
        Arguments.of("東京"),
        Arguments.of("emoji-🙂"),
        Arguments.of("x".repeat(255)));
  }

  private static Stream<Arguments> shortValues() {
    return Stream.of(
        Arguments.of(0),
        Arguments.of(1),
        Arguments.of(127),
        Arguments.of(255),
        Arguments.of(256),
        Arguments.of(32_767),
        Arguments.of(32_768),
        Arguments.of(65_535));
  }

  private static Stream<Arguments> intValues() {
    return Stream.of(
        Arguments.of(0L),
        Arguments.of(1L),
        Arguments.of(127L),
        Arguments.of(255L),
        Arguments.of(65_535L),
        Arguments.of(2_147_483_647L),
        Arguments.of(4_294_967_295L));
  }

  private static Stream<Arguments> bufferPayloads() {
    return Stream.of(
        Arguments.of(new byte[0]),
        Arguments.of(new byte[]{0}),
        Arguments.of(new byte[]{1, 2}),
        Arguments.of(sequence(16)),
        Arguments.of(sequence(127)),
        Arguments.of(sequence(255)));
  }

  private static Stream<Arguments> connAckCases() {
    List<Arguments> result = new ArrayList<>();
    for (byte response = ConnAck.SUCCESS; response <= ConnAck.NOT_AUTHORISED; response++) {
      result.add(Arguments.of(response, false));
      result.add(Arguments.of(response, true));
    }
    return result.stream();
  }

  private static Stream<Arguments> publishCases() {
    List<Arguments> result = new ArrayList<>();
    for (QualityOfService qos : List.of(
        QualityOfService.AT_MOST_ONCE,
        QualityOfService.AT_LEAST_ONCE,
        QualityOfService.EXACTLY_ONCE)) {
      for (boolean retain : List.of(false, true)) {
        for (int payloadSize : List.of(0, 1, 16, 255)) {
          result.add(Arguments.of(qos, retain, payloadSize));
        }
      }
    }
    return result.stream();
  }

  private static Stream<Arguments> publishMaximumBufferCases() {
    return Stream.of(
        Arguments.of(0, 1L, true),
        Arguments.of(1, 1L, true),
        Arguments.of(2, 1L, false),
        Arguments.of(16, 16L, true),
        Arguments.of(17, 16L, false),
        Arguments.of(255, 0L, true));
  }

  private static Stream<Arguments> invalidPublishTopics() {
    return Stream.of(
        Arguments.of(""),
        Arguments.of("sensor/+"),
        Arguments.of("sensor/#"),
        Arguments.of("a//b"),
        Arguments.of("a/../b"),
        Arguments.of("$SYS/status"),
        Arguments.of("bad\u0001topic"));
  }

  private static Stream<Arguments> subscriptionCases() {
    return Stream.of(
        Arguments.of(
            1,
            List.of(new SubscriptionInfo("/a", QualityOfService.AT_MOST_ONCE))),
        Arguments.of(
            2,
            List.of(new SubscriptionInfo("/a", QualityOfService.AT_LEAST_ONCE))),
        Arguments.of(
            3,
            List.of(new SubscriptionInfo("/a", QualityOfService.EXACTLY_ONCE))),
        Arguments.of(
            255,
            List.of(
                new SubscriptionInfo("/a", QualityOfService.AT_MOST_ONCE),
                new SubscriptionInfo("/b", QualityOfService.AT_LEAST_ONCE))),
        Arguments.of(
            65_535,
            List.of(
                new SubscriptionInfo("sensor/one", QualityOfService.AT_MOST_ONCE),
                new SubscriptionInfo("sensor/two", QualityOfService.AT_LEAST_ONCE),
                new SubscriptionInfo("sensor/three", QualityOfService.EXACTLY_ONCE))));
  }

  private static Stream<Arguments> unsubscribeCases() {
    return Stream.of(
        Arguments.of(1, List.of("/a")),
        Arguments.of(2, List.of("sensor/temp")),
        Arguments.of(255, List.of("a", "b")),
        Arguments.of(256, List.of("/a", "/b", "/c")),
        Arguments.of(65_535, List.of("$schema/test")));
  }

  private static Stream<Arguments> publishMonitorCases() {
    List<Arguments> result = new ArrayList<>();
    for (int packetType : List.of(
        MQTTPacket.PUBACK,
        MQTTPacket.PUBREC,
        MQTTPacket.PUBREL,
        MQTTPacket.PUBCOMP)) {
      for (int packetId : List.of(1, 255, 256, 32_767, 65_535)) {
        result.add(Arguments.of(packetType, packetId));
      }
    }
    return result.stream();
  }

  private static Stream<Arguments> invalidMonitorHeaders() {
    List<Arguments> result = new ArrayList<>();
    for (int packetType : List.of(
        MQTTPacket.PUBACK,
        MQTTPacket.PUBREC,
        MQTTPacket.PUBCOMP)) {
      for (int nibble : List.of(1, 2, 8, 15)) {
        result.add(Arguments.of(packetType, nibble));
      }
    }
    for (int nibble : List.of(0, 1, 3, 8, 15)) {
      result.add(Arguments.of(MQTTPacket.PUBREL, nibble));
    }
    return result.stream();
  }

  private static Stream<Arguments> subAckCases() {
    return Stream.of(
        Arguments.of(1, new byte[]{0}),
        Arguments.of(2, new byte[]{1}),
        Arguments.of(3, new byte[]{2}),
        Arguments.of(255, new byte[]{0, 1, 2}),
        Arguments.of(65_535, new byte[]{0, (byte) 0x80}));
  }
}
