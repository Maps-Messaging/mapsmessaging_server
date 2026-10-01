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

package io.mapsmessaging.network.io.impl.lora.serial;

import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.Packet;
import static io.mapsmessaging.network.protocol.impl.loragateway.Constants.DATA;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LoRaStreamHandlerTest {
  private final LoRaStreamHandler handler = new LoRaStreamHandler(LoggerFactory.getLogger(getClass()));

  @Test
  void truncated_frame_is_rejected_at_each_boundary() {
    byte[] frame = {Constants.START_FRAME, DATA, 2, 42, 43, Constants.END_FRAME};
    for (int length = 0; length < frame.length; length++) {
      byte[] truncated = Arrays.copyOf(frame, length);
      assertThrows(IOException.class,
          () -> handler.parseInput(new ByteArrayInputStream(truncated), new Packet(300, false)),
          "Truncated frame length " + length);
    }
  }

  @Test
  void unsigned_payload_length_and_ff_data_are_preserved() throws IOException {
    byte[] frame = new byte[132];
    frame[0] = Constants.START_FRAME;
    frame[1] = DATA;
    frame[2] = (byte) 128;
    Arrays.fill(frame, 3, 131, (byte) 0xff);
    frame[131] = Constants.END_FRAME;
    Packet packet = new Packet(300, false);
    assertEquals(130, handler.parseInput(new ByteArrayInputStream(frame), packet));
    packet.flip();
    assertEquals(DATA & 0xff, packet.getByte());
    assertEquals(128, packet.getByte());
    for (int index = 0; index < 128; index++) {
      assertEquals(255, packet.getByte());
    }
    assertFalse(packet.hasRemaining());
  }
}
