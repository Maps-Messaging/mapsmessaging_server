/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkStreamHandlerTest {

  private static final int MAVLINK_V1_MAGIC = 0xFE;
  private static final int MAVLINK_V2_MAGIC = 0xFD;

  private static final int MAVLINK_V1_HEADER_REST = 5;
  private static final int MAVLINK_V1_CRC_LEN = 2;

  private static final int MAVLINK_V2_HEADER_REST = 8;
  private static final int MAVLINK_V2_CRC_LEN = 2;
  private static final int MAVLINK_V2_SIGNATURE_LEN = 13;
  private static final int MAVLINK_V2_INCOMPAT_FLAG_SIGNED = 0x01;

  @Test
  void parseInput_skipsNoiseUntilMagic_andDoesNotSkipFrames() throws Exception {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] v1 = buildV1Frame(
        4,
        (byte) 10, (byte) 11, (byte) 12, (byte) 13,
        (byte) 1, (byte) 2
    );

    byte[] v2 = buildV2Frame(
        3,
        false,
        (byte) 21, (byte) 22, (byte) 23,
        (byte) 4, (byte) 5
    );

    byte[] noise = new byte[]{0x00, 0x7E, 0x55, (byte) 0xAA, 0x33};

    byte[] stream = concat(noise, v1, v2);
    ByteArrayInputStream input = new ByteArrayInputStream(stream);

    TestPacket packet1 = new TestPacket(512);
    int len1 = handler.parseInput(input, packet1);
    assertEquals(v1.length, len1);
    assertArrayEquals(v1, packet1.toByteArrayExact());

    TestPacket packet2 = new TestPacket(512);
    int len2 = handler.parseInput(input, packet2);
    assertEquals(v2.length, len2);
    assertArrayEquals(v2, packet2.toByteArrayExact());
  }

  @Test
  void parseInput_v2Signed_readsSignatureWhenFlagSet() throws Exception {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] v2Signed = buildV2Frame(
        6,
        true,
        (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6,
        (byte) 9, (byte) 10
    );

    ByteArrayInputStream input = new ByteArrayInputStream(v2Signed);
    TestPacket packet = new TestPacket(512);

    int len = handler.parseInput(input, packet);
    assertEquals(v2Signed.length, len);
    assertArrayEquals(v2Signed, packet.toByteArrayExact());
  }

  @Test
  void parseInput_v2Unsigned_doesNotTryToReadSignature() throws Exception {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] v2Unsigned = buildV2Frame(
        0,
        false
    );

    ByteArrayInputStream input = new ByteArrayInputStream(v2Unsigned);
    TestPacket packet = new TestPacket(128);

    int len = handler.parseInput(input, packet);
    assertEquals(v2Unsigned.length, len);
    assertArrayEquals(v2Unsigned, packet.toByteArrayExact());

    assertEquals(0, input.available());
  }

  @Test
  void parseInput_truncatedHeader_throwsUnexpectedEnd() {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] truncated = new byte[]{
        (byte) MAVLINK_V1_MAGIC,
        10,
        1, 2
    };

    ByteArrayInputStream input = new ByteArrayInputStream(truncated);
    TestPacket packet = new TestPacket(256);

    IOException ex = assertThrows(IOException.class, () -> handler.parseInput(input, packet));
    assertTrue(ex.getMessage().toLowerCase().contains("unexpected end"));
  }

  @Test
  void parseInput_truncatedPayload_throwsUnexpectedEnd() {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] frame = buildV1Frame(
        10,
        (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5,
        (byte) 9, (byte) 9
    );

    byte[] truncated = Arrays.copyOf(frame, frame.length - 5);

    ByteArrayInputStream input = new ByteArrayInputStream(truncated);
    TestPacket packet = new TestPacket(256);

    IOException ex = assertThrows(IOException.class, () -> handler.parseInput(input, packet));
    assertTrue(ex.getMessage().toLowerCase().contains("unexpected end"));
  }

  @Test
  void parseInput_capacityTooSmall_throws() {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] v2Signed = buildV2Frame(
        20,
        true,
        (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10,
        (byte) 0x11, (byte) 0x22
    );

    ByteArrayInputStream input = new ByteArrayInputStream(v2Signed);
    TestPacket packet = new TestPacket(16);

    IOException ex = assertThrows(IOException.class, () -> handler.parseInput(input, packet));
    assertTrue(ex.getMessage().toLowerCase().contains("smaller than required"));
  }

  @Test
  void parseOutput_writesAllBytes_afterFlip() throws Exception {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] v1 = buildV1Frame(
        5,
        (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14,
        (byte) 1, (byte) 2
    );

    Packet packet = new Packet(128, false);
    packet.clear();
    packet.put(v1, 0, v1.length);
    packet.flip(); // critical with your Packet/ByteBuffer contract

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int written = handler.parseOutput(out, packet);

    assertEquals(v1.length, written);
    assertArrayEquals(v1, out.toByteArray());
  }
  @Test
  void parseOutput_zeroLength_returnsZero() throws Exception {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    Packet packet = new Packet(64, false);
    packet.clear();
    packet.limit(0); // empty: position=0, limit=0 (since clear() makes limit=capacity)

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int written = handler.parseOutput(out, packet);

    assertEquals(0, written);
    assertEquals(0, out.size());
  }
  @Test
  void parseInput_handlesChunkedInputStream_withoutSkipping() throws Exception {
    MavlinkStreamHandler handler = new MavlinkStreamHandler();

    byte[] v1 = buildV1Frame(
        20,
        (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5,
        (byte) 0x12, (byte) 0x34
    );

    byte[] v2 = buildV2Frame(
        25,
        true,
        (byte) 0x55, (byte) 0x66
    );

    byte[] stream = concat(v1, v2);

    ByteArrayInputStream base = new ByteArrayInputStream(stream);
    InputStream chunked = new ChunkedInputStream(base, 3); // max 3 bytes per read

    Packet packet1 = new Packet(512, false);
    int len1 = handler.parseInput(chunked, packet1);
    assertEquals(v1.length, len1);
    packet1.flip();
    assertArrayEquals(v1, toByteArray(packet1));

    Packet packet2 = new Packet(512, false);
    int len2 = handler.parseInput(chunked, packet2);
    assertEquals(v2.length, len2);
    packet2.flip();
    assertArrayEquals(v2, toByteArray(packet2));
  }

  private static byte[] toByteArray(Packet packet) {
    ByteBuffer dup = packet.getRawBuffer().duplicate();
    byte[] out = new byte[dup.remaining()];
    dup.get(out);
    return out;
  }

  private static final class ChunkedInputStream extends InputStream {

    private final InputStream delegate;
    private final int maxPerRead;

    private ChunkedInputStream(InputStream delegate, int maxPerRead) {
      this.delegate = delegate;
      this.maxPerRead = maxPerRead;
    }

    @Override
    public int read() throws IOException {
      return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int capped = Math.min(len, maxPerRead);
      return delegate.read(b, off, capped);
    }
  }


  private static byte[] buildV1Frame(int payloadLength, byte seq, byte sysId, byte compId, byte msgId, byte... payloadAndCrc) {
    byte[] payload;
    byte[] crc;
    if (payloadLength == 0) {
      payload = new byte[0];
      crc = new byte[]{0, 0};
    } else {
      if (payloadAndCrc.length < payloadLength + 2) {
        payload = new byte[payloadLength];
        for (int i = 0; i < payloadLength; i++) {
          payload[i] = (byte) (0xA0 + i);
        }
        crc = new byte[]{(byte) 0x12, (byte) 0x34};
      } else {
        payload = Arrays.copyOfRange(payloadAndCrc, 0, payloadLength);
        crc = Arrays.copyOfRange(payloadAndCrc, payloadLength, payloadLength + 2);
      }
    }

    int length = 2 + MAVLINK_V1_HEADER_REST + payloadLength + MAVLINK_V1_CRC_LEN;
    ByteBuffer bb = ByteBuffer.allocate(length);

    bb.put((byte) MAVLINK_V1_MAGIC);
    bb.put((byte) payloadLength);

    bb.put(seq);
    bb.put(sysId);
    bb.put(compId);
    bb.put(msgId);
    bb.put((byte) 0x00);

    bb.put(payload);
    bb.put(crc);

    return bb.array();
  }

  private static byte[] buildV2Frame(int payloadLength, boolean signed, byte... payloadAndCrc) {
    byte incompatFlags = (byte) (signed ? MAVLINK_V2_INCOMPAT_FLAG_SIGNED : 0x00);
    byte compatFlags = 0x00;
    byte seq = 0x01;
    byte sysId = 0x02;
    byte compId = 0x03;
    byte msgId0 = 0x11;
    byte msgId1 = 0x22;
    byte msgId2 = 0x33;

    byte[] payload;
    byte[] crc;
    if (payloadLength == 0) {
      payload = new byte[0];
      crc = new byte[]{0, 0};
    } else {
      if (payloadAndCrc.length < payloadLength + 2) {
        payload = new byte[payloadLength];
        for (int i = 0; i < payloadLength; i++) {
          payload[i] = (byte) (0xB0 + i);
        }
        crc = new byte[]{(byte) 0x55, (byte) 0x66};
      } else {
        payload = Arrays.copyOfRange(payloadAndCrc, 0, payloadLength);
        crc = Arrays.copyOfRange(payloadAndCrc, payloadLength, payloadLength + 2);
      }
    }

    byte[] signature = new byte[0];
    if (signed) {
      signature = new byte[MAVLINK_V2_SIGNATURE_LEN];
      for (int i = 0; i < signature.length; i++) {
        signature[i] = (byte) (0xC0 + i);
      }
    }

    int length = 2 + MAVLINK_V2_HEADER_REST + payloadLength + MAVLINK_V2_CRC_LEN + signature.length;
    ByteBuffer bb = ByteBuffer.allocate(length);

    bb.put((byte) MAVLINK_V2_MAGIC);
    bb.put((byte) payloadLength);

    bb.put(incompatFlags);
    bb.put(compatFlags);
    bb.put(seq);
    bb.put(sysId);
    bb.put(compId);
    bb.put(msgId0);
    bb.put(msgId1);
    bb.put(msgId2);

    bb.put(payload);
    bb.put(crc);
    bb.put(signature);

    return bb.array();
  }

  private static byte[] concat(byte[]... parts) {
    int total = 0;
    for (byte[] p : parts) {
      total += p.length;
    }
    byte[] out = new byte[total];
    int pos = 0;
    for (byte[] p : parts) {
      System.arraycopy(p, 0, out, pos, p.length);
      pos += p.length;
    }
    return out;
  }

  /**
   * Adjust this to match your real Packet API if needed.
   * The handler only needs: clear(), capacity(), putByte(int), put(byte[],off,len), getRawBuffer().
   */
  public static final class TestPacket extends Packet {

    public TestPacket(int capacity) {
      super(capacity, false);
    }

    public byte[] toByteArrayExact() {
      ByteBuffer dup = getRawBuffer().duplicate();
      dup.flip();
      byte[] out = new byte[dup.remaining()];
      dup.get(out);
      return out;
    }
  }
}
