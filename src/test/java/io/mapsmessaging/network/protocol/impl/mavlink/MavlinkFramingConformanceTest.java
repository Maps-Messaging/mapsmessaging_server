package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mavlink")
class MavlinkFramingConformanceTest {

  private static final String SPEC = "MAVLink Packet Serialization";
  private static final String SOURCE = ProtocolRequirement.MAVLINK_SERIALIZATION_SOURCE;

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section MAVLink 1 Packet Format: STX 0xFE, payload length, 4-byte remaining header, payload and 2-byte checksum",
      source = SOURCE)
  void extractsMavlink1FrameUsingDeclaredPayloadLength() {
    byte[] frame = v1Frame(3, 17, 42, 7);
    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(frame);
    assertEquals(1, frames.size());
    assertArrayEquals(frame, frames.getFirst());
    assertEquals(42, MavlinkFrameExtractor.getSystemId(frame));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section MAVLink 2 Packet Format: STX 0xFD and unsigned packet length is payload plus 12 framing bytes",
      source = SOURCE)
  void extractsUnsignedMavlink2Frame() {
    byte[] frame = v2Frame(4, 0, 0x80, 18, 43, 8);
    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(frame);
    assertEquals(1, frames.size());
    assertArrayEquals(frame, frames.getFirst());
    assertEquals(43, MavlinkFrameExtractor.getSystemId(frame));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section MAVLink 2 Signed Packet Format: signed incompatibility flag appends 13 signature bytes",
      source = SOURCE)
  void signedMavlink2FrameIncludesThirteenSignatureBytes() {
    byte[] frame = v2Frame(2, 0x01, 0, 19, 44, 9);
    byte[] signed = java.util.Arrays.copyOf(frame, frame.length + 13);
    for (int i = frame.length; i < signed.length; i++) {
      signed[i] = (byte) i;
    }

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(signed);

    assertEquals(1, frames.size());
    assertArrayEquals(signed, frames.getFirst());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Incompatibility Flags: receiver must discard a MAVLink 2 packet if it does not understand an incompatibility flag",
      source = SOURCE)
  void unknownIncompatibilityFlagIsRejectedAndExtractorResynchronises() {
    byte[] unsupported = v2Frame(0, 0x02, 0, 1, 2, 3);
    byte[] valid = v1Frame(0, 2, 7, 8);
    byte[] stream = concat(unsupported, valid);

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(stream);

    assertEquals(1, frames.size());
    assertArrayEquals(valid, frames.getFirst());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Incompatibility Flags: stream receiver must reject packets containing unsupported incompatibility bits",
      source = SOURCE)
  void streamHandlerRejectsUnknownIncompatibilityFlag() {
    MavlinkStreamHandler handler = new MavlinkStreamHandler(100);
    byte[] unsupported = v2Frame(0, 0x02, 0, 1, 2, 3);
    Packet packet = new Packet(unsupported.length, false);

    IOException error = assertThrows(
        IOException.class,
        () -> handler.parseInput(new ByteArrayInputStream(unsupported), packet));

    assertTrue(error.getMessage().contains("incompatibility"));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Compatibility Flags: unknown compatibility flags may be ignored because they do not alter packet handling",
      source = SOURCE)
  void unknownCompatibilityFlagDoesNotInvalidateFraming() throws Exception {
    MavlinkStreamHandler handler = new MavlinkStreamHandler(100);
    byte[] frame = v2Frame(0, 0, 0x80, 5, 11, 22);
    Packet packet = new Packet(frame.length, false);

    int read = handler.parseInput(new ByteArrayInputStream(frame), packet);

    assertEquals(frame.length, read);
    packet.flip();
    byte[] decoded = new byte[packet.available()];
    packet.get(decoded);
    assertArrayEquals(frame, decoded);
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section Packet Format: receiver may resynchronise by scanning for MAVLink 1 or MAVLink 2 start markers",
      source = SOURCE)
  void noiseBeforeFrameIsDiscardedDuringResynchronisation() {
    byte[] valid = v1Frame(1, 9, 23, 24);
    byte[] input = concat(new byte[]{0x00, 0x11, 0x22}, valid);

    List<byte[]> frames = MavlinkFrameExtractor.extractMavlinkFrames(input);

    assertEquals(1, frames.size());
    assertArrayEquals(valid, frames.getFirst());
  }

  private static byte[] v1Frame(int payloadLength, int sequence, int systemId, int componentId) {
    byte[] frame = new byte[payloadLength + 8];
    frame[0] = (byte) 0xFE;
    frame[1] = (byte) payloadLength;
    frame[2] = (byte) sequence;
    frame[3] = (byte) systemId;
    frame[4] = (byte) componentId;
    frame[5] = 0;
    return frame;
  }

  private static byte[] v2Frame(
      int payloadLength,
      int incompatFlags,
      int compatFlags,
      int sequence,
      int systemId,
      int componentId) {
    byte[] frame = new byte[payloadLength + 12];
    frame[0] = (byte) 0xFD;
    frame[1] = (byte) payloadLength;
    frame[2] = (byte) incompatFlags;
    frame[3] = (byte) compatFlags;
    frame[4] = (byte) sequence;
    frame[5] = (byte) systemId;
    frame[6] = (byte) componentId;
    return frame;
  }

  private static byte[] concat(byte[] first, byte[] second) {
    byte[] result = new byte[first.length + second.length];
    System.arraycopy(first, 0, result, 0, first.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
}
