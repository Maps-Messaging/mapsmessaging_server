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
import io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PingFrame;
import io.mapsmessaging.test.ProtocolFragmentationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NatsFragmentationTest {

  private static final byte[] PING = "PING\r\n".getBytes(StandardCharsets.US_ASCII);

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 10, 100, 1024})
  void parses_ping_across_chunk_sizes(int chunkSize) throws Exception {
    NatsFrameProcessor processor = new NatsFrameProcessor();

    ProtocolFragmentationTestSupport.feed(PING, chunkSize, processor::process);

    assertPing(processor.getCompletedFrame());
  }

  @Test
  void parses_ping_at_every_split_point() throws Exception {
    for (int split = 1; split < PING.length; split++) {
      NatsFrameProcessor processor = new NatsFrameProcessor();
      ProtocolFragmentationTestSupport.feed(PING, split, PING.length, processor::process);
      assertPing(processor.getCompletedFrame());
    }
  }

  private void assertPing(NatsFrame frame) {
    assertNotNull(frame);
    assertInstanceOf(PingFrame.class, frame);
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
