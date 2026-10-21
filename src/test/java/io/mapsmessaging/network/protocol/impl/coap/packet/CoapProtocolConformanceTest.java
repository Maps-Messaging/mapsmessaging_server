package io.mapsmessaging.network.protocol.impl.coap.packet;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("conformance")
@Tag("conformance-core")
@Tag("coap")
class CoapProtocolConformanceTest {

  private static final String RFC7252 = "CoAP RFC 7252";
  private static final String RFC7641 = "CoAP Observe RFC 7641";
  private static final String RFC7959 = "CoAP Blockwise RFC 7959";
  private static final PacketFactory FACTORY = new PacketFactory();

  @Test
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 §3: Version MUST be 1 and TKL values 0..8 are valid",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void versionOneWithEightByteTokenParses() throws Exception {
    byte[] wire = new byte[] {
        (byte) 0x48, 0x01, 0x12, 0x34,
        1,2,3,4,5,6,7,8
    };
    BasePacket packet = parse(wire);
    assertEquals(1, packet.getVersion());
    assertArrayEquals(new byte[]{1,2,3,4,5,6,7,8}, packet.getToken());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2, 3})
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 §3: messages with an unsupported CoAP version are message format errors",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void unsupportedVersionsAreRejected(int version) {
    byte first = (byte) ((version << 6) | (TYPE.CON.getValue() << 4));
    assertThrows(IOException.class, () -> parse(new byte[]{first, 0x01, 0x00, 0x01}));
  }

  @ParameterizedTest
  @ValueSource(ints = {9,10,11,12,13,14,15})
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 §3: TKL values 9..15 are reserved and MUST be treated as message format errors",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void reservedTokenLengthsAreRejected(int tokenLength) {
    byte first = (byte) ((1 << 6) | (TYPE.CON.getValue() << 4) | tokenLength);
    byte[] wire = new byte[4 + tokenLength];
    wire[0] = first;
    wire[1] = 0x01;
    assertThrows(IOException.class, () -> parse(wire));
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 §3: payload marker 0xFF MUST NOT appear when the message has a zero-length payload",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void emptyPayloadMarkerIsRejected() {
    assertThrows(IOException.class, () -> parse(new byte[]{0x50, 0x45, 0x00, 0x01, (byte)0xFF}));
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 §3.1: option delta nibble 15 is reserved for the payload marker and is a format error in an option header",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void reservedOptionDeltaNibbleIsRejected() {
    assertThrows(IOException.class, () -> parse(new byte[]{0x50, 0x01, 0, 1, (byte)0xF0}));
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 §3.1: option length nibble 15 is reserved and is a message format error",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void reservedOptionLengthNibbleIsRejected() {
    assertThrows(IOException.class, () -> parse(new byte[]{0x50, 0x01, 0, 1, 0x1F}));
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 §3 and §5: response Code classes 2.xx/4.xx/5.xx are responses, not request methods",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void contentResponseIsNotDispatchedAsFetchRequest() throws Exception {
    BasePacket packet = parse(new byte[]{0x60, 0x45, 0x00, 0x01});
    assertEquals(Code.CONTENT, packet.getCode());
    assertFalse(packet instanceof Fetch);
  }

  @Test
  @Disabled("Known CoAP empty-message validation gap: MSG-369")
  @ProtocolRequirement(
      specification = RFC7252,
      value = "RFC 7252 §4.1: an Empty message has Code 0.00, TKL 0, and no payload",
      source = ProtocolRequirement.COAP_RFC7252_SOURCE)
  void emptyMessageWithTokenIsRejected() {
    assertThrows(IOException.class, () -> parse(new byte[]{0x41, 0x00, 0, 1, 0x55}));
  }

  @Test
  @Disabled("Known CoAP Observe option-length validation gap: MSG-369")
  @ProtocolRequirement(
      specification = RFC7641,
      value = "RFC 7641 §2: Observe Option format is uint with length 0..3 bytes",
      source = ProtocolRequirement.COAP_RFC7641_SOURCE)
  void fourByteObserveOptionIsRejected() {
    assertThrows(IOException.class, () -> parse(new byte[]{
        0x40, 0x01, 0, 1,
        0x64, 0, 0, 0, 1
    }));
  }

  @ParameterizedTest
  @ValueSource(ints = {0,1,2,3,4,5,6})
  @ProtocolRequirement(
      specification = RFC7959,
      value = "RFC 7959 §2.2 and §2.3: UDP Block1/Block2 SZX values 0..6 encode block sizes 16..1024",
      source = ProtocolRequirement.COAP_RFC7959_SOURCE)
  void blockOptionRoundTripsValidUdpSzxValues(int szx) throws Exception {
    Block original = new Block(23, 0x1234, true, szx);
    Block decoded = new Block(23);
    decoded.update(original.pack());
    assertEquals(0x1234, decoded.getNumber());
    assertTrue(decoded.isMore());
    assertEquals(szx, decoded.getSizeEx());
  }

  @Test
  @ProtocolRequirement(
      specification = RFC7959,
      value = "RFC 7959 §2.2: SZX=7 is reserved for UDP CoAP block-wise transfers",
      source = ProtocolRequirement.COAP_RFC7959_SOURCE)
  void blockSzxSevenIsRejectedOnDecode() {
    Block block = new Block(23);
    assertThrows(IOException.class, () -> block.update(new byte[]{0x07}));
  }

  private static BasePacket parse(byte[] bytes) throws Exception {
    return FACTORY.parseFrame(new Packet(ByteBuffer.wrap(bytes)));
  }
}
