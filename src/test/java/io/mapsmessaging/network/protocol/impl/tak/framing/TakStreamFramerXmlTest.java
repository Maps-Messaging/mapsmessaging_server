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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakStreamFramerXmlTest {

  @Test
  void xmlDelimitsMultipleEvents() throws IOException {
    TakStreamFramer framer = new TakStreamFramer(TakStreamFramer.Mode.XML_STREAM, 1024 * 1024);
    String payload = "<event uid=\"1\"><point lat=\"1\" lon=\"2\"/></event>\n<event uid=\"2\"><point lat=\"3\" lon=\"4\"/></event>\n";

    List<byte[]> frames = framer.onBytes(payload.getBytes(StandardCharsets.UTF_8), payload.length());

    assertEquals(2, frames.size());
    assertTrue(new String(frames.get(0), StandardCharsets.UTF_8).contains("uid=\"1\""));
    assertTrue(new String(frames.get(1), StandardCharsets.UTF_8).contains("uid=\"2\""));
  }

  @Test
  void xmlPartialEventBuffersUntilComplete() throws IOException {
    TakStreamFramer framer = new TakStreamFramer(TakStreamFramer.Mode.XML_STREAM, 1024 * 1024);
    byte[] bytes = "<event uid=\"1\"><point lat=\"1\" lon=\"2\"/></event>".getBytes(StandardCharsets.UTF_8);

    List<byte[]> first = framer.onBytes(bytes, 10);
    assertTrue(first.isEmpty());

    byte[] rest = new byte[bytes.length - 10];
    System.arraycopy(bytes, 10, rest, 0, rest.length);
    List<byte[]> second = framer.onBytes(rest, rest.length);
    assertEquals(1, second.size());
  }
}
