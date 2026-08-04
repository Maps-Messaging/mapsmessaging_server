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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketFrameDecoderTest {

  private static final byte[] MASK = {(byte) 0xE9, (byte) 0xBF, (byte) 0x98, 0x15};

  @Test
  void decodesExtendedLengthFrameWhenHeaderAndPayloadArriveSeparately() throws IOException {
    byte[] payload = stompPayload(1_183);
    byte[] frame = maskedFrame(WebSocketFrameDecoder.TEXT, true, payload, MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    RecordingListener listener = new RecordingListener();
    Packet decoded = new Packet(payload.length, false);

    Packet headerOnly = packet(Arrays.copyOfRange(frame, 0, 8));
    assertEquals(0, decoder.decode(headerOnly, decoded, listener));
    assertEquals(0, decoded.position());

    Packet payloadOnly = packet(Arrays.copyOfRange(frame, 8, frame.length));
    assertEquals(payload.length, decoder.decode(payloadOnly, decoded, listener));
    assertArrayEquals(payload, bytes(decoded));
    assertTrue(listener.pings.isEmpty());
  }

  @Test
  void decodesFrameWhenEveryNetworkReadContainsOneByte() throws IOException {
    byte[] payload = "SEND\ndestination:/topic/test\n\nhello\0".getBytes(StandardCharsets.UTF_8);
    byte[] frame = maskedFrame(WebSocketFrameDecoder.TEXT, true, payload, MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    RecordingListener listener = new RecordingListener();
    Packet decoded = new Packet(payload.length, false);

    for (byte value : frame) {
      decoder.decode(packet(new byte[]{value}), decoded, listener);
    }

    assertArrayEquals(payload, bytes(decoded));
  }

  @Test
  void withholdsFragmentedMessageUntilFinalFragment() throws IOException {
    byte[] first = maskedFrame(WebSocketFrameDecoder.TEXT, false, "HEL".getBytes(StandardCharsets.UTF_8), MASK);
    byte[] ping = maskedFrame(WebSocketFrameDecoder.PING, true, "check".getBytes(StandardCharsets.UTF_8), MASK);
    byte[] last = maskedFrame(WebSocketFrameDecoder.CONTINUATION, true, "LO".getBytes(StandardCharsets.UTF_8), MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    RecordingListener listener = new RecordingListener();
    Packet decoded = new Packet(5, false);

    assertEquals(0, decoder.decode(packet(first), decoded, listener));
    assertEquals(0, decoded.position());
    assertEquals(0, decoder.decode(packet(ping), decoded, listener));
    assertEquals(0, decoded.position());
    assertEquals(5, decoder.decode(packet(last), decoded, listener));

    assertEquals("HELLO", new String(bytes(decoded), StandardCharsets.UTF_8));
    assertEquals(1, listener.pings.size());
    assertEquals("check", new String(listener.pings.get(0), StandardCharsets.UTF_8));
  }

  @Test
  void zeroLengthPingIsDeliveredImmediately() throws IOException {
    byte[] ping = maskedFrame(WebSocketFrameDecoder.PING, true, new byte[0], MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    RecordingListener listener = new RecordingListener();

    decoder.decode(packet(ping), new Packet(1, false), listener);

    assertEquals(1, listener.pings.size());
    assertEquals(0, listener.pings.get(0).length);
  }

  @Test
  void pongDoesNotGenerateAnotherControlResponse() throws IOException {
    byte[] pong = maskedFrame(WebSocketFrameDecoder.PONG, true, "ok".getBytes(StandardCharsets.UTF_8), MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    RecordingListener listener = new RecordingListener();

    decoder.decode(packet(pong), new Packet(1, false), listener);

    assertEquals(1, listener.pongs.size());
    assertTrue(listener.pings.isEmpty());
  }

  @Test
  void drainsValidatedMessageAcrossApplicationBuffers() throws IOException {
    byte[] payload = new byte[100];
    Arrays.fill(payload, (byte) 'z');
    byte[] frame = maskedFrame(WebSocketFrameDecoder.BINARY, true, payload, MASK);
    Packet source = packet(frame);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    RecordingListener listener = new RecordingListener();
    ByteArrayOutputStream decoded = new ByteArrayOutputStream();

    do {
      Packet chunk = new Packet(17, false);
      decoder.decode(source, chunk, listener);
      decoded.writeBytes(bytes(chunk));
    } while (source.hasRemaining() || decoder.hasPendingOutput());

    assertArrayEquals(payload, decoded.toByteArray());
  }

  @Test
  void reportsProtocolCloseCodeForUnmaskedFrame() {
    byte[] frame = {(byte) 0x81, 0x01, 'x'};
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();

    WebSocketProtocolException error = assertThrows(WebSocketProtocolException.class,
        () -> decoder.decode(packet(frame), new Packet(8, false), new RecordingListener()));

    assertEquals(1002, error.getCloseCode());
  }

  @Test
  void rejectsUnmaskedClientFrame() {
    byte[] frame = {(byte) 0x81, 0x01, 'x'};
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();

    IOException error = assertThrows(IOException.class,
        () -> decoder.decode(packet(frame), new Packet(8, false), new RecordingListener()));

    assertTrue(error.getMessage().contains("must be masked"));
  }

  @Test
  void rejectsFragmentedControlFrame() {
    byte[] frame = maskedFrame(WebSocketFrameDecoder.PING, false, new byte[]{1}, MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();

    IOException error = assertThrows(IOException.class,
        () -> decoder.decode(packet(frame), new Packet(8, false), new RecordingListener()));

    assertTrue(error.getMessage().contains("Control frames must not be fragmented"));
  }

  @Test
  void rejectsContinuationWithoutInitialDataFrame() {
    byte[] frame = maskedFrame(WebSocketFrameDecoder.CONTINUATION, true, new byte[]{1}, MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();

    IOException error = assertThrows(IOException.class,
        () -> decoder.decode(packet(frame), new Packet(8, false), new RecordingListener()));

    assertTrue(error.getMessage().contains("without an open fragmented message"));
  }

  @Test
  void rejectsInvalidUtf8TextPayloadWithoutExposingData() {
    byte[] frame = maskedFrame(
        WebSocketFrameDecoder.TEXT,
        true,
        new byte[]{(byte) 0xC3, 0x28},
        MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    Packet decoded = new Packet(8, false);

    WebSocketProtocolException error = assertThrows(
        WebSocketProtocolException.class,
        () -> decoder.decode(packet(frame), decoded, new RecordingListener()));

    assertEquals(1007, error.getCloseCode());
    assertEquals(0, decoded.position());
  }

  @Test
  void rejectsNonMinimalExtendedPayloadLength() {
    byte[] frame = {(byte) 0x81, (byte) 0xFE, 0x00, 0x7D};
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();

    WebSocketProtocolException error = assertThrows(
        WebSocketProtocolException.class,
        () -> decoder.decode(packet(frame), new Packet(128, false), new RecordingListener()));

    assertEquals(1002, error.getCloseCode());
    assertTrue(error.getMessage().contains("Non-minimal"));
  }

  @Test
  void validatesUtf8AcrossFragmentBoundaries() throws IOException {
    byte[] euro = "€".getBytes(StandardCharsets.UTF_8);
    byte[] first = maskedFrame(WebSocketFrameDecoder.TEXT, false, Arrays.copyOfRange(euro, 0, 1), MASK);
    byte[] last = maskedFrame(WebSocketFrameDecoder.CONTINUATION, true, Arrays.copyOfRange(euro, 1, euro.length), MASK);
    WebSocketFrameDecoder decoder = new WebSocketFrameDecoder();
    Packet decoded = new Packet(euro.length, false);

    assertEquals(0, decoder.decode(packet(first), decoded, new RecordingListener()));
    decoder.decode(packet(last), decoded, new RecordingListener());

    assertArrayEquals(euro, bytes(decoded));
  }

  private static byte[] stompPayload(int length) {
    byte[] prefix = "SEND\ndestination:4817/catl/maps/json/USV-003/MessageTypeEnum_TASK_ADMIN\ncontent-type:application/json\n\n".getBytes(StandardCharsets.UTF_8);
    byte[] payload = new byte[length];
    System.arraycopy(prefix, 0, payload, 0, prefix.length);
    Arrays.fill(payload, prefix.length, payload.length - 1, (byte) 'x');
    payload[payload.length - 1] = 0;
    return payload;
  }

  private static byte[] maskedFrame(int opcode, boolean finish, byte[] payload, byte[] mask) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.write((finish ? 0x80 : 0) | opcode);
    if (payload.length < 126) {
      output.write(0x80 | payload.length);
    } else if (payload.length <= 0xFFFF) {
      output.write(0x80 | 126);
      output.write((payload.length >>> 8) & 0xFF);
      output.write(payload.length & 0xFF);
    } else {
      output.write(0x80 | 127);
      long length = payload.length;
      for (int shift = 56; shift >= 0; shift -= 8) {
        output.write((int) ((length >>> shift) & 0xFF));
      }
    }
    output.writeBytes(mask);
    for (int index = 0; index < payload.length; index++) {
      output.write(payload[index] ^ mask[index & 3]);
    }
    return output.toByteArray();
  }

  private static Packet packet(byte[] value) {
    return new Packet(ByteBuffer.wrap(value));
  }

  private static byte[] bytes(Packet packet) {
    ByteBuffer duplicate = packet.getRawBuffer().duplicate();
    duplicate.flip();
    byte[] value = new byte[duplicate.remaining()];
    duplicate.get(value);
    return value;
  }

  private static final class RecordingListener implements WebSocketFrameDecoder.Listener {
    private final List<byte[]> pings = new ArrayList<>();
    private final List<byte[]> pongs = new ArrayList<>();

    @Override
    public void onPing(byte[] payload) {
      pings.add(payload);
    }

    @Override
    public void onPong(byte[] payload) {
      pongs.add(payload);
    }

    @Override
    public void onClose(byte[] payload) {
    }
  }
}
