package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.GenericOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CoapRfc7252WireComplianceTest {

  private final PacketFactory factory = new PacketFactory();

  @ParameterizedTest
  @ValueSource(ints = {0, 2, 3})
  void onlyVersionOneIsAccepted(int version) {
    byte first = (byte) ((version << 6) | (TYPE.CON.getValue() << 4));
    byte[] wire = new byte[]{first, Code.EMPTY.getValue(), 0x12, 0x34};

    assertThrows(
        IOException.class,
        () -> parse(wire),
        "RFC 7252 messages for this implementation must use CoAP version 1");
  }

  @ParameterizedTest
  @ValueSource(ints = {9, 10, 11, 12, 13, 14, 15})
  void tokenLengthsNineThroughFifteenAreFormatErrors(int tokenLength) {
    byte first = (byte) ((1 << 6) | (TYPE.CON.getValue() << 4) | tokenLength);
    byte[] wire = new byte[4 + tokenLength];
    wire[0] = first;
    wire[1] = Code.EMPTY.getValue();
    wire[2] = 0x01;
    wire[3] = 0x02;

    assertThrows(
        IOException.class,
        () -> parse(wire),
        "RFC 7252 reserves TKL 9..15 and requires them to be treated as format errors");
  }

  @ParameterizedTest
  @MethodSource("responseCodes")
  void responseCodesAreNeverDispatchedAsRequestPacketClasses(
      Code code, Class<?> forbiddenRequestClass) throws Exception {
    byte[] wire = new byte[]{
        (byte) ((1 << 6) | (TYPE.ACK.getValue() << 4)),
        code.getValue(),
        0x01,
        0x02
    };

    BasePacket packet = parse(wire);

    assertEquals(code, packet.getCode());
    assertFalse(
        forbiddenRequestClass.isInstance(packet),
        "Response code class must be considered before the five-bit detail when selecting packet type");
  }

  @Test
  void emptyPayloadMarkerWithoutPayloadIsFormatError() {
    byte[] wire = new byte[]{
        (byte) ((1 << 6) | (TYPE.NON.getValue() << 4)),
        Code.CONTENT.getValue(),
        0x01,
        0x02,
        (byte) 0xff
    };

    assertThrows(
        IOException.class,
        () -> parse(wire),
        "RFC 7252 forbids a payload marker followed by a zero-length payload");
  }

  @ParameterizedTest
  @MethodSource("optionDeltaBoundaries")
  void optionDeltaEncodingMatchesRfc7252(int optionId, byte[] expectedPrefix)
      throws Exception {
    BasePacket packet = packet();
    GenericOption option = new GenericOption(optionId);
    option.update(new byte[]{0x55});
    packet.getOptions().putOption(option);

    byte[] encoded = encode(packet);
    byte[] actualPrefix = Arrays.copyOfRange(
        encoded,
        4,
        Math.min(encoded.length, 4 + expectedPrefix.length));

    assertArrayEquals(
        expectedPrefix,
        actualPrefix,
        "RFC 7252 option delta encoding must use nibble 13 for 13..268 and nibble 14 for >=269");
  }

  @ParameterizedTest
  @MethodSource("optionLengthBoundaries")
  void optionLengthEncodingMatchesRfc7252(int optionLength, byte[] expectedPrefix)
      throws Exception {
    BasePacket packet = packet();
    GenericOption option = new GenericOption(1);
    option.update(new byte[optionLength]);
    packet.getOptions().putOption(option);

    byte[] encoded = encode(packet);
    byte[] actualPrefix = Arrays.copyOfRange(
        encoded,
        4,
        Math.min(encoded.length, 4 + expectedPrefix.length));

    assertArrayEquals(
        expectedPrefix,
        actualPrefix,
        "RFC 7252 option length encoding must use the option-length nibble and correct extension offset");
  }

  @ParameterizedTest
  @MethodSource("blockValues")
  void blockOptionRoundTripsValidUdpValues(int number, boolean more, int szx)
      throws Exception {
    Block source = new Block(23, number, more, szx);

    byte[] encoded = source.pack();
    Block decoded = new Block(23);
    decoded.update(encoded);

    assertEquals(number, decoded.getNumber());
    assertEquals(more, decoded.isMore());
    assertEquals(szx, decoded.getSizeEx());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 15, 16, 255, 4095, 4096, 0xfffff})
  void blockOptionSupportsLegalBlockNumbers(int number) throws Exception {
    Block source = new Block(23, number, true, 6);

    Block decoded = new Block(23);
    decoded.update(source.pack());

    assertEquals(number, decoded.getNumber());
    assertTrue(decoded.isMore());
    assertEquals(6, decoded.getSizeEx());
  }

  @Test
  void bertSizeExponentIsRejectedForUdpCoapBlockwise() {
    Block block = new Block(23);

    assertThrows(
        IOException.class,
        () -> block.update(new byte[]{0x07}),
        "SZX=7 is BERT and is not valid for RFC 7959 UDP blockwise transfers");
  }

  private BasePacket parse(byte[] wire) throws Exception {
    return factory.parseFrame(new Packet(ByteBuffer.wrap(wire)));
  }

  private BasePacket packet() {
    return new BasePacket(
        PacketFactory.GET,
        TYPE.NON,
        Code.CONTENT,
        1,
        0x1234,
        new byte[0]);
  }

  private byte[] encode(BasePacket packet) {
    Packet out = new Packet(4096, false);
    packet.packFrame(out);
    out.flip();
    byte[] data = new byte[out.available()];
    out.get(data);
    return data;
  }

  private static Stream<Arguments> responseCodes() {
    return Stream.of(
        Arguments.of(Code.CREATED, Get.class),
        Arguments.of(Code.DELETED, Delete.class),
        Arguments.of(Code.VALID, Put.class),
        Arguments.of(Code.CHANGED, Delete.class),
        Arguments.of(Code.CONTENT, Fetch.class),
        Arguments.of(Code.BAD_REQUEST, Empty.class),
        Arguments.of(Code.NOT_FOUND, Delete.class),
        Arguments.of(Code.METHOD_NOT_ALLOWED, Fetch.class),
        Arguments.of(Code.INTERNAL_SERVER_ERROR, Empty.class),
        Arguments.of(Code.NOT_IMPLEMENTED, Get.class),
        Arguments.of(Code.BAD_GATEWAY, Post.class),
        Arguments.of(Code.SERVICE_UNAVAILABLE, Put.class),
        Arguments.of(Code.GATEWAY_TIMEOUT, Delete.class),
        Arguments.of(Code.PROXYING_NOT_SUPPORTED, Fetch.class)
    );
  }

  private static Stream<Arguments> optionDeltaBoundaries() {
    return Stream.of(
        Arguments.of(1, new byte[]{0x11}),
        Arguments.of(12, new byte[]{(byte) 0xc1}),
        Arguments.of(13, new byte[]{(byte) 0xd1, 0x00}),
        Arguments.of(268, new byte[]{(byte) 0xd1, (byte) 0xff}),
        Arguments.of(269, new byte[]{(byte) 0xe1, 0x00, 0x00}),
        Arguments.of(270, new byte[]{(byte) 0xe1, 0x00, 0x01}),
        Arguments.of(1024, new byte[]{(byte) 0xe1, 0x02, (byte) 0xf3}));
  }

  private static Stream<Arguments> optionLengthBoundaries() {
    return Stream.of(
        Arguments.of(0, new byte[]{0x10}),
        Arguments.of(1, new byte[]{0x11}),
        Arguments.of(12, new byte[]{0x1c}),
        Arguments.of(13, new byte[]{0x1d, 0x00}),
        Arguments.of(268, new byte[]{0x1d, (byte) 0xff}),
        Arguments.of(269, new byte[]{0x1e, 0x00, 0x00}),
        Arguments.of(270, new byte[]{0x1e, 0x00, 0x01}));
  }

  private static Stream<Arguments> blockValues() {
    return Stream.of(
        Arguments.of(0, false, 0),
        Arguments.of(0, true, 0),
        Arguments.of(1, false, 1),
        Arguments.of(15, true, 2),
        Arguments.of(16, false, 3),
        Arguments.of(255, true, 4),
        Arguments.of(4095, false, 5),
        Arguments.of(4096, true, 6),
        Arguments.of(0xfffff, false, 6));
  }
}
