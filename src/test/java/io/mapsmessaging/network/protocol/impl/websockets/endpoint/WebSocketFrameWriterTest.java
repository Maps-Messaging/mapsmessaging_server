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

package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketFrameWriterTest {

  @Test
  void preservesOneFrameAcrossPartialSocketWrites() throws IOException {
    ChunkedSink sink = new ChunkedSink(3);
    WebSocketFrameWriter writer = new WebSocketFrameWriter(sink);
    byte[] payload = "hello websocket".getBytes(StandardCharsets.UTF_8);
    Packet source = new Packet(ByteBuffer.wrap(payload));

    int consumed;
    do {
      consumed = writer.writeBinary(source);
    } while (consumed == 0);

    assertEquals(payload.length, consumed);
    assertEquals(source.limit(), source.position());
    byte[] expected = new byte[payload.length + 2];
    expected[0] = (byte) 0x82;
    expected[1] = (byte) payload.length;
    System.arraycopy(payload, 0, expected, 2, payload.length);
    assertArrayEquals(expected, sink.bytes());
  }

  @Test
  void preservesSingleBytePayloadAcrossBlockedWrite() throws IOException {
    BlockingOnceSink sink = new BlockingOnceSink();
    WebSocketFrameWriter writer = new WebSocketFrameWriter(sink);
    Packet source = new Packet(8, false);
    source.put((byte) '\n');
    source.flip();

    assertEquals(0, writer.writeBinary(source));
    assertEquals(1, writer.writeBinary(source));

    assertArrayEquals(new byte[]{(byte) 0x82, 0x01, '\n'}, sink.bytes());
    assertEquals(source.limit(), source.position());
  }

  @Test
  void writesPongWithThePingPayloadAndNoPadding() throws IOException {
    ChunkedSink sink = new ChunkedSink(Integer.MAX_VALUE);
    WebSocketFrameWriter writer = new WebSocketFrameWriter(sink);

    assertTrue(writer.writeControl(WebSocketFrameDecoder.PONG, "ok".getBytes(StandardCharsets.UTF_8)));

    assertArrayEquals(new byte[]{(byte) 0x8A, 0x02, 'o', 'k'}, sink.bytes());
    assertFalse(writer.hasPendingApplicationData());
  }

  @Test
  void usesExtendedLengthForLargerPayload() throws IOException {
    ChunkedSink sink = new ChunkedSink(Integer.MAX_VALUE);
    WebSocketFrameWriter writer = new WebSocketFrameWriter(sink);
    byte[] payload = new byte[1_183];
    Packet source = new Packet(ByteBuffer.wrap(payload));

    assertEquals(payload.length, writer.writeBinary(source));

    byte[] frame = sink.bytes();
    assertEquals((byte) 0x82, frame[0]);
    assertEquals((byte) 126, frame[1]);
    assertEquals(0x04, frame[2] & 0xFF);
    assertEquals(0x9F, frame[3] & 0xFF);
    assertEquals(payload.length + 4, frame.length);
  }

  private static final class BlockingOnceSink implements WebSocketFrameWriter.PacketSink {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private boolean blocked = true;

    @Override
    public int write(Packet packet) {
      if (blocked) {
        blocked = false;
        return 0;
      }
      int count = packet.available();
      byte[] value = new byte[count];
      packet.get(value);
      output.writeBytes(value);
      return count;
    }

    private byte[] bytes() {
      return output.toByteArray();
    }
  }

  private static final class ChunkedSink implements WebSocketFrameWriter.PacketSink {
    private final int maximumWrite;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private ChunkedSink(int maximumWrite) {
      this.maximumWrite = maximumWrite;
    }

    @Override
    public int write(Packet packet) {
      int count = Math.min(maximumWrite, packet.available());
      byte[] value = new byte[count];
      packet.get(value);
      output.writeBytes(value);
      return count;
    }

    private byte[] bytes() {
      return output.toByteArray();
    }
  }
}
