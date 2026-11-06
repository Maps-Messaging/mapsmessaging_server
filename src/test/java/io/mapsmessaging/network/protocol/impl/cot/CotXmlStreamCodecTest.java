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

package io.mapsmessaging.network.protocol.impl.cot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CotXmlStreamCodecTest {

  private static final int MAXIMUM_EVENT_SIZE = 16_384;
  private static final String HEADER = "<?xml version='1.0' standalone='yes'?>\n";

  @Test
  void decode_preserves_events_across_every_fragmentation_boundary() throws Exception {
    byte[] xml = event("fragmented");
    for (int split = 0; split <= xml.length; split++) {
      CotStreamCodec codec = new CotXmlStreamCodec(MAXIMUM_EVENT_SIZE);
      List<byte[]> events = new ArrayList<>();
      events.addAll(codec.decode(Arrays.copyOfRange(xml, 0, split)));
      events.addAll(codec.decode(Arrays.copyOfRange(xml, split, xml.length)));
      assertEquals(1, events.size(), "split=" + split);
      assertArrayEquals(xml, events.getFirst(), "split=" + split);
    }
  }

  @Test
  void decode_separates_adjacent_events_without_splitting_comments_or_cdata() throws Exception {
    byte[] first = event("first");
    byte[] second = bytes(new String(event("second"), StandardCharsets.UTF_8).replace(
        "</event>", "<detail><!-- </event> --><remarks><![CDATA[</event>]]></remarks></detail></event>"));
    CotStreamCodec codec = new CotXmlStreamCodec(MAXIMUM_EVENT_SIZE);
    byte[] wire = bytes(HEADER + new String(first, StandardCharsets.UTF_8)
        + HEADER + new String(second, StandardCharsets.UTF_8));

    List<byte[]> events = codec.decode(wire);

    assertEquals(2, events.size());
    assertArrayEquals(first, events.get(0));
    assertArrayEquals(second, events.get(1));
  }

  @Test
  void encode_adds_the_existing_xml_stream_header() throws Exception {
    byte[] xml = event("outbound");
    CotStreamCodec codec = new CotXmlStreamCodec(MAXIMUM_EVENT_SIZE);

    assertArrayEquals(bytes(HEADER + new String(xml, StandardCharsets.UTF_8)), codec.encode(xml));
  }

  @Test
  void encode_preserves_an_existing_declaration() throws Exception {
    byte[] xml = bytes("<?xml version='1.0' encoding='UTF-8'?>"
        + new String(event("declared"), StandardCharsets.UTF_8));
    CotStreamCodec codec = new CotXmlStreamCodec(MAXIMUM_EVENT_SIZE);

    assertArrayEquals(xml, codec.encode(xml));
  }

  @Test
  void encode_rejects_invalid_cot_events() {
    CotStreamCodec codec = new CotXmlStreamCodec(MAXIMUM_EVENT_SIZE);

    assertThrows(IOException.class, () -> codec.encode(bytes("<event/>")));
    assertThrows(IOException.class, () -> codec.encode(bytes("<event>")));
  }

  @Test
  void decode_enforces_the_event_size_limit() {
    CotStreamCodec codec = new CotXmlStreamCodec(256);

    assertThrows(IOException.class, () -> codec.decode(bytes("<event>" + "x".repeat(256))));
  }

  @Test
  void decoder_state_is_isolated_between_connections() throws Exception {
    CotStreamCodec first = new CotXmlStreamCodec(MAXIMUM_EVENT_SIZE);
    CotStreamCodec second = new CotXmlStreamCodec(MAXIMUM_EVENT_SIZE);
    byte[] xml = event("separate");

    assertTrue(first.decode(Arrays.copyOfRange(xml, 0, 10)).isEmpty());
    assertArrayEquals(xml, second.decode(xml).getFirst());
    assertArrayEquals(xml, first.decode(Arrays.copyOfRange(xml, 10, xml.length)).getFirst());
  }

  @Test
  void version_preserves_the_existing_protocol_identity() {
    CotStreamCodec codec = new CotXmlStreamCodec(MAXIMUM_EVENT_SIZE);

    assertEquals("CoT 2.0 XML Streaming", codec.getVersion());
  }

  private static byte[] event(String uid) {
    return bytes("<event version='2.0' uid='" + uid + "' type='a-f-G-U-C' "
        + "time='2026-10-07T07:00:00Z' start='2026-10-07T07:00:00Z' "
        + "stale='2026-10-07T07:01:00Z' how='m-g'>"
        + "<point lat='1' lon='2' hae='3' ce='4' le='5'/></event>");
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
