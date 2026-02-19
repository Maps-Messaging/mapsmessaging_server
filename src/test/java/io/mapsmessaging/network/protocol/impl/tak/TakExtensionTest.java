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

package io.mapsmessaging.network.protocol.impl.tak;

import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.protocol.impl.tak.framing.TakStreamFramer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TakExtensionTest {

  @Test
  void parsesExtensionConfigWithDefaultsAndOverrides() throws Exception {
    ExtensionConfigDTO dto = new ExtensionConfigDTO();
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("payload", "tak_proto_v1");
    config.put("framing", "proto_stream");
    config.put("max_payload_bytes", 2048);
    config.put("reconnect_delay_ms", 1500);
    config.put("read_buffer_bytes", 4096);
    setField(dto, "config", config);

    TakExtensionConfig parsed = TakExtensionConfig.from(dto);

    assertEquals("tak_proto_v1", parsed.getPayload());
    assertEquals(TakStreamFramer.Mode.PROTO_STREAM, parsed.getFramingMode());
    assertEquals(2048, parsed.getMaxPayloadBytes());
    assertEquals(1500, parsed.getReconnectDelayMs());
    assertEquals(4096, parsed.getReadBufferBytes());
  }

  @Test
  void appliesSafeDefaultsWhenMissing() {
    TakExtensionConfig parsed = TakExtensionConfig.from(null);
    assertEquals("cot_xml", parsed.getPayload());
    assertEquals(TakStreamFramer.Mode.XML_STREAM, parsed.getFramingMode());
    assertEquals(1024 * 1024, parsed.getMaxPayloadBytes());
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
