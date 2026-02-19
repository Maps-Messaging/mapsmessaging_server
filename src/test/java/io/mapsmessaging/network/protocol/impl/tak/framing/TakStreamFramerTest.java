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

package io.mapsmessaging.network.protocol.impl.tak.framing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakStreamFramerTest {

  @Test
  void protoFrameRoundTrip() throws IOException {
    TakStreamFramer framer = new TakStreamFramer(TakStreamFramer.Mode.PROTO_STREAM, 1024 * 1024);
    byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);

    byte[] framed = framer.frame(payload);
    List<byte[]> decoded = framer.onBytes(framed, framed.length);

    assertEquals(1, decoded.size());
    assertArrayEquals(payload, decoded.getFirst());
  }

  @Test
  void protoPartialReadReassemblesFrames() throws IOException {
    TakStreamFramer framer = new TakStreamFramer(TakStreamFramer.Mode.PROTO_STREAM, 1024 * 1024);
    byte[] payload = "abc123".getBytes(StandardCharsets.UTF_8);
    byte[] framed = framer.frame(payload);

    List<byte[]> first = framer.onBytes(framed, 2);
    assertTrue(first.isEmpty());
    List<byte[]> second = framer.onBytes(slice(framed, 2), framed.length - 2);

    assertEquals(1, second.size());
    assertArrayEquals(payload, second.getFirst());
  }

  @Test
  void protoRejectsOversizedFrame() {
    TakStreamFramer framer = new TakStreamFramer(TakStreamFramer.Mode.PROTO_STREAM, 8);
    byte[] fake = new byte[]{(byte) 0xBF, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    assertThrows(IOException.class, () -> framer.onBytes(fake, fake.length));
  }

  @Test
  void protoRejectsInvalidMagic() {
    TakStreamFramer framer = new TakStreamFramer(TakStreamFramer.Mode.PROTO_STREAM, 1024);
    byte[] invalid = new byte[]{0x01, 0x01, 0x7F};
    assertThrows(IOException.class, () -> framer.onBytes(invalid, invalid.length));
  }

  private static byte[] slice(byte[] source, int from) {
    byte[] out = new byte[source.length - from];
    System.arraycopy(source, from, out, 0, out.length);
    return out;
  }
}
