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

package io.mapsmessaging.network.protocol.impl.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebSocketFragmentationTest extends StompBaseTest {

  private static final byte[] CAPTURE_MASK = {(byte) 0xE9, (byte) 0xBF, (byte) 0x98, 0x15};
  private static final int CAPTURE_PAYLOAD_LENGTH = 1_183;

  @Test
  void acceptsSplitUpgradeAndSplitExtendedLengthStompFrame() throws Exception {
    String destination = "/topic/ws-fragment-" + UUID.randomUUID();
    try (Socket socket = new Socket("localhost", 8674)) {
      socket.setSoTimeout(5_000);
      socket.setTcpNoDelay(true);
      InputStream input = socket.getInputStream();
      OutputStream output = socket.getOutputStream();

      sendSplitUpgrade(output);
      String upgradeResponse = readHttpHeaders(input);
      assertTrue(upgradeResponse.startsWith("HTTP/1.1 101"), upgradeResponse);
      assertTrue(
          upgradeResponse.toLowerCase().contains("sec-websocket-protocol: v12.stomp"),
          upgradeResponse);

      writeClientFrame(
          output,
          stompFrame(
              "CONNECT\n"
                  + "accept-version:1.2\n"
                  + "host:localhost\n"
                  + "heart-beat:0,0\n"
                  + "\n"),
          false);
      assertTrue(readServerFrame(input).text().startsWith("CONNECTED\n"));

      writeClientFrame(
          output,
          stompFrame(
              "SUBSCRIBE\n"
                  + "id:fragment-test\n"
                  + "destination:"
                  + destination
                  + "\n"
                  + "ack:auto\n"
                  + "receipt:subscribed\n"
                  + "\n"),
          false);
      assertTrue(readServerFrame(input).text().startsWith("RECEIPT\n"));

      byte[] sendFrame = exactLengthSendFrame(destination, CAPTURE_PAYLOAD_LENGTH);
      assertEquals(CAPTURE_PAYLOAD_LENGTH, sendFrame.length);
      writeClientFrame(output, sendFrame, true);

      ServerFrame received = readServerFrame(input);
      assertTrue(received.text().startsWith("MESSAGE\n"), received.text());
      assertTrue(received.text().contains("destination:" + destination), received.text());
      assertFalse(socket.isClosed());
    }
  }

  private static void sendSplitUpgrade(OutputStream output) throws IOException, InterruptedException {
    byte[] first =
        ("GET /ws HTTP/1.1\r\n"
                + "Host: localhost:8674\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: keep-alive, Upgrade\r\n"
                + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n")
            .getBytes(StandardCharsets.US_ASCII);
    byte[] second =
        ("Sec-WebSocket-Version: 13\r\n"
                + "Sec-WebSocket-Protocol: v12.stomp\r\n"
                + "\r\n")
            .getBytes(StandardCharsets.US_ASCII);

    output.write(first);
    output.flush();
    Thread.sleep(50);
    output.write(second);
    output.flush();
  }

  private static String readHttpHeaders(InputStream input) throws IOException {
    ByteArrayOutputStream response = new ByteArrayOutputStream();
    int matched = 0;
    byte[] end = {'\r', '\n', '\r', '\n'};
    while (matched < end.length) {
      int value = input.read();
      if (value < 0) {
        throw new EOFException("Connection closed during WebSocket upgrade");
      }
      response.write(value);
      matched = value == end[matched] ? matched + 1 : value == end[0] ? 1 : 0;
    }
    return response.toString(StandardCharsets.US_ASCII);
  }

  private static byte[] exactLengthSendFrame(String destination, int length) {
    byte[] prefix =
        ("SEND\n"
                + "destination:"
                + destination
                + "\n"
                + "content-type:text/plain\n"
                + "\n")
            .getBytes(StandardCharsets.UTF_8);
    int bodyLength = length - prefix.length - 1;
    if (bodyLength < 1) {
      throw new IllegalArgumentException("Requested STOMP frame length is too small");
    }

    byte[] frame = new byte[length];
    System.arraycopy(prefix, 0, frame, 0, prefix.length);
    Arrays.fill(frame, prefix.length, frame.length - 1, (byte) 'x');
    frame[frame.length - 1] = 0;
    return frame;
  }

  private static byte[] stompFrame(String value) {
    byte[] text = value.getBytes(StandardCharsets.UTF_8);
    return Arrays.copyOf(text, text.length + 1);
  }

  private static void writeClientFrame(OutputStream output, byte[] payload, boolean splitHeader)
      throws IOException, InterruptedException {
    byte[] header = clientHeader(payload.length, CAPTURE_MASK);
    byte[] maskedPayload = payload.clone();
    for (int index = 0; index < maskedPayload.length; index++) {
      maskedPayload[index] ^= CAPTURE_MASK[index & 3];
    }

    output.write(header);
    output.flush();
    if (splitHeader) {
      assertEquals(8, header.length);
      Thread.sleep(50);
    }
    output.write(maskedPayload);
    output.flush();
  }

  private static byte[] clientHeader(int payloadLength, byte[] mask) {
    ByteArrayOutputStream header = new ByteArrayOutputStream();
    header.write(0x81);
    if (payloadLength < 126) {
      header.write(0x80 | payloadLength);
    } else if (payloadLength <= 0xFFFF) {
      header.write(0x80 | 126);
      header.write((payloadLength >>> 8) & 0xFF);
      header.write(payloadLength & 0xFF);
    } else {
      header.write(0x80 | 127);
      long length = payloadLength;
      for (int shift = 56; shift >= 0; shift -= 8) {
        header.write((int) ((length >>> shift) & 0xFF));
      }
    }
    header.writeBytes(mask);
    return header.toByteArray();
  }

  private static ServerFrame readServerFrame(InputStream input) throws IOException {
    int first = readRequired(input);
    int second = readRequired(input);
    boolean finish = (first & 0x80) != 0;
    int opcode = first & 0x0F;
    boolean masked = (second & 0x80) != 0;
    long length = second & 0x7F;
    if (length == 126) {
      length = ((long) readRequired(input) << 8) | readRequired(input);
    } else if (length == 127) {
      length = 0;
      for (int index = 0; index < 8; index++) {
        length = (length << 8) | readRequired(input);
      }
    }
    if (length > Integer.MAX_VALUE) {
      throw new IOException("Server frame is too large for test: " + length);
    }
    byte[] payload = input.readNBytes((int) length);
    if (payload.length != length) {
      throw new EOFException("Connection closed during server WebSocket frame");
    }
    return new ServerFrame(finish, opcode, masked, payload);
  }

  private static int readRequired(InputStream input) throws IOException {
    int value = input.read();
    if (value < 0) {
      throw new EOFException("Connection closed during server WebSocket frame");
    }
    return value;
  }

  private record ServerFrame(boolean finish, int opcode, boolean masked, byte[] payload) {
    private ServerFrame {
      assertTrue(finish);
      assertTrue(opcode == 1 || opcode == 2, "Unexpected server opcode " + opcode);
      assertFalse(masked, "Server WebSocket frames must not be masked");
    }

    private String text() {
      int length = payload.length;
      while (length > 0 && payload[length - 1] == 0) {
        length--;
      }
      return new String(payload, 0, length, StandardCharsets.UTF_8);
    }
  }
}
