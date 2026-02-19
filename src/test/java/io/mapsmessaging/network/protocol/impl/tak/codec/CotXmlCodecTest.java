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

package io.mapsmessaging.network.protocol.impl.tak.codec;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CotXmlCodecTest {

  private static final String VALID_COT = """
      <event uid="u-1" type="a-f-G-U-C" time="2026-02-19T09:10:00Z" start="2026-02-19T09:10:00Z" stale="2026-02-19T09:15:00Z" how="m-g">
        <point lat="51.5007" lon="-0.1246" hae="10" ce="5" le="5"/>
      </event>
      """;

  @Test
  void decodeValidCotExtractsCoreFields() throws IOException {
    CotXmlCodec codec = new CotXmlCodec();

    Message message = codec.decode(VALID_COT.getBytes(StandardCharsets.UTF_8));

    assertNotNull(message.getMeta());
    assertEquals("u-1", message.getMeta().get("tak.uid"));
    assertEquals("a-f-G-U-C", message.getMeta().get("tak.type"));
    assertEquals("51.5007", message.getMeta().get("tak.lat"));
    assertEquals("-0.1246", message.getMeta().get("tak.lon"));
    assertEquals("application/cot+xml", message.getContentType());
    assertNotNull(message.getOpaqueData());
  }

  @Test
  void decodeMissingRequiredFieldFails() {
    CotXmlCodec codec = new CotXmlCodec();
    String invalid = """
        <event uid="u-1" time="2026-02-19T09:10:00Z" start="2026-02-19T09:10:00Z" stale="2026-02-19T09:15:00Z" how="m-g">
          <point lat="1" lon="2"/>
        </event>
        """;

    assertThrows(IOException.class, () -> codec.decode(invalid.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void decodeMalformedXmlFails() {
    CotXmlCodec codec = new CotXmlCodec();
    String invalid = "<event><point lat=\"1\" lon=\"2\"></event";
    assertThrows(IOException.class, () -> codec.decode(invalid.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void decodeRejectsXxe() {
    CotXmlCodec codec = new CotXmlCodec();
    String xxe = """
        <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
        <event uid="u-1" type="a-f-G-U-C" time="2026-02-19T09:10:00Z" start="2026-02-19T09:10:00Z" stale="2026-02-19T09:15:00Z" how="m-g">
          <point lat="1" lon="2"/>
          <detail>&xxe;</detail>
        </event>
        """;
    assertThrows(IOException.class, () -> codec.decode(xxe.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void encodeFromMetaBuildsCotXml() throws IOException {
    CotXmlCodec codec = new CotXmlCodec();
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("tak.uid", "u-2");
    meta.put("tak.type", "a-f-G-U-C");
    meta.put("tak.time", "2026-02-19T09:10:00Z");
    meta.put("tak.start", "2026-02-19T09:10:00Z");
    meta.put("tak.stale", "2026-02-19T09:15:00Z");
    meta.put("tak.how", "m-g");
    meta.put("tak.lat", "1");
    meta.put("tak.lon", "2");
    meta.put("tak.hae", "3");
    meta.put("tak.ce", "4");
    meta.put("tak.le", "5");

    Message message = new MessageBuilder().setMeta(meta).build();
    byte[] xml = codec.encode(message);
    String text = new String(xml, StandardCharsets.UTF_8);

    assertTrue(text.startsWith("<event"));
    assertTrue(text.contains("uid=\"u-2\""));
    assertTrue(text.contains("<point"));
  }
}
