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

package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import io.mapsmessaging.test.ProtocolFragmentationTestSupport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NatsPayloadFragmentationTest {

  @ParameterizedTest
  @ValueSource(strings = {"PUB", "MSG", "HPUB", "HMSG"})
  void payload_and_subscription_survive_every_split_without_reply_to(String command) throws Exception {
    String input = switch (command) {
      case "PUB" -> "topic 3\r\nabc\r\n";
      case "MSG" -> "topic 42 3\r\nabc\r\n";
      case "HPUB" -> "topic 27 30\r\nNATS/1.0\r\nX-Test: value\r\n\r\nabc\r\n";
      case "HMSG" -> "topic 42 27 30\r\nNATS/1.0\r\nX-Test: value\r\n\r\nabc\r\n";
      default -> throw new IllegalArgumentException(command);
    };
    byte[] bytes = input.getBytes(StandardCharsets.US_ASCII);
    for (int split = 1; split < bytes.length; split++) {
      PayloadFrame frame = switch (command) {
        case "PUB" -> new PubFrame(1024);
        case "MSG" -> new MsgFrame(1024);
        case "HPUB" -> new HPubFrame(1024);
        case "HMSG" -> new HMsgFrame(1024);
        default -> throw new IllegalArgumentException(command);
      };
      boolean[] completed = {false};
      ProtocolFragmentationTestSupport.feed(bytes, split, bytes.length, packet -> {
        try {
          frame.parseFrame(packet);
          completed[0] = true;
        } catch (EndOfBufferException incomplete) {
          assertEquals(0, packet.position(), "Incomplete frame must retain its complete input");
        }
      });
      assertTrue(completed[0], "Frame must finish after receiving the remaining bytes");
      assertEquals("topic", frame.getSubject());
      assertEquals(command.endsWith("MSG") ? "42" : null, frame.getSubscriptionId());
      assertNull(frame.getReplyTo());
      assertArrayEquals("abc".getBytes(StandardCharsets.US_ASCII), frame.getPayload());
      if (frame instanceof HPayloadFrame headerFrame) {
        assertEquals("value", headerFrame.getHeader().get("X-Test"));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"bad.*", "bad.>", ".bad", "bad.", "bad..topic"})
  void publish_subject_validation_survives_fragmentation_merge(String subject) {
    Packet packet = new Packet(1024, false);
    packet.put((subject + " 0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
    packet.flip();
    PubFrame frame = new PubFrame(1024);

    assertThrows(NatsProtocolException.class, () -> frame.parseFrame(packet));
  }
}
