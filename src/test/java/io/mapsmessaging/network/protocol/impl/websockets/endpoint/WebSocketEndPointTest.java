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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.FutureTask;
import org.junit.jupiter.api.Test;

class WebSocketEndPointTest {

  private static final byte[] MASK = {(byte) 0xE9, (byte) 0xBF, (byte) 0x98, 0x15};

  @Test
  void preservesHeaderStateAcrossSeparateTcpReads() throws IOException {
    byte[] payload = new byte[1_183];
    Arrays.fill(payload, (byte) 'x');
    byte[] frame = maskedFrame(WebSocketFrameDecoder.TEXT, true, payload, MASK);
    TestEndPoint delegate = new TestEndPoint();
    delegate.queueRead(Arrays.copyOfRange(frame, 0, 8));
    delegate.queueRead(Arrays.copyOfRange(frame, 8, frame.length));
    WebSocketEndPoint webSocket = new WebSocketEndPoint(delegate);
    Packet decoded = new Packet(payload.length, false);

    assertEquals(0, webSocket.readPacket(decoded));
    assertEquals(payload.length, webSocket.readPacket(decoded));

    assertArrayEquals(payload, bytes(decoded));
    assertEquals(0, delegate.writtenBytes().length);
  }

  @Test
  void drainsValidatedMessageWithoutAnotherSocketRead() throws IOException {
    byte[] payload = new byte[100];
    Arrays.fill(payload, (byte) 'z');
    TestEndPoint delegate = new TestEndPoint();
    delegate.queueRead(maskedFrame(WebSocketFrameDecoder.TEXT, true, payload, MASK));
    WebSocketEndPoint webSocket = new WebSocketEndPoint(delegate);
    ByteArrayOutputStream decoded = new ByteArrayOutputStream();

    do {
      Packet chunk = new Packet(17, false);
      int count = webSocket.readPacket(chunk);
      if (count > 0) {
        decoded.writeBytes(bytes(chunk));
      }
    } while (webSocket.hasBufferedReadData());

    assertArrayEquals(payload, decoded.toByteArray());
    assertEquals(1, delegate.readCalls());
  }

  @Test
  void decodesBytesCarriedWithTheUpgradeRequest() throws IOException {
    byte[] payload = "SEND\n\nhello\0".getBytes(StandardCharsets.UTF_8);
    byte[] frame = maskedFrame(WebSocketFrameDecoder.TEXT, true, payload, MASK);
    TestEndPoint delegate = new TestEndPoint();
    WebSocketEndPoint webSocket =
        new WebSocketEndPoint(delegate, new Packet(ByteBuffer.wrap(frame)));
    Packet decoded = new Packet(payload.length, false);

    assertEquals(payload.length, webSocket.readPacket(decoded));

    assertArrayEquals(payload, bytes(decoded));
  }

  @Test
  void respondsToPingWithOneValidPongFrame() throws IOException {
    byte[] pingPayload = "ok".getBytes(StandardCharsets.UTF_8);
    TestEndPoint delegate = new TestEndPoint();
    delegate.queueRead(maskedFrame(WebSocketFrameDecoder.PING, true, pingPayload, MASK));
    WebSocketEndPoint webSocket = new WebSocketEndPoint(delegate);

    assertEquals(0, webSocket.readPacket(new Packet(8, false)));

    assertArrayEquals(new byte[] {(byte) 0x8A, 0x02, 'o', 'k'}, delegate.writtenBytes());
  }

  @Test
  void sendsProtocolErrorCloseForMalformedClientFrame() {
    TestEndPoint delegate = new TestEndPoint();
    delegate.queueRead(new byte[] {(byte) 0x81, 0x01, 'x'});
    WebSocketEndPoint webSocket = new WebSocketEndPoint(delegate);

    assertThrows(IOException.class, () -> webSocket.readPacket(new Packet(8, false)));

    assertArrayEquals(
        new byte[] {(byte) 0x88, 0x02, 0x03, (byte) 0xEA}, delegate.writtenBytes());
  }

  @Test
  void echoesValidClosePayloadBeforeClosing() throws IOException {
    byte[] closePayload = {0x03, (byte) 0xE8};
    TestEndPoint delegate = new TestEndPoint();
    delegate.queueRead(maskedFrame(WebSocketFrameDecoder.CLOSE, true, closePayload, MASK));
    WebSocketEndPoint webSocket = new WebSocketEndPoint(delegate);

    assertEquals(-1, webSocket.readPacket(new Packet(8, false)));

    assertArrayEquals(
        new byte[] {(byte) 0x88, 0x02, 0x03, (byte) 0xE8}, delegate.writtenBytes());
  }

  private static byte[] maskedFrame(int opcode, boolean finish, byte[] payload, byte[] mask) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.write((finish ? 0x80 : 0) | opcode);
    if (payload.length < 126) {
      output.write(0x80 | payload.length);
    } else {
      output.write(0x80 | 126);
      output.write((payload.length >>> 8) & 0xFF);
      output.write(payload.length & 0xFF);
    }
    output.writeBytes(mask);
    for (int index = 0; index < payload.length; index++) {
      output.write(payload[index] ^ mask[index & 3]);
    }
    return output.toByteArray();
  }

  private static byte[] bytes(Packet packet) {
    ByteBuffer duplicate = packet.getRawBuffer().duplicate();
    duplicate.flip();
    byte[] value = new byte[duplicate.remaining()];
    duplicate.get(value);
    return value;
  }

  private static final class TestEndPoint extends EndPoint {
    private final Deque<byte[]> reads = new ArrayDeque<>();
    private final ByteArrayOutputStream writes = new ByteArrayOutputStream();
    private int readCalls;

    private TestEndPoint() {
      super(1, null);
      name = "tcp_test";
    }

    private void queueRead(byte[] value) {
      reads.offer(value);
    }

    private byte[] writtenBytes() {
      return writes.toByteArray();
    }

    private int readCalls() {
      return readCalls;
    }

    @Override
    public String getProtocol() {
      return "tcp";
    }

    @Override
    public int sendPacket(Packet packet) {
      int count = packet.available();
      byte[] value = new byte[count];
      packet.get(value);
      writes.writeBytes(value);
      return count;
    }

    @Override
    public int readPacket(Packet packet) {
      readCalls++;
      byte[] value = reads.poll();
      if (value == null) {
        return 0;
      }
      packet.put(value);
      return value.length;
    }

    @Override
    public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) {
      return null;
    }

    @Override
    public FutureTask<SelectionKey> deregister(int selectionKey)
        throws ClosedChannelException {
      return null;
    }

    @Override
    public String getAuthenticationConfig() {
      return null;
    }

    @Override
    protected Logger createLogger() {
      return LoggerFactory.getLogger(TestEndPoint.class);
    }

    @Override
    public String getRemoteSocketAddress() {
      return "test";
    }
  }
}
