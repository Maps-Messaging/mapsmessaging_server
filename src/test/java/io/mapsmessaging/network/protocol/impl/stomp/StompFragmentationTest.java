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

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Event;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.FrameFactory;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Send;
import io.mapsmessaging.test.ProtocolFragmentationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StompFragmentationTest {

  private static final byte[] BODY = "hello fragmented".getBytes(StandardCharsets.UTF_8);
  private static final byte[] FRAME = createFrame();

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 10, 100, 1024, 10240})
  void parses_send_across_chunk_sizes(int chunkSize) throws Exception {
    StompFrameProcessor processor = new StompFrameProcessor();

    ProtocolFragmentationTestSupport.feed(FRAME, chunkSize, processor::process);

    assertSend(processor.getCompletedFrame());
  }

  @Test
  void parses_send_at_every_split_point() throws Exception {
    for (int split = 1; split < FRAME.length; split++) {
      StompFrameProcessor processor = new StompFrameProcessor();
      ProtocolFragmentationTestSupport.feed(FRAME, split, FRAME.length, processor::process);
      assertSend(processor.getCompletedFrame());
    }
  }

  private void assertSend(Frame frame) {
    assertNotNull(frame);
    Send send = assertInstanceOf(Send.class, frame);
    Event event = send;
    assertEquals("/topic/fragmentation", event.getDestination());
    assertArrayEquals(BODY, event.getData());
  }

  private static byte[] createFrame() {
    byte[] header = ("SEND\n"
        + "destination:/topic/fragmentation\n"
        + "content-length:" + BODY.length + "\n"
        + "test-header:test-value\n"
        + "\n").getBytes(StandardCharsets.UTF_8);
    byte[] frame = new byte[header.length + BODY.length + 1];
    System.arraycopy(header, 0, frame, 0, header.length);
    System.arraycopy(BODY, 0, frame, header.length, BODY.length);
    frame[frame.length - 1] = 0;
    return frame;
  }

  private static final class StompFrameProcessor {

    private final FrameFactory frameFactory = new FrameFactory(1024 * 1024, false, false);
    private Frame activeFrame;
    private Frame completedFrame;

    private void process(Packet packet) throws Exception {
      while (packet.hasRemaining()) {
        Frame frame = activeFrame;
        activeFrame = null;
        try {
          if (frame == null) {
            frame = frameFactory.parseFrame(packet);
          }
          activeFrame = frame;
          activeFrame.scanFrame(packet);
          if (!activeFrame.isValid()) {
            throw new StompProtocolException("Fragmented frame was not valid");
          }
          completedFrame = activeFrame;
          activeFrame = null;
        } catch (EndOfBufferException expected) {
          activeFrame = frame;
          return;
        }
      }
    }

    private Frame getCompletedFrame() {
      return completedFrame;
    }
  }
}
