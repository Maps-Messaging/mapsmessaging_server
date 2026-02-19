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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakProtobufCodecTest {

  @Test
  void encodesCotXmlIntoProtobufPayload() throws IOException {
    TakProtobufCodec codec = new TakProtobufCodec();
    String cot = """
        <event uid="u-100" type="a-f-G-U-C" time="2026-02-19T09:10:00Z" start="2026-02-19T09:10:00Z" stale="2026-02-19T09:15:00Z" how="m-g">
          <point lat="1" lon="2"/>
        </event>
        """;
    Message message = new MessageBuilder().setOpaqueData(cot.getBytes(StandardCharsets.UTF_8)).build();

    byte[] encoded = codec.encode(message);
    Message decoded = codec.decode(encoded);

    assertEquals("application/x-protobuf", decoded.getContentType());
    assertEquals("protobuf", decoded.getMeta().get("tak.format"));
    assertEquals("u-100", decoded.getMeta().get("tak.uid"));
    assertEquals("a-f-G-U-C", decoded.getMeta().get("tak.type"));
    assertNotNull(decoded.getOpaqueData());
    assertTrue(decoded.getOpaqueData().length > 0);
  }

  @Test
  void passesThroughBinaryProtobufPayload() throws IOException {
    TakProtobufCodec codec = new TakProtobufCodec();
    byte[] raw = new byte[]{0x08, 0x7F, 0x10, 0x01};
    Message message = new MessageBuilder().setOpaqueData(raw).build();

    byte[] encoded = codec.encode(message);
    Message decoded = codec.decode(encoded);

    assertArrayEquals(raw, encoded);
    assertArrayEquals(raw, decoded.getOpaqueData());
    assertEquals("protobuf", decoded.getMeta().get("tak.format"));
  }

  @Test
  void buildsCotFromMetaWhenPayloadMissing() throws IOException {
    TakProtobufCodec codec = new TakProtobufCodec();
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("tak.uid", "u-200");
    meta.put("tak.type", "a-f-G-U-C");
    meta.put("tak.time", "2026-02-19T09:10:00Z");
    meta.put("tak.start", "2026-02-19T09:10:00Z");
    meta.put("tak.stale", "2026-02-19T09:15:00Z");
    meta.put("tak.how", "m-g");
    meta.put("tak.lat", "1");
    meta.put("tak.lon", "2");
    Message message = new MessageBuilder().setMeta(meta).build();

    byte[] encoded = codec.encode(message);
    Message decoded = codec.decode(encoded);

    assertEquals("u-200", decoded.getMeta().get("tak.uid"));
    assertEquals("a-f-G-U-C", decoded.getMeta().get("tak.type"));
  }
}
