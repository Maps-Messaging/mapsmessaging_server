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

package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.nats.frames.ConnectFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory;
import io.mapsmessaging.network.protocol.impl.nats.frames.HMsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.HPubFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PingFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PubFrame;
import io.mapsmessaging.test.ProtocolFragmentationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NatsFragmentationTest {

  private static final byte[] PING = "PING\r\n".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] CONNECT = ("CONNECT {\"echo\":true,\"headers\":true,\"user\":\"test-user\",\"pass\":\"test-pass\"}\r\n")
      .getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PAYLOAD = "hello world".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] HEADERS = "NATS/1.0\r\nX-Test: value\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PUB = payloadFrame("PUB topic reply.inbox " + PAYLOAD.length + "\r\n", PAYLOAD);
  private static final byte[] MSG = payloadFrame("MSG topic 42 reply.inbox " + PAYLOAD.length + "\r\n", PAYLOAD);
  private static final byte[] HPUB = headerPayloadFrame("HPUB topic reply.inbox ");
  private static final byte[] HMSG = headerPayloadFrame("HMSG topic 42 reply.inbox ");

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 10, 100, 1024})
  void parses_all_frame_types_across_chunk_sizes(int chunkSize) throws Exception {
    assertFragmented(PING, chunkSize, this::assertPing);
    assertFragmented(CONNECT, chunkSize, this::assertConnect);
    assertFragmented(PUB, chunkSize, frame -> assertPayload(frame, PubFrame.class, null));
    assertFragmented(MSG, chunkSize, frame -> assertPayload(frame, MsgFrame.class, "42"));
    assertFragmented(HPUB, chunkSize, frame -> assertHeaderPayload(frame, HPubFrame.class, null));
    assertFragmented(HMSG, chunkSize, frame -> assertHeaderPayload(frame, HMsgFrame.class, "42"));
  }

  @Test
  void parses_all_frame_types_at_every_split_point() throws Exception {
    assertEverySplit(PING, this::assertPing);
    assertEverySplit(CONNECT, this::assertConnect);
    assertEverySplit(PUB, frame -> assertPayload(frame, PubFrame.class, null));
    assertEverySplit(MSG, frame -> assertPayload(frame, MsgFrame.class, "42"));
    assertEverySplit(HPUB, frame -> assertHeaderPayload(frame, HPubFrame.class, null));
    assertEverySplit(HMSG, frame -> assertHeaderPayload(frame, HMsgFrame.class, "42"));
  }

  private void assertFragmented(byte[] source, int chunkSize, Consumer<NatsFrame> assertion) throws Exception {
    NatsFrameProcessor processor = new NatsFrameProcessor();
    ProtocolFragmentationTestSupport.feed(source, chunkSize, processor::process);
    assertion.accept(processor.getCompletedFrame());
  }

  private void assertEverySplit(byte[] source, Consumer<NatsFrame> assertion) throws Exception {
    for (int split = 1; split < source.length; split++) {
      NatsFrameProcessor processor = new NatsFrameProcessor();
      ProtocolFragmentationTestSupport.feed(source, split, source.length, processor::process);
      assertion.accept(processor.getCompletedFrame());
    }
  }

  private void assertPing(NatsFrame frame) {
    assertNotNull(frame);
    assertInstanceOf(PingFrame.class, frame);
  }

  private void assertConnect(NatsFrame frame) {
    ConnectFrame connect = assertInstanceOf(ConnectFrame.class, frame);
    assertTrue(connect.isEcho());
    assertTrue(connect.isHeaders());
    assertEquals("test-user", connect.getUser());
    assertEquals("test-pass", connect.getPass());
  }

  private <T extends PayloadFrame> void assertPayload(NatsFrame frame, Class<T> frameType, String subscriptionId) {
    T payloadFrame = assertInstanceOf(frameType, frame);
    assertEquals("topic", payloadFrame.getSubject());
    assertEquals("reply.inbox", payloadFrame.getReplyTo());
    assertEquals(subscriptionId, payloadFrame.getSubscriptionId());
    assertArrayEquals(PAYLOAD, payloadFrame.getPayload());
  }

  private void assertHeaderPayload(NatsFrame frame, Class<? extends PayloadFrame> frameType, String subscriptionId) {
    assertPayload(frame, frameType, subscriptionId);
    if (frame instanceof HPubFrame hPubFrame) {
      assertEquals("value", hPubFrame.getHeader().get("X-Test"));
    } else if (frame instanceof HMsgFrame hMsgFrame) {
      assertEquals("value", hMsgFrame.getHeader().get("X-Test"));
    }
  }

  private static byte[] payloadFrame(String commandLine, byte[] payload) {
    byte[] prefix = commandLine.getBytes(StandardCharsets.US_ASCII);
    byte[] frame = new byte[prefix.length + payload.length + 2];
    System.arraycopy(prefix, 0, frame, 0, prefix.length);
    System.arraycopy(payload, 0, frame, prefix.length, payload.length);
    frame[frame.length - 2] = '\r';
    frame[frame.length - 1] = '\n';
    return frame;
  }

  private static byte[] headerPayloadFrame(String commandPrefix) {
    int totalSize = HEADERS.length + PAYLOAD.length;
    String commandLine = commandPrefix + HEADERS.length + " " + totalSize + "\r\n";
    byte[] prefix = commandLine.getBytes(StandardCharsets.US_ASCII);
    byte[] frame = new byte[prefix.length + totalSize + 2];
    int offset = 0;
    System.arraycopy(prefix, 0, frame, offset, prefix.length);
    offset += prefix.length;
    System.arraycopy(HEADERS, 0, frame, offset, HEADERS.length);
    offset += HEADERS.length;
    System.arraycopy(PAYLOAD, 0, frame, offset, PAYLOAD.length);
    frame[frame.length - 2] = '\r';
    frame[frame.length - 1] = '\n';
    return frame;
  }

  private static final class NatsFrameProcessor {

    private final FrameFactory frameFactory = new FrameFactory(1024 * 1024, false);
    private NatsFrame activeFrame;
    private NatsFrame completedFrame;

    private void process(Packet packet) throws Exception {
      while (packet.hasRemaining()) {
        NatsFrame frame = activeFrame;
        activeFrame = null;
        try {
          if (frame == null) {
            frame = frameFactory.parseFrame(packet);
          }
          activeFrame = frame;
          activeFrame.parseFrame(packet);
          if (!activeFrame.isValid()) {
            throw new NatsProtocolException("Fragmented frame was not valid");
          }
          completedFrame = activeFrame;
          activeFrame = null;
        } catch (EndOfBufferException expected) {
          activeFrame = frame;
          return;
        }
      }
    }

    private NatsFrame getCompletedFrame() {
      return completedFrame;
    }
  }
}
