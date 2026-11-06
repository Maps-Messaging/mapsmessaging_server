package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.mavlink.MavlinkEventFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mavlink")
class MavlinkDialectConformanceTest {

  private static final String SERIALIZATION = "MAVLink Packet Serialization";
  private static final String DIALECT = "MAVLink XML Dialect Schema";

  @Test
  @ProtocolRequirement(
      specification = SERIALIZATION,
      value = "Section CRC_EXTRA: checksum incorporates the message-definition CRC_EXTRA so sender and receiver must share a compatible definition",
      source = ProtocolRequirement.MAVLINK_SERIALIZATION_SOURCE)
  void commonDialectValidatesAndDecodesHeartbeatUsingCrcExtra() throws Exception {
    MavlinkEventFactory factory = new MavlinkEventFactory("common");
    byte[] frame = heartbeatV1(7, 3, 1);

    ProcessedFrame processed = factory.unpack("conformance", ByteBuffer.wrap(frame)).orElseThrow();

    assertTrue(processed.isValid());
    assertEquals("HEARTBEAT", processed.getMessageName());
    assertEquals(0, processed.getFrame().getMessageId());
    assertEquals(3, processed.getFrame().getSystemId());
    assertEquals(1, processed.getFrame().getComponentId());
    assertEquals(3, ((Number) processed.getFields().get("mavlink_version")).intValue());
  }

  @Test
  @ProtocolRequirement(
      specification = SERIALIZATION,
      value = "Section CRC_EXTRA: a checksum mismatch must classify the frame invalid rather than decode it as normal telemetry",
      source = ProtocolRequirement.MAVLINK_SERIALIZATION_SOURCE)
  void corruptHeartbeatChecksumIsClassifiedInvalid() throws Exception {
    MavlinkEventFactory factory = new MavlinkEventFactory("common");
    byte[] frame = heartbeatV1(8, 3, 1);
    frame[frame.length - 1] ^= 0x01;

    ProcessedFrame processed = factory.unpack("conformance", ByteBuffer.wrap(frame)).orElseThrow();

    assertFalse(processed.isValid());
    assertTrue(processed.getFields().isEmpty());
  }

  @Test
  @ProtocolRequirement(
      specification = DIALECT,
      value = "Section Message Definition: XML dialect message definitions drive field decoding for the selected message ID",
      source = ProtocolRequirement.MAVLINK_XML_SCHEMA_SOURCE)
  void commonDialectMapsHeartbeatPayloadFieldsBySchema() throws Exception {
    MavlinkEventFactory factory = new MavlinkEventFactory("common");
    byte[] frame = heartbeatV1(9, 42, 7);

    ProcessedFrame processed = factory.unpack("conformance", ByteBuffer.wrap(frame)).orElseThrow();

    assertTrue(processed.isValid());
    assertEquals(0L, ((Number) processed.getFields().get("custom_mode")).longValue());
    assertEquals(0, ((Number) processed.getFields().get("type")).intValue());
    assertEquals(0, ((Number) processed.getFields().get("autopilot")).intValue());
    assertEquals(0, ((Number) processed.getFields().get("base_mode")).intValue());
    assertEquals(0, ((Number) processed.getFields().get("system_status")).intValue());
    assertEquals(3, ((Number) processed.getFields().get("mavlink_version")).intValue());
  }

  @Test
  @ProtocolRequirement(
      specification = SERIALIZATION,
      value = "Section Compatibility Flags: MAVLink 2 compatibility flags can be ignored without invalidating an otherwise correct packet",
      source = ProtocolRequirement.MAVLINK_SERIALIZATION_SOURCE)
  void commonDialectAcceptsUnknownCompatibilityFlagOnValidV2Heartbeat() throws Exception {
    MavlinkEventFactory factory = new MavlinkEventFactory("common");
    byte[] frame = heartbeatV2(10, 42, 7, 0, 0x80);

    ProcessedFrame processed = factory.unpack("conformance", ByteBuffer.wrap(frame)).orElseThrow();

    assertTrue(processed.isValid());
    assertEquals("HEARTBEAT", processed.getMessageName());
  }

  private static byte[] heartbeatV1(int sequence, int systemId, int componentId) {
    byte[] payload = heartbeatPayload();
    byte[] frame = new byte[6 + payload.length + 2];
    frame[0] = (byte) 0xFE;
    frame[1] = (byte) payload.length;
    frame[2] = (byte) sequence;
    frame[3] = (byte) systemId;
    frame[4] = (byte) componentId;
    frame[5] = 0;
    System.arraycopy(payload, 0, frame, 6, payload.length);

    int crc = 0xFFFF;
    for (int index = 1; index < 6 + payload.length; index++) {
      crc = accumulate(frame[index], crc);
    }
    crc = accumulate((byte) 50, crc);
    frame[6 + payload.length] = (byte) crc;
    frame[7 + payload.length] = (byte) (crc >>> 8);
    return frame;
  }

  private static byte[] heartbeatV2(
      int sequence,
      int systemId,
      int componentId,
      int incompatFlags,
      int compatFlags) {
    byte[] payload = heartbeatPayload();
    byte[] frame = new byte[10 + payload.length + 2];
    frame[0] = (byte) 0xFD;
    frame[1] = (byte) payload.length;
    frame[2] = (byte) incompatFlags;
    frame[3] = (byte) compatFlags;
    frame[4] = (byte) sequence;
    frame[5] = (byte) systemId;
    frame[6] = (byte) componentId;
    frame[7] = 0;
    frame[8] = 0;
    frame[9] = 0;
    System.arraycopy(payload, 0, frame, 10, payload.length);

    int crc = 0xFFFF;
    for (int index = 1; index < 10 + payload.length; index++) {
      crc = accumulate(frame[index], crc);
    }
    crc = accumulate((byte) 50, crc);
    frame[10 + payload.length] = (byte) crc;
    frame[11 + payload.length] = (byte) (crc >>> 8);
    return frame;
  }

  private static byte[] heartbeatPayload() {
    return new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 3};
  }

  private static int accumulate(byte input, int crc) {
    int tmp = (input ^ (crc & 0xFF)) & 0xFF;
    tmp = (tmp ^ ((tmp << 4) & 0xFF)) & 0xFF;
    return ((crc >>> 8) ^ (tmp << 8) ^ (tmp << 3) ^ (tmp >>> 4)) & 0xFFFF;
  }
}
