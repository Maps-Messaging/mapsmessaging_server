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

package io.mapsmessaging.network.protocol.impl.websockets;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.protocol.impl.websockets.frames.Frame;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectingTest {

  private static final String REQUEST = "GET /ws HTTP/1.1\r\n"
      + "Host: example.test\r\n"
      + "Upgrade: websocket\r\n"
      + "Connection: keep-alive, Upgrade\r\n"
      + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
      + "Sec-WebSocket-Version: 13\r\n"
      + "\r\n";

  @Test
  void parsesUpgradeAcrossMultipleTcpReads() throws Exception {
    Connecting connecting = new Connecting();
    TestEndPoint endPoint = new TestEndPoint();
    byte[] request = REQUEST.getBytes(StandardCharsets.US_ASCII);
    int split = REQUEST.indexOf("Connection:") + 7;

    Packet firstRead = packet(request, 0, split);
    assertNull(connecting.handle(firstRead, endPoint));
    byte[] outstanding = remaining(firstRead);
    byte[] secondRead = new byte[outstanding.length + request.length - split];
    System.arraycopy(outstanding, 0, secondRead, 0, outstanding.length);
    System.arraycopy(request, split, secondRead, outstanding.length, request.length - split);
    Frame response = connecting.handle(new Packet(ByteBuffer.wrap(secondRead)), endPoint);

    assertNotNull(response);
    assertEquals("websocket", response.getHeaders().get("Upgrade"));
    assertEquals("Upgrade", response.getHeaders().get("Connection"));
    assertEquals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=", response.getHeaders().get("Sec-WebSocket-Accept"));
  }

  @Test
  void rejectsUnsupportedVersion() {
    Connecting connecting = new Connecting();
    String request = REQUEST.replace("Sec-WebSocket-Version: 13", "Sec-WebSocket-Version: 12");

    Exception error = assertThrows(Exception.class,
        () -> connecting.handle(packet(request.getBytes(StandardCharsets.US_ASCII), 0, request.length()), new TestEndPoint()));

    assertTrue(error.getMessage().contains("Unsupported WebSocket version"));
  }

  private static Packet packet(byte[] value, int start, int end) {
    return new Packet(ByteBuffer.wrap(value, start, end - start).slice());
  }

  private static byte[] remaining(Packet packet) {
    ByteBuffer duplicate = packet.getRawBuffer().duplicate();
    byte[] value = new byte[duplicate.remaining()];
    duplicate.get(value);
    return value;
  }

  private static final class TestEndPoint extends EndPoint {

    private TestEndPoint() {
      super(1, null);
      name = "tcp_test";
    }

    @Override
    public String getProtocol() {
      return "tcp";
    }

    @Override
    public int sendPacket(Packet packet) {
      return 0;
    }

    @Override
    public int readPacket(Packet packet) {
      return 0;
    }

    @Override
    public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) {
      return null;
    }

    @Override
    public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
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
